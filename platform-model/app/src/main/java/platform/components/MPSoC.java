package platform.components;

import java.util.*;

import org.antlr.v4.runtime.misc.Pair;

import forsyde.io.core.EdgeTrait;
import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedCommunicationModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedProcessingModuleViewer;

public class MPSoC {

    public SystemGraph sGraph;
    private StructureViewer platform = null;
    private StructureViewer PS = null;
    private StructureViewer APU = null;
    private StructureViewer RPU = null;

    private GreyBoxViewer platformGreyBox = null;
    private GreyBoxViewer psGreyBox = null;
    public MPSoC() {
        SystemGraph sGraph = new SystemGraph();

        StructureViewer platform = Structure.enforce(
                sGraph, sGraph.newVertex("MPSoC"));
        var platformGreyBox = GreyBox.enforce(Visualizable.enforce(platform));

        StructureViewer PS = Structure.enforce(sGraph, sGraph.newVertex("MPSoC.PS"));
        sGraph.connect(platform, PS, EdgeTraits.StructuralContainment);
        platformGreyBox.addContained(Visualizable.enforce(PS));
        var psGreyBox = GreyBox.enforce(PS);

        this.sGraph = sGraph;
        this.platform = platform;
        this.PS = PS;
        this.platformGreyBox = platformGreyBox;
        this.psGreyBox = psGreyBox;
    }

    // Real-time Processing Unit (RPU): 2 cores @ 600 MHz
    // - Execution time varies between instructions
    // Connected to platform structurally
    public List<InstrumentedProcessingModuleViewer> rpuCores = new ArrayList<>();
    public void AddRPU() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        final String RPU_NAME = "MPSoC.PS.RPU";
        StructureViewer RPU = Structure.enforce(
                sGraph, sGraph.newVertex(RPU_NAME));
        sGraph.connect(PS, RPU, EdgeTraits.StructuralContainment);

        var rpuGreyBox = GreyBox.enforce(Visualizable.enforce(RPU));

        int rpuCores = 2;
        for (int i = 0; i < rpuCores; i++) {
            var core = InstrumentedProcessingModule.enforce(
                    sGraph, sGraph.newVertex(RPU_NAME + ".C" + i));
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(15 * 100000000L);
            core.modalInstructionsPerCycle(Map.of(
                "economy", Map.of(
                    "FloatOp", 0.43, // the applicaiton must provide a subset of these instructions
                    "NonFloatOp", 2.325
                )
            ));

            core.addPorts("portToOCMSwitch");

            sGraph.connect(APU, core, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
            rpuGreyBox.addContained(Visualizable.enforce(core));

            this.rpuCores.add(core);
        }

        this.RPU = RPU;
        this.psGreyBox.addContained(rpuGreyBox);
    }

    public List<InstrumentedProcessingModuleViewer> apuCores = new ArrayList<>();

    // Application Processing Unit (APU): 4 cores @ 1.5 GHz
    // - Execution time varies between instructions
    // Connected to platform structurally
    public void AddAPU() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        final String APU_NAME = "MPSoC.PS.APU";
        StructureViewer APU = Structure.enforce(sGraph, sGraph.newVertex(APU_NAME));
        sGraph.connect(PS, APU, EdgeTraits.StructuralContainment);
        var apuGreyBox = GreyBox.enforce(Visualizable.enforce(APU));

        int apuCores = 4;
        for (int i = 0; i < apuCores; i++) {
            var core = InstrumentedProcessingModule.enforce(
                    sGraph, sGraph.newVertex(APU_NAME + ".C" + i));
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(15 * 100000000L);
            core.modalInstructionsPerCycle(Map.of(
                "economy", Map.of(
                    "FloatOp", 0.43, // the applicaiton must provide a subset of these instructions
                    "NonFloatOp", 2.325
                )
            ));

            core.addPorts("portToOCMSwitch");

            sGraph.connect(APU, core, EdgeTraits.StructuralContainment);
            apuGreyBox.addContained(Visualizable.enforce(core));

            this.apuCores.add(core);
        }

        this.APU = APU;
        this.psGreyBox.addContained(apuGreyBox);
    }

    // Memory: 4GB, 600 MHz
    public void AddOCM() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        GenericMemoryModuleViewer ocm = GenericMemoryModule.enforce(
                sGraph, sGraph.newVertex("MPSoC.PS.OCM"));
        ocm.operatingFrequencyInHertz(6 * 100000000L);
        ocm.spaceInBits(4 * 8 * 1024 * 1024 * 1024L); // 4GB

        ocm.getViewedVertex().addPort("OCMPortToSwitch");
        sGraph.connect(this.OcmSwitch, ocm, "OCMSwitchPort1", "OCMPortToSwitch");
        sGraph.connect(ocm, this.OcmSwitch, "OCMPortToSwitch", "OCMSwitchPort1");

        sGraph.connect(this.platform, ocm, EdgeTraits.StructuralContainment);
        this.platformGreyBox.addContained(Visualizable.enforce(ocm));
    }

    public InstrumentedCommunicationModuleViewer OcmSwitch = null;

    public void AddOCMSwitch() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        InstrumentedCommunicationModuleViewer ocmSwitch = InstrumentedCommunicationModule.enforce(
                sGraph, sGraph.newVertex("MPSoC.PS.OCMSwitch"));

        // scheduler?????
        ForSyDeHierarchy.TimeDivisionMultiplexingRuntime.enforce(
                sGraph, sGraph.newVertex("MPSoC.PS.OCMSwitch.TDM"));

        // can't find round robin class
        ocmSwitch.operatingFrequencyInHertz(600 * 100000000L);
        ocmSwitch.flitSizeInBits(8L);
        ocmSwitch.initialLatency(0L);
        ocmSwitch.maxCyclesPerFlit(1); // cycles to send 1 byte
        ocmSwitch.maxConcurrentFlits(1); // could match ports below with = 4

        ocmSwitch.addPorts("OCMSwitchPort1", "OCMSwitchPort2", "OCMSwitchPort3", "OCMSwitchPort4");
        for (int i = 0; i < 4; i++) {
            sGraph.connect(
                    this.apuCores.get(i),
                    ocmSwitch,
                    "portToOCMSwitch",
                    "OCMSwitchPort" + (i + 1),
                    EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(
                    ocmSwitch,
                    this.apuCores.get(i),
                    "OCMSwitchPort" + (i + 1),
                    "portToMemSwitch",
                    EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);

            if (i >= this.rpuCores.size()) {
                continue;
            }
            sGraph.connect(
                    this.rpuCores.get(i),
                    ocmSwitch,
                    "portToOCMSwitch",
                    "OCMSwitchPort" + (i + 1),
                    EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(
                    ocmSwitch,
                    this.rpuCores.get(i),
                    "OCMSwitchPort" + (i + 1),
                    "portToMemSwitch",
                    EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
        }

        sGraph.connect(this.platform, ocmSwitch, EdgeTraits.StructuralContainment);
        this.platformGreyBox.addContained(Visualizable.enforce(ocmSwitch));

        this.OcmSwitch = ocmSwitch;
    }

    public void AddFPGA() {
        // TODO
    }
}
