
package models.platform_model.components;

import java.util.*;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedCommunicationModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedProcessingModuleViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;

import models.utils.Units;

public class MPSoC {
    public SystemGraph sGraph;
    private GreyBoxViewer platformGreyBox = null;
    private Map<InstrumentedProcessingModuleViewer, String> PSCores = new HashMap<>();
    // private StructureViewer platform = null;
    // private StructureViewer PS = null;
    // private GreyBoxViewer psGreyBox = null;
    // private InstrumentedCommunicationModuleViewer OCMSwitch = null;
    // private StructureViewer APU = null;
    // private StructureViewer RPU = null;

    public MPSoC() {
        SystemGraph sGraph = new SystemGraph();

        StructureViewer platform = Structure.enforce(
                sGraph, sGraph.newVertex("MPSoC"));
        var platformGreyBox = GreyBox.enforce(Visualizable.enforce(platform));

        StructureViewer PS = Structure.enforce(sGraph, sGraph.newVertex("MPSoC_PS"));
        sGraph.connect(platform, PS, EdgeTraits.StructuralContainment);
        platformGreyBox.addContained(Visualizable.enforce(PS));

        this.sGraph = sGraph;
        this.platformGreyBox = platformGreyBox;
    }

    private void BiDirectionalConnect(VertexViewer a, VertexViewer b, String portA, String portB) {
        sGraph.connect(a, b, portA, portB, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        sGraph.connect(b, a, portB, portA, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
    }

    private void BiDirectionalConnect(VertexViewer a, VertexViewer b) {
        sGraph.connect(a, b, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        sGraph.connect(b, a, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
    }

    public void AddMemoryToPS(long frequency, long spaceInBits) {
        // memory
        GenericMemoryModuleViewer ocm = GenericMemoryModule.enforce(
                sGraph, sGraph.newVertex("MPSoC_PS_Mem"));
        ocm.operatingFrequencyInHertz(600 * Units.MHZ);
        ocm.spaceInBits(4 * Units.GB * Units.BYTES_TO_BITS);
        ocm.addPorts("toMemSwitch");
        platformGreyBox.addContained(Visualizable.enforce(ocm));

        // memory switch
        var swith = InstrumentedCommunicationModule.enforce(
                sGraph, sGraph.newVertex("MPSoC_PS_MemSwitch"));
        swith.operatingFrequencyInHertz(600 * Units.MHZ);
        swith.initialLatency(0L);
        swith.flitSizeInBits(8L); // 1 byte
        swith.maxCyclesPerFlit(1); // cycles to send 1 byte
        swith.maxConcurrentFlits(1); // could match ports below with = 4
        platformGreyBox.addContained(Visualizable.enforce(swith));
        swith.addPorts("toPU");

        // connect all added cores to switch
        this.PSCores.forEach((core, moduleName) -> {
            this.BiDirectionalConnect(core, swith, "toMemSwitch", "toPU");
        });

        // connect switch to memory
        this.BiDirectionalConnect(ocm, swith, "toMemSwitch", "toPU");
    }

    public void AddProcessingSystemModule(String name, int cores, long frequency,
            Map<String, Map<String, Double>> modalInstructions) {
        String moduleName = "MPSoC_PS_" + name;
        StructureViewer module = Structure.enforce(sGraph, sGraph.newVertex(moduleName));
        GreyBoxViewer box = GreyBox.enforce(Visualizable.enforce(module));

        for (int i = 0; i < cores; i++) {
            String coreName = moduleName + "_C" + i;
            var core = InstrumentedProcessingModule.enforce(
                    sGraph, sGraph.newVertex(coreName));
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz((long) 1.5 * Units.GHZ);
            core.modalInstructionsPerCycle(Map.of(
                    "economy", Map.of(
                            "FloatOp", 0.43)));
            core.addPorts("toMemSwitch");
            platformGreyBox.addContained(Visualizable.enforce(core));

            var tdmApu = TimeDivisionMultiplexingRuntime.enforce(sGraph,
                    sGraph.newVertex(coreName + "_Scheduler"));
            tdmApu.addManaged(core);

            this.BiDirectionalConnect(core, tdmApu);

            platformGreyBox.addContained(Visualizable.enforce(tdmApu));

            this.PSCores.put(core, moduleName);
        }

        this.platformGreyBox.addContained(box);
    }

    public void AddFPGA() {
        // TODO
    }
}
