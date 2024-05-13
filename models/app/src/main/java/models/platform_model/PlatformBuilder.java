
package models.platform_model;

import java.util.*;
import java.util.stream.Collectors;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedProcessingModuleViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;
import models.utils.Requirements;
import models.utils.Units;


public class PlatformBuilder {
    private SystemGraph sGraph;
    private GreyBoxViewer greyBox;
    public Map<String, VertexViewer> viewers = new HashMap<>();

    public PlatformBuilder(String name) {
        SystemGraph sGraph = new SystemGraph();

        var platform = Structure.enforce(sGraph, sGraph.newVertex(name));
        this.viewers.put(name, platform);
        this.greyBox = GreyBox.enforce(Visualizable.enforce(platform));
        this.sGraph = sGraph;
    }

    /**
     * Create a platform from an existing graph, by extraction of the viewers.
     * @param g The existing system graph to use as platform base.
     */
    public PlatformBuilder(String name, SystemGraph g) {
        this.sGraph = g;
        this.greyBox = g.vertexSet()
            .stream()
            .flatMap(v -> GreyBoxViewer.tryView(g, v).stream())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No GreyBox for visualization found in the given graph."
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
            LogicProgrammableModule.tryView(g, v).ifPresent(view -> 
                viewers.put(v.getIdentifier(), view)
            );
        }
    }

    /**
     * Get the system graph of the platform.
     * @return The system graph of the platform.
     */
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
            EdgeTraits.PhysicalConnection, EdgeTraits.VisualConnection
        );
        sGraph.connect(
            b, a, portB, portA, 
            EdgeTraits.PhysicalConnection, EdgeTraits.VisualConnection
        );
    }

    /**
     * Connect two hardware components bidirectionally and adds visual traits
     * to the connections as well.
     * @param a The first component.
     * @param b The second component.
     */
    private void CreateEdge(VertexViewer a, VertexViewer b) {
        sGraph.connect(
            a, b, EdgeTraits.PhysicalConnection, EdgeTraits.VisualConnection
        );
        sGraph.connect(
            b, a, EdgeTraits.PhysicalConnection, EdgeTraits.VisualConnection
        );
    }

    /**
     * Connect two components unidirectionally, thus order of arguments matter.
     * Can communicate a -> b, but not b -> a.
     * @param a The first component, source of the edge.
     * @param b The second component, destination of the edge.
     * @param portA The port of the first component.
     * @param portB The port of the second component.
     */
    private void CreateDirectedEdge(
        VertexViewer a, VertexViewer b, String portA, String portB
    ) {
        sGraph.connect(
            a, b, portA, portB, 
            EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection
        );
    }
    
    /**
     * Connect a component uni-directionally to another component.
     * @param srcCompName The name of the source component (must exist).
     * @param dstCompName The name of the destination component (must exist).
     */
    public void ConnectOneWay(String srcCompName, String dstCompName) {
        assert this.viewers.keySet().contains(srcCompName) :
            "No component named " + srcCompName + " found";
        assert this.viewers.keySet().contains(dstCompName) :
            "No component named " + dstCompName + " found";
        var src = this.viewers.get(srcCompName);
        var dst = this.viewers.get(dstCompName);
        String dstPort = "from_" + src.getIdentifier();
        String srcPort = "to_" + dst.getIdentifier();
        if ((dst instanceof GenericMemoryModuleViewer && 
            src instanceof InstrumentedProcessingModuleViewer) ||
            (src instanceof GenericMemoryModuleViewer &&
            dst instanceof InstrumentedProcessingModuleViewer)
        ) {
            throw new UnsupportedOperationException(
                "Cannot directly connect processing modules and memory."
            );
        }

        dst.addPorts(dstPort);
        src.addPorts(srcPort);
        this.CreateDirectedEdge(src, dst, srcPort, dstPort);
    }

    /**
     * Connect all components bidirectionally from the given namespace to 
     * another component.
     * @param srcCompNamespace The namespace or exact name of the source components.
     * (must exist).
     * @param dstCompName The name of the destination component (must exist).
     */
    public void ConnectTwoWay(String srcCompNamespace, String dstCompName) {
        assert this.viewers.keySet().contains(dstCompName) :
            "No component named " + dstCompName + " found";
        var dst = this.viewers.get(dstCompName);
        List<String> srcCompNames = this.viewers.keySet().stream()
            .filter((key) -> key.contains(srcCompNamespace))
            .filter((key) -> key.compareTo(dst.getIdentifier()) != 0)
            .toList();

        assert !srcCompNames.isEmpty() :
            "No component(s) found for name(space) " + srcCompNamespace +
            " when trying to add connections to " + dstCompName;
        
        srcCompNames.stream()
        .map((srcCompName) -> this.viewers.get(srcCompName))
        .forEach((srcComp) -> {
            String dstPort = "to_from_" + srcComp.getIdentifier();
            String srcPort = "to_from_" + dstCompName;
            if ((dst instanceof GenericMemoryModuleViewer && 
                srcComp instanceof InstrumentedProcessingModuleViewer) ||
                (srcComp instanceof GenericMemoryModuleViewer &&
                dst instanceof InstrumentedProcessingModuleViewer)
            ) {
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
     * Add physical memory to the platform.
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
        this.greyBox.addContained(Visualizable.enforce(mem));

        mem.operatingFrequencyInHertz(600 * Units.MHz);
        mem.spaceInBits(4 * Units.GB * Units.BYTES_TO_BITS);
    }

    /**
     * Adds a switch (communication element) to the platform
     * traversalTime(dataSize) = initialLatency
     *  + ceil(dataSize / flitSizeInBits) * (maxCyclesPerFlit / maxConcurrentFlits) / elementFrequency
     * @param name The identifier for the switch.
     * @param frequency The operating frequency of the switch.
     * @param flitInBits The size of the flit in bits.
     */
    public void AddSwitch(String name, long frequency, long flitInBits) {
        var sw = InstrumentedCommunicationModule.enforce(
            sGraph, sGraph.newVertex(name)
        );
        this.viewers.put(name, sw);
        this.greyBox.addContained(Visualizable.enforce(sw));
        sw.operatingFrequencyInHertz(frequency);
        sw.initialLatency(0L);
        sw.flitSizeInBits(flitInBits);
        sw.maxCyclesPerFlit(Integer.MAX_VALUE); // EASY TO CHANGE BANDWIDTH HERE
        sw.maxConcurrentFlits(1);
    }

    /**
     * Add routes inside a switch that connect ports arbitrarily. Component names
     * part of `routes` will be mapped to corresponding port names automatically.
     * @param name The identifier for the switch.
     * @param routes A map of component names that the switch has ports and
     * connections to
     */
    public void AddInternalSwitchRoutes(
        String name, Map<String, List<String>> routes
    ) {
        var sw = this.viewers.get(name);
        assert sw != null : "Switch " + name + " not found.";
        var ports = sw.getPorts();

        routes.forEach((compName, connectsToNames) -> {
            assert ports.stream().anyMatch(p -> p.contains(compName)) : 
                "Switch port for " + compName + " not found, ports: " + ports;
            assert connectsToNames.stream().allMatch(
                v -> ports.stream().anyMatch(p -> p.contains(v))
            ) : "Switch port for " + connectsToNames + " not found, ports: " + ports;
        });
        
        HashMap<String, List<String>> portConnections = new HashMap<>();
        routes.forEach((compName, connectsToNames) -> {
            String portName = ports.stream()
                .filter(p -> p.contains(compName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Port for component " + compName + " not found in " + ports +
                    " (port connections for " + name + ")"
                ));
            var connectsToPorts = connectsToNames.stream()
                .map(v -> ports.stream()
                    .filter(p -> p.contains(v))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Port for component " + v + " not found in " + ports +
                        " (port connections for " + name + ")"
                    ))
                )
                .toList();

            
            // System.out.println(name + ": "+compName+" <--> "+connectsToNames);
            // System.out.println("-   ports: "+ports);
            // System.out.println("-   port: "+portName);
            // for (int i = 0; i < connectsToNames.size(); i++) {
            //     System.out.println("-   "+connectsToNames.get(i)+": "+connectsToPorts.get(i));
            // }
            portConnections.put(portName, connectsToPorts);
        });

        CommunicationModulePortSpecification.enforce(
            sGraph, sw.getViewedVertex()
        ).portConnections(portConnections);
    }

    /**
     * Add a CPU to the platform as <numCores> independent processing modules with 
     * 1-1 mapped runtimes.
     * @param name The identifier for the CPU.
     * @param numCores The number of CPU cores.
     * @param frequency The operating frequency of the CPU.
     * @param instructionSet Available CPU instructions and their cycle cost.
     */
    public void AddCPU(String name, int numCores, long frequency,
                    Map<String, Map<String, Integer>> instructionSet) {
        for (int i = 0; i < numCores; i++) {
            String coreName;
            if (numCores > 1) coreName = name + "_C" + i;
            else coreName = name;
            var core = InstrumentedProcessingModule.enforce(
                sGraph, sGraph.newVertex(coreName)
            );
            this.viewers.put(coreName, core);
            this.greyBox.addContained(Visualizable.enforce(core));
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(frequency);
            core.modalInstructionsPerCycle( //TODO: must test!!!!!!!1
                instructionSet.entrySet().stream().collect(
                    Collectors.toMap(
                        iSet -> iSet.getKey(),
                        iSet -> iSet.getValue().entrySet().stream().collect(
                            Collectors.toMap(
                                inst -> inst.getKey(),
                                inst -> 1.0 / inst.getValue()
                            )
                        )
                    )));

            var tdmApu = SuperLoopRuntime.enforce(sGraph,
                sGraph.newVertex(coreName + "_Scheduler")
            );
            this.greyBox.addContained(Visualizable.enforce(tdmApu));
            tdmApu.addManaged(core);
            this.CreateEdge(core, tdmApu);
        }
    }

    /**
     * Add an FPGA to the platform.
     * @param name The identifier for the FPGA.
     * @param availableLogicArea The available logic area of the FPGA.
     * @param bramSizeInBits The size of the block RAM in bits.
     * @param bramFlitSize The size of the flit in bits for the BRAM.
     * @param frequency The operating frequency of the FPGA and BRAM.
     */
    public void AddFPGA(
        String name, int availableLogicArea, int bramSizeInBits, 
        int bramFlitSize, long frequency
    ) {
        var fpga = LogicProgrammableModule.enforce(
            sGraph, sGraph.newVertex(name)
        );
        this.greyBox.addContained(Visualizable.enforce(fpga));
        this.viewers.put(name, fpga);
        fpga.availableLogicArea(availableLogicArea);
        fpga.blockRamSizeInBits(bramSizeInBits);
        fpga.operatingFrequencyInHertz(frequency);

        if (bramSizeInBits > 0) {
            var swName = name + "_BRAM_SWITCH";
            var bramName = name + "_BRAM";

            AddSwitch(swName, frequency, bramFlitSize);
            AddMemory(bramName, frequency, bramSizeInBits);
            ConnectTwoWay(name, swName);
            ConnectTwoWay(swName, bramName);
        }
    }
}
