
package models.platform_model.components;

import java.util.*;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedProcessingModuleViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;

import models.utils.Units;


public class Platform {
    private SystemGraph sGraph;
    private GreyBoxViewer platformGreyBox;
    public Map<String, VertexViewer> viewers = new HashMap<>();
    public static enum SwitchType {
        ROUTER, MEM_INTERFACE
    }

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
                "No GreyBox, required for visualization, found in the given graph."
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
     * Connect all components from the given namespace to another component.
     * @param srcCompNamespace The namespace or exact name of the source components.
     * (must exist).
     * @param dstCompName The exact name of the destination component (must exist).
     */
    public void Connect(String srcCompNamespace, String dstCompName) {
        if (!this.viewers.keySet().contains(dstCompName)) {
            throw new IllegalArgumentException(
                "No component named " + dstCompName + " found."
            );
        }
        
        List<String> srcCompNames = this.viewers.keySet().stream()
            .filter((key) -> key.contains(srcCompNamespace))
            .toList();

        if (srcCompNames.isEmpty()) {
            throw new IllegalArgumentException(
                "No component(s) found for name(space) " + srcCompNamespace +
                " when trying to add connections to " + dstCompName + "."
            );
        }
            
        var dst = this.viewers.get(dstCompName);
        srcCompNames.stream()
        .map((srcCompName) -> this.viewers.get(srcCompName))
        .forEach((srcComp) -> {
            String dstPort = dstCompName + "_to_" + srcComp.getIdentifier();
            String srcPort = srcComp.getIdentifier() + "_to_" + dstCompName;
            if ((dst instanceof GenericMemoryModuleViewer && 
                srcComp instanceof InstrumentedProcessingModuleViewer) ||
                (srcComp instanceof GenericMemoryModuleViewer &&
                dst instanceof InstrumentedProcessingModuleViewer)) {
                throw new UnsupportedOperationException(
                    "Cannot directly connect processing modules and memory."
                );
            }
            dst.addPorts(dstPort);
            srcComp.addPorts(srcPort);
            this.CreateEdge(srcComp, dst, srcPort, dstPort);
        });
    }

    /**
     * Connect all components from the given namespace to another component with
     * a switch in between. One switch is created for each source component to
     * overcome the IDeSyDe assumption that it can route traffic between its
     * components when, in reality, just being.
     * @param srcCompNamespace
     * @param dstCompName
     * @param frequency
     */
    public void ConnectToMemory(
        String srcCompNamespace, String memName, long frequency
    ) {
        if (!this.viewers.keySet().contains(memName)) {
            throw new IllegalArgumentException(
                "No memory named " + memName + " found."
            );
        }
        
        List<String> srcCompNames = this.viewers.keySet().stream()
            .filter((key) -> key.contains(srcCompNamespace))
            .toList();

        if (srcCompNames.isEmpty()) {
            throw new IllegalArgumentException(
                "No component(s) found for name(space) " + srcCompNamespace +
                " when trying to add connections to " + memName + "."
            );
        }

        // add switch between each source component and the destination component
        // to avoid the switch becoming a "router"
        for (var srcCompName : srcCompNames) {
            String switchName = memName + "_" + srcCompName + "_SWITCH";
            if (this.viewers.keySet().contains(switchName)) {
                throw new IllegalArgumentException(
                    "Switch " + switchName + " already exists."
                );
            }
            this.AddRouter(switchName, frequency);
            var sw = this.viewers.get(switchName);
            String swToSrc = switchName + "_to_" + srcCompName;
            sw.addPorts(swToSrc);
            String swToDst = switchName + "_to_" + memName;
            sw.addPorts(swToDst);

            var srcComp = this.viewers.get(srcCompName);
            String srcToSw = srcCompName + "_to_" + switchName;
            srcComp.addPorts(srcToSw);
            var dstComp = this.viewers.get(memName);
            String dstToSw = memName + "_to_" + switchName;
            dstComp.addPorts(dstToSw);
        
            this.CreateEdge(sw, srcComp, swToSrc, srcToSw);
            this.CreateEdge(sw, dstComp, swToDst, dstToSw);
        }
    }

    /**
     * Add a memory module to the platform.
     * @param name The identifier for the memory module.
     * @param frequency The operating frequency of the memory module.
     * @param spaceInBits The space in bits of the memory module.
     */
    public void AddMemory(String name, long frequency, long spaceInBits) {
        String memoryName = name;
        var mem = GenericMemoryModule.enforce(
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
    public void AddRouter(String name, long frequency) {
        var sw = InstrumentedCommunicationModule.enforce(
            sGraph, sGraph.newVertex(name)
        );
        this.viewers.put(name, sw);
        this.platformGreyBox.addContained(Visualizable.enforce(sw));
        sw.operatingFrequencyInHertz(frequency);
        sw.initialLatency(0L);
        sw.flitSizeInBits((long)1 * Units.BYTES_TO_BITS);
        sw.maxCyclesPerFlit(1);   // cycles to send 1 byte
        sw.maxConcurrentFlits(1); //! exactly match the number of connections?
    }

    /**
     * Add a CPU to the MPSoC as <cores> independent processing modules with 
     * 1-1 mapped runtimes.
     * @param name The identifier for the CPU.
     * @param numCores The number of CPU cores.
     * @param frequency The operating frequency of the CPU.
     * @param modalInstructions Available CPU instructions and their costs.
     */
    public void AddCPU(String name, int numCores, long frequency,
                        Map<String, Map<String, Double>> modalInstructions) {
        for (int i = 0; i < numCores; i++) {
            String coreName;
            if (numCores > 1) 
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
