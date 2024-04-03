
package models.platform_model.components;

import java.util.*;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedProcessingModuleViewer;
// import forsyde.io.lib.hierarchy.platform.hardware.LogicProgrammableModuleViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;
import models.utils.Units;
import models.utils.Ports;


public class Platform {
    private SystemGraph sGraph;
    private GreyBoxViewer platformGreyBox;
    private String name;
    private Map<String, VertexViewer> viewers = new HashMap<>();

    public Platform(String name) {
        SystemGraph sGraph = new SystemGraph();

        var platform = Structure.enforce(
            sGraph, sGraph.newVertex(name)
        );
        viewers.put(name, platform);
        this.platformGreyBox = GreyBox.enforce(Visualizable.enforce(platform));
        
        this.sGraph = sGraph;
        this.name = name;
    }

    public SystemGraph GetGraph() {
        return this.sGraph;
    }

    /**
     * Add a structure as child to the parent structure.
     * @param parent Parent structure name.
     * @param child Child structure name.
     */
    // public void AddChildStructure(String parent, String child) {
    //     StructureViewer structure = Structure.enforce(
    //         sGraph, sGraph.newVertex(child)
    //     );
    //     viewers.put(parent + "_" + child, structure);
    //     if (!viewers.containsKey(parent)) {
    //         throw new IllegalArgumentException("Parent structure not found.");
    //     } else {
    //         this.platformGreyBox.addContained(
    //             Visualizable.enforce(structure)
    //         );
    //     }
    // }

    private void BiDirectionalConnect(
        VertexViewer a, VertexViewer b, String portA, String portB
    ) {
        sGraph.connect(
            a, b, portA, portB, 
            EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection
        );
        sGraph.connect(
            b, a, portB, portA, 
            EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection
        );
    }

    private void BiDirectionalConnect(VertexViewer a, VertexViewer b) {
        sGraph.connect(
            a, b, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection
        );
        sGraph.connect(
            b, a, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection
        );
    }

    public void AddMemory(String name, long frequency, long spaceInBits) {
        // ACTUAL MEMORY
        String memoryName = name;
        GenericMemoryModuleViewer mem = GenericMemoryModule.enforce(
            sGraph, sGraph.newVertex(memoryName)
        );
        this.platformGreyBox.addContained(Visualizable.enforce(mem));

        mem.operatingFrequencyInHertz(600 * Units.MHz);
        mem.spaceInBits(4 * Units.GB * Units.BYTES_TO_BITS);
        mem.addPorts(Ports.MEM_TO_MEMSWITCH);

        // MEMORY SWITCH
        String switchName = memoryName + "_Switch";
        var sw = InstrumentedCommunicationModule.enforce(
            sGraph, sGraph.newVertex(switchName)
        );
        this.viewers.put(switchName, sw);
        this.platformGreyBox.addContained(Visualizable.enforce(sw));
        sw.operatingFrequencyInHertz(600 * Units.MHz);
        sw.initialLatency(0L);
        sw.flitSizeInBits(8L); // 1 byte
        sw.maxCyclesPerFlit(1); // cycles to send 1 byte
        sw.maxConcurrentFlits(1); // could match ports below with = 4
        sw.addPorts(Ports.MEMSWITCH_TO_MEM);

        // connect switch to memory
        this.BiDirectionalConnect(
            mem, sw, Ports.MEM_TO_MEMSWITCH, Ports.MEMSWITCH_TO_MEM
        );
    }

    /**
     * Connect all processing units from the given namespace to a memory module.
     * In reality the processing units are connected to the memory's switch
     * which is necessary for analysis, 
     * @param puNamespace The namespace of the processing units, e.g. "APU"
     * (must exist).
     * @param memory The memory module name (must exist).
     */
    public void ConnectPUsToMemory(String puNamespace, String memoryName) {
        String switchName = memoryName + "_Switch";
        if (!this.viewers.keySet().contains(switchName)) {
            throw new IllegalArgumentException(
                "No memory switch for memory " + memoryName + " found."
            );
        }
        
        List<String> identified = this.viewers.keySet().stream()
            .filter((key) -> key.contains(puNamespace))
            .filter((key) -> this.viewers.get(key) 
                instanceof InstrumentedProcessingModuleViewer
            ).toList();

        System.out.println(puNamespace + "<->" + memoryName + ":" + identified);
        
        if (identified.isEmpty()) {
            throw new IllegalArgumentException(
                "No processing units found for namespace" + puNamespace + "."
            );
        }
            
        var sw = this.viewers.get(switchName);
        sw.addPorts(Ports.MEMSWITCH_TO_PU);

        identified.stream()
            .map((key) -> this.viewers.get(key))
            .forEach((core) -> {
                core.addPorts(Ports.PU_TO_MEMSWITCH);
                this.BiDirectionalConnect(
                    core, sw, 
                    Ports.PU_TO_MEMSWITCH, Ports.MEMSWITCH_TO_PU
                );
            }
        );
    }

    /**
     * Add a CPU to the MPSoC as <cores> processing modules.
     * @param name The identifier for the CPU.
     * @param cores The number of CPU cores.
     * @param frequency The operating frequency of the CPU.
     * @param modalInstructions Available CPU instructions and their costs.
     */
    public void AddCPU(String cpuName, int cores, long frequency,
                        Map<String, Map<String, Double>> modalInstructions) {
        for (int i = 0; i < cores; i++) {
            String coreName = cpuName + "_C" + i;
            var core = InstrumentedProcessingModule.enforce(
                sGraph, sGraph.newVertex(coreName)
            );
            this.viewers.put(coreName, core);
            this.platformGreyBox.addContained(Visualizable.enforce(core));
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(frequency);
            core.modalInstructionsPerCycle(modalInstructions);

            var tdmApu = TimeDivisionMultiplexingRuntime.enforce(sGraph,
                    sGraph.newVertex(coreName + "_Scheduler"));
            tdmApu.addManaged(core);

            this.BiDirectionalConnect(core, tdmApu);

            this.platformGreyBox.addContained(Visualizable.enforce(tdmApu));
        }
    }

    public void AddFPGA() {
        // LogicProgrammableModuleViewer fpga = LogicProgrammableModule.enforce(
        //         sGraph, sGraph.newVertex("MPSoC_PS_FPGA"));
    }
}
