
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

    public MPSoC(boolean allInOne) {
        SystemGraph sGraph = new SystemGraph();

        StructureViewer platform = Structure.enforce(
                sGraph, sGraph.newVertex("MPSoC"));
        var platformGreyBox = GreyBox.enforce(Visualizable.enforce(platform));

        StructureViewer PS = Structure.enforce(sGraph, sGraph.newVertex("MPSoC_PS"));
        sGraph.connect(platform, PS, EdgeTraits.StructuralContainment);
        platformGreyBox.addContained(Visualizable.enforce(PS));

        this.sGraph = sGraph;
        this.platformGreyBox = platformGreyBox;
        if (allInOne) {
            // RPU
            // var rpu = InstrumentedProcessingModule.enforce(
            // sGraph, sGraph.newVertex("MPSoC_PS_RPU"));
            // rpu.maximumComputationParallelism(1);
            // rpu.operatingFrequencyInHertz(600 * Units.MHZ);
            // rpu.modalInstructionsPerCycle(Map.of(
            // "default", Map.of(
            // "FloatOp", 0.84)));
            // rpu.addPorts("toRuntime", "toMemSwitch");
            // platformGreyBox.addContained(Visualizable.enforce(rpu));

            // var tdmRpu = TimeDivisionMultiplexingRuntime.enforce(sGraph,
            // sGraph.newVertex("MPSoC_PS_RPU_Scheduler"));
            // tdmRpu.addManaged(rpu);
            // sGraph.connect(rpu, tdmRpu, EdgeTraits.StructuralContainment,
            // EdgeTraits.VisualConnection);
            // sGraph.connect(tdmRpu, rpu, EdgeTraits.StructuralContainment,
            // EdgeTraits.VisualConnection);
            // platformGreyBox.addContained(Visualizable.enforce(tdmRpu));

            // APU
            var apu = InstrumentedProcessingModule.enforce(
                    sGraph, sGraph.newVertex("MPSoC_PS_APU"));
            apu.maximumComputationParallelism(1);
            apu.operatingFrequencyInHertz((long) 1.5 * Units.GHZ);
            apu.modalInstructionsPerCycle(Map.of(
                    "economy", Map.of(
                            "FloatOp", 0.43)));
            apu.addPorts("toRuntime", "toMemSwitch");
            platformGreyBox.addContained(Visualizable.enforce(apu));

            var tdmApu = TimeDivisionMultiplexingRuntime.enforce(sGraph,
                    sGraph.newVertex("MPSoC_PS_APU_Scheduler"));
            tdmApu.addManaged(apu);
            // sGraph.connect(tdmApu, apu, EdgeTraits.PhysicalConnection,
            //         EdgeTraits.VisualConnection);
            // sGraph.connect(apu, tdmApu, EdgeTraits.PhysicalConnection,
            //         EdgeTraits.VisualConnection);
            // platformGreyBox.addContained(Visualizable.enforce(tdmApu));

            // this.AddProcessingSystemModule("APU", 2, 2 * Units.GHZ, Map.of(
            // "economy", Map.of(
            // "FloatOp", 0.43, // the applicaiton must provide a subset of these
            // instructions
            // "NonFloatOp", 2.325)));

            var swith = InstrumentedCommunicationModule.enforce(
                    sGraph, sGraph.newVertex("MPSoC_PS_MemSwitch"));
            swith.operatingFrequencyInHertz(600 * Units.MHZ);
            swith.initialLatency(0L);
            swith.flitSizeInBits(8L); // 1 byte
            swith.maxCyclesPerFlit(1); // cycles to send 1 byte
            swith.maxConcurrentFlits(1); // could match ports below with = 4
            platformGreyBox.addContained(Visualizable.enforce(swith));
            swith.addPorts("toPU");
            this.comm = swith;

            sGraph.connect(apu, swith, "toMemSwitch", "toPU", EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(swith, apu, "toPU", "toMemSwitch", EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            // sGraph.connect(rpu, scheduler, "toMemSwitch", "toPU",
            // EdgeTraits.PhysicalConnection,
            // EdgeTraits.VisualConnection);
            // sGraph.connect(scheduler, rpu, "toPU", "toMemSwitch",
            // EdgeTraits.PhysicalConnection,
            // EdgeTraits.VisualConnection);

            this.AddMemoryToPS(600 * Units.MHZ, 4 * Units.GB * Units.BYTES_TO_BITS);

            // GenericMemoryModuleViewer ocm = GenericMemoryModule.enforce(
            // sGraph, sGraph.newVertex("MPSoC_PS_OCM"));
            // ocm.operatingFrequencyInHertz(600 * Units.MHZ);
            // ocm.spaceInBits(4 * Units.GB * Units.BYTES_TO_BITS);
            // platformGreyBox.addContained(Visualizable.enforce(ocm));

            // sGraph.connect(swith, ocm, EdgeTraits.PhysicalConnection,
            // EdgeTraits.VisualConnection);
            // sGraph.connect(ocm, swith, EdgeTraits.PhysicalConnection,
            // EdgeTraits.VisualConnection);

        }
    }

    private InstrumentedCommunicationModuleViewer comm;
    private InstrumentedProcessingModuleViewer core;

    private void BiDirectionalConnect(VertexViewer a, VertexViewer b, String portA, String portB) {
        sGraph.connect(a, b, portA, portB, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        sGraph.connect(b, a, portB, portA, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
    }

    private void BiDirectionalConnect(VertexViewer a, VertexViewer b) {
        sGraph.connect(a, b, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        sGraph.connect(b, a, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
    }

    public void AddMemoryToPS(long frequency, long spaceInBits) {
        // actual memory
        GenericMemoryModuleViewer ocm = GenericMemoryModule.enforce(
                sGraph, sGraph.newVertex("MPSoC_PS_Mem"));
        ocm.operatingFrequencyInHertz(600 * Units.MHZ);
        ocm.spaceInBits(4 * Units.GB * Units.BYTES_TO_BITS);
        ocm.addPorts("toMemSwitch");
        platformGreyBox.addContained(Visualizable.enforce(ocm));

        // memory switch
        var memSwitch = InstrumentedCommunicationModule.enforce(
                sGraph, sGraph.newVertex("MPSoC_PS_MemSwitch"));
        memSwitch.operatingFrequencyInHertz(600 * Units.MHZ);
        memSwitch.initialLatency(0L);
        memSwitch.flitSizeInBits(8L); // 1 byte
        memSwitch.maxCyclesPerFlit(1); // cycles to send 1 byte
        memSwitch.maxConcurrentFlits(1); // could match ports below with = 4
        memSwitch.addPorts("toPU", "toMem");
        platformGreyBox.addContained(Visualizable.enforce(memSwitch));

        // connect all added cores to switch
        // this.PSCores.forEach((core, moduleName) -> {
        //     this.BiDirectionalConnect(core, ocm, "toMemSwitch", "toPU");
        // });
        this.BiDirectionalConnect(this.core, memSwitch, "toMemSwitch", "toPU");

        // connect switch to memory
        this.BiDirectionalConnect(ocm, this.comm);
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
            core.maximumComputationParallelism(cores);
            core.operatingFrequencyInHertz(frequency);
            core.modalInstructionsPerCycle(modalInstructions);
            core.addPorts("toRuntime", "toMemSwitch");

            sGraph.connect(module, core, EdgeTraits.StructuralContainment);
            this.platformGreyBox.addContained(Visualizable.enforce(core));

            var tdmApu = TimeDivisionMultiplexingRuntime.enforce(sGraph,
                    sGraph.newVertex(coreName + "_Runtime"));
            tdmApu.addManaged(core);
            // sGraph.connect(tdmApu, core, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
            // sGraph.connect(core, tdmApu, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
            platformGreyBox.addContained(Visualizable.enforce(tdmApu));

            this.PSCores.put(core, moduleName);
            this.core = core;
        }

        this.platformGreyBox.addContained(box);
    }

    public void AddFPGA() {
        // TODO
    }
}
