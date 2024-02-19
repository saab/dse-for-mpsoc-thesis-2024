package platform.components;

import java.util.Map;

import org.antlr.v4.runtime.misc.Pair;

import forsyde.io.core.EdgeTrait;
import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedCommunicationModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;

public class MPSoC {
    
    public SystemGraph sGraph;
    private StructureViewer platform = null;
    private StructureViewer PS = null;
    private StructureViewer APU = null;
    private StructureViewer PL = null;
    private StructureViewer RPU = null;
    
    public MPSoC() {
        SystemGraph sGraph = new SystemGraph();
        
        // whole platform
        StructureViewer platform = Structure.enforce(
            sGraph, sGraph.newVertex("MPSoC")
        );
        Visualizable.enforce(platform);
        // GreyBox.enforce(platform);

        StructureViewer PS = Structure.enforce(sGraph, sGraph.newVertex("MPSoC.PS"));
        sGraph.connect(platform, PS, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        GreyBox.enforce(PS);

        StructureViewer PL = Structure.enforce(sGraph, sGraph.newVertex("MPSoC.PL"));
        sGraph.connect(platform, PL, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        GreyBox.enforce(PL);
        
        this.sGraph = sGraph;
        this.platform = platform;
        this.PS = PS;
        this.PL = PL;
    }
        
    // Application Processing Unit (APU): 4 cores @ 1.5 GHz
    // - Execution time varies between instructions
    // Connected to platform structurally
    public void AddAPU() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        final String APU_NAME = "MPSoC.PS.APU";
        StructureViewer APU = Structure.enforce(
            sGraph, sGraph.newVertex(APU_NAME)
        );
        sGraph.connect(PS, APU, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        Visualizable.enforce(APU);
        GreyBox.enforce(APU);

        int apuCores = 4;
        for (int i = 0; i < apuCores; i++) {
            var core = InstrumentedProcessingModule.enforce(
                sGraph, sGraph.newVertex(APU_NAME + ".C" + i)
            );
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(15 * 100000000L);
            core.modalInstructionsPerCycle(Map.of(
                "defaultTicks", Map.of(
                    "tick", 1.0
                ),
                "defaultNeeds", Map.of(
                    "FloatOp", 0.43,
                    "NonFloatOp", 2.325
                )
            ));
            core.addPorts("portToMem");
            core.getViewedVertex().putProperty("portWidthInBits", Map.of("portToMem", 128));
            core.getViewedVertex().putProperty("portIsInitiator", Map.of("portToMem", true));

            sGraph.connect(APU, core, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
            Visualizable.enforce(core);
            GreyBox.enforce(core);
        }

        this.APU = APU;
    }
    
    // Real-time Processing Unit (RPU): 2 cores @ 600 MHz
    // - Execution time varies between instructions
    // Connected to platform structurally
    public void AddRPU() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        final String RPU_NAME = "MPSoC.PS.RPU";
        StructureViewer RPU = Structure.enforce(
            sGraph, sGraph.newVertex(RPU_NAME)
        );
        sGraph.connect(PS, RPU, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        Visualizable.enforce(RPU);
        GreyBox.enforce(RPU);


        int rpuCores = 2;
        for (int i = 0; i < rpuCores; i++) {
            var core = InstrumentedProcessingModule.enforce(
                sGraph, sGraph.newVertex(RPU_NAME + ".C" + i)
            );
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(15 * 100000000L);
            core.modalInstructionsPerCycle(Map.of(
                "defaultTicks", Map.of(
                    "tick", 1.0
                ),
                "defaultNeeds", Map.of(
                    "FloatOp", 0.43,
                    "NonFloatOp", 2.325
                )
            ));
            core.addPorts("portToMem");
            core.getViewedVertex().putProperty("portWidthInBits", Map.of("portToMem", 128));
            core.getViewedVertex().putProperty("portIsInitiator", Map.of("portToMem", true));

            sGraph.connect(APU, core, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
            Visualizable.enforce(core);
            GreyBox.enforce(core);
        }

        this.RPU = RPU;
    }



    // Memory: 4GB, 600 MHz
    public void AddOCM() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        GenericMemoryModuleViewer ocm = GenericMemoryModule.enforce(
            sGraph, sGraph.newVertex("MPSoC.PS.OCM")
        );
        ocm.operatingFrequencyInHertz(6 * 100000000L);
        ocm.spaceInBits(4 * 8 * 1024 * 1024 * 1024L); // 4GB

        ocm.getViewedVertex().addPort("OCM1Port1");
        ocm.getViewedVertex().putProperty("portWidthInBits", Map.of("OCM1Port1", 128));
        ocm.getViewedVertex().putProperty("portIsInitiator", Map.of("OCM1Port1", false));
        ocm.getViewedVertex().putProperty("portProtocolAcronym", Map.of("OCM1Port1", "AXI4"));
        
        // int apuCores = 4;
        // for (int i = 0; i < apuCores; i++) {
        //     String portName = "APU.C" + i;
        //     psMemory.getViewedVertex().addPort(portName);
        //     sGraph.connect(
        //         psMemory,
        //         APU,
        //         "psMem",
        //         portName,
        //         EdgeTraits.PhysicalConnection,
        //         EdgeTraits.VisualConnection
        //     );
        //     sGraph.connect(
        //         psMemory,
        //         APU,
        //         "psMem",
        //         portName,
        //         EdgeTraits.PhysicalConnection,
        //         EdgeTraits.VisualConnection
        //     );
        // }

        // RPU connections
        // int rpuCores = 2;
        // for (int i = 0; i < rpuCores; i++) {
        //     String portName = "RPU.C" + i;
        //     psMemory.getViewedVertex().addPort(portName);
        //     sGraph.connect(
        //         psMemory,
        //         APU,
        //         "psMem",
        //         portName,
        //         EdgeTraits.PhysicalConnection,
        //         EdgeTraits.VisualConnection
        //     );
        //     sGraph.connect(
        //         psMemory,
        //         APU,
        //         "psMem",
        //         portName,
        //         EdgeTraits.PhysicalConnection,
        //         EdgeTraits.VisualConnection
        //     );
        // }

        //TODO: PL Memory

        sGraph.connect(this.platform, ocm, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        Visualizable.enforce(ocm);
        GreyBox.enforce(ocm);
    }

    public void AddOCMSwitch() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        InstrumentedCommunicationModuleViewer ocmSwitch = InstrumentedCommunicationModule.enforce(
            sGraph, sGraph.newVertex("MPSoC.PS.OCMSwitch")
        );

        // scheduler?????
        ForSyDeHierarchy.TimeDivisionMultiplexingRuntime.enforce(
            sGraph, sGraph.newVertex("MPSoC.PS.OCMSwitch.TDM")
        );
        
        // can't find round robin class
        ocmSwitch.operatingFrequencyInHertz(600 * 100000000L);
        ocmSwitch.flitSizeInBits(8L);
        ocmSwitch.initialLatency(0L);
        ocmSwitch.maxCyclesPerFlit(0);
        ocmSwitch.maxConcurrentFlits(1);
        ocmSwitch.addPorts("OCMSwitchPort1", "OCMSwitchPort2", "OCMSwitchPort3", "OCMSwitchPort4");
        ocmSwitch.getViewedVertex().putProperty("portWidthInBits", Map.of(
            "OCMSwitchPort1", 128,
            "OCMSwitchPort2", 128,
            "OCMSwitchPort3", 128,
            "OCMSwitchPort4", 128
        ));
        ocmSwitch.getViewedVertex().putProperty("portIsInitiator", Map.of(
            "OCMSwitchPort1", false,
            "OCMSwitchPort2", false,
            "OCMSwitchPort3", true,
            "OCMSwitchPort4", true
        ));
        ocmSwitch.getViewedVertex().putProperty("portProtocolAcronym", Map.of(
            "OCMSwitchPort1", "AXI4",
            "OCMSwitchPort2", "AXI4",
            "OCMSwitchPort3", "AXI4",
            "OCMSwitchPort4", "AXI4"
        ));
        ocmSwitch.getViewedVertex().putProperty("totalWeights", 7);
        ocmSwitch.getViewedVertex().putProperty("allocatedWeights", Map.of(
            "APU.C0", 1,
            "APU.C1", 1,
            "APU.C2", 1,
            "APU.C3", 1,
            "RPU.C0", 1,
            "RPU.C1", 1,
            "OCM", 1
        ));

        sGraph.connect(this.platform, ocmSwitch, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);
        Visualizable.enforce(ocmSwitch);
        GreyBox.enforce(ocmSwitch);
    }

    public void AddFPGA() {
        //TODO
    }
}
