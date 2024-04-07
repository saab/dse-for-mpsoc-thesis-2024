
package models.platform_model.components;

import java.util.*;
import java.util.stream.Collectors;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
// import forsyde.io.lib.hierarchy.platform.hardware.LogicProgrammableModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedProcessingModuleViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;

import models.utils.Units;


public class Platform {
    private SystemGraph sGraph;
    private GreyBoxViewer platformGreyBox;
    public Map<String, VertexViewer> viewers = new HashMap<>();

    public Platform(String name) {
        SystemGraph sGraph = new SystemGraph();

        var platform = Structure.enforce(
            sGraph, sGraph.newVertex(name)
        );
        viewers.put(name, platform);
        this.platformGreyBox = GreyBox.enforce(Visualizable.enforce(platform));
        
        this.sGraph = sGraph;
    }

    /**
     * Create a platform from an existing graph, by extraction of the viewers.
     * @param g The graph to use as platform.
     */
    public Platform(String name, SystemGraph g) {
        this.sGraph = g;
        this.platformGreyBox = g.vertexSet()
            .stream()
            .flatMap(v -> GreyBoxViewer.tryView(g, v).stream())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No GreyBox found in the given graph."
            ));
            
        for (var v : g.vertexSet()) {
            InstrumentedProcessingModule.tryView(g, v).ifPresent(view -> 
                viewers.put(v.getIdentifier(), view)
            );
            GenericMemoryModule.tryView(g, v).ifPresent(view -> 
                viewers.put(v.getIdentifier(), view)
            );
            InstrumentedCommunicationModule.tryView(g, v).ifPresent(view -> 
                viewers.put(v.getIdentifier(), view)
            );
            Structure.tryView(g, v).ifPresent(view -> 
                viewers.put(v.getIdentifier(), view)
            );
        }

        // System.out.println(viewers.size() + " viewers found.");
    }


    public SystemGraph GetGraph() {
        return this.sGraph;
    }


    /**
     * Connect two components bidirectionally with explicit port names.
     * @param a The first component.
     * @param b The second component.
     * @param portA The port of the first component.
     * @param portB The port of the second component.
     */
    private void CreateEdge(
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

    /**
     * Connect two components bidirectionally.
     * @param a The first component.
     * @param b The second component.
     */
    private void CreateEdge(VertexViewer a, VertexViewer b) {
        sGraph.connect(
            a, b, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection
        );
        sGraph.connect(
            b, a, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection
        );
    }
    
    /**
     * Connect all processing units from the given namespace to a memory module.
     * In reality the processing units are connected to the memory's switch
     * which is necessary for analysis, 
     * @param srcNamespace The namespace or exact name of the source components.
     * (must exist).
     * @param memory The exact name of the destination component (must exist).
     */
    public void Connect(String srcNamespace, String dstName) {
        if (!this.viewers.keySet().contains(dstName)) {
            throw new IllegalArgumentException(
                "No component named " + dstName + " found."
            );
        }
        
        List<String> srcNames = this.viewers.keySet().stream()
            .filter((key) -> key.contains(srcNamespace))
            .toList();

        if (srcNames.isEmpty()) {
            throw new IllegalArgumentException(
                "No component(s) found for name(space)" + srcNamespace + "."
            );
        }
            
        var dst = this.viewers.get(dstName);
        srcNames.stream()
        .map((srcName) -> this.viewers.get(srcName))
        .forEach((src) -> {
            String dstPort = dstName + "_to_" + src.getIdentifier();
            String srcPort = src.getIdentifier() + "_to_" + dstName;
            if ((dst instanceof GenericMemoryModuleViewer && 
                src instanceof InstrumentedProcessingModuleViewer) ||
                (src instanceof GenericMemoryModuleViewer &&
                dst instanceof InstrumentedProcessingModuleViewer)) {
                throw new UnsupportedOperationException(
                    "Cannot directly connect processing modules and memory."
                );
            }
            dst.addPorts(dstPort);
            src.addPorts(srcPort);
            this.CreateEdge(src, dst, srcPort, dstPort);
        });
    }

    /**
     * Add a memory module to the platform.
     * @param name The identifier for the memory module.
     * @param frequency The operating frequency of the memory module.
     * @param spaceInBits The space in bits of the memory module.
     */
    public void AddMemory(String name, long frequency, long spaceInBits) {
        String memoryName = name;
        GenericMemoryModuleViewer mem = GenericMemoryModule.enforce(
            sGraph, sGraph.newVertex(memoryName)
        );
        this.viewers.put(memoryName, mem);
        this.platformGreyBox.addContained(Visualizable.enforce(mem));

        mem.operatingFrequencyInHertz(600 * Units.MHz);
        mem.spaceInBits(4 * Units.GB * Units.BYTES_TO_BITS);
    }

    /**
     * Adds a switch (communication element) to the platform
     * @param name The identifier for the switch.
     * @param frequency The operating frequency of the switch.
     */
    public void AddSwitch(String name, long frequency) {
        var sw = InstrumentedCommunicationModule.enforce(
            sGraph, sGraph.newVertex(name)
        );
        this.viewers.put(name, sw);
        this.platformGreyBox.addContained(Visualizable.enforce(sw));
        sw.operatingFrequencyInHertz(frequency);
        sw.initialLatency(0L);
        sw.flitSizeInBits((long)1 * Units.BYTES_TO_BITS);
        sw.maxCyclesPerFlit(1);   // cycles to send 1 byte
        sw.maxConcurrentFlits(1); // could match ports below with = 4
    }

    /**
     * Add a CPU to the MPSoC as <cores> independent processing modules with 
     * 1-1 mapped runtimes.
     * @param name The identifier for the CPU.
     * @param cores The number of CPU cores.
     * @param frequency The operating frequency of the CPU.
     * @param modalInstructions Available CPU instructions and their costs.
     */
    public void AddCPU(String name, int cores, long frequency,
                        Map<String, Map<String, Double>> modalInstructions) {
        for (int i = 0; i < cores; i++) {
            String coreName;
            if (cores > 1) 
                coreName = name + "_C" + i;
            else coreName = name;
            var core = InstrumentedProcessingModule.enforce(
                sGraph, sGraph.newVertex(coreName)
            );
            this.viewers.put(coreName, core);
            this.platformGreyBox.addContained(Visualizable.enforce(core));
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(frequency);
            core.modalInstructionsPerCycle(modalInstructions);

            var tdmApu = TimeDivisionMultiplexingRuntime.enforce(sGraph,
                sGraph.newVertex(coreName + "_Scheduler")
            );
            this.platformGreyBox.addContained(Visualizable.enforce(tdmApu));
            tdmApu.addManaged(core);
            this.CreateEdge(core, tdmApu);
        }
    }

    public void AddFPGA(String name, int availableLogicArea) {
        var fpga = LogicProgrammableModule.enforce(
            sGraph, sGraph.newVertex(name)
        );
        this.platformGreyBox.addContained(Visualizable.enforce(fpga));
        this.viewers.put(name, fpga);
        fpga.availableLogicArea(availableLogicArea);
    }
}
