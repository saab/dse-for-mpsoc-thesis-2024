package platform.components;

import java.util.Map;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;

public class MPSoC {
    
    public SystemGraph sGraph;
    private StructureViewer platform = null;
    private StructureViewer APU = null;
    private StructureViewer RPU = null;
    
    public MPSoC() {
        SystemGraph sGraph = new SystemGraph();
        
        // whole platform
        this.platform = Structure.enforce(
            sGraph, sGraph.newVertex("MPSoC")
        );
        Visualizable.enforce(this.platform);
        
        this.sGraph = sGraph;
    }
        
    // Application Processing Unit (APU): 4 cores @ 1.5 GHz
    // - ET differences for instructions
    // Connected to platform structurally
    public void AddAPU() {
        final String APU_NAME = "MPSoC.APU";
        StructureViewer APU = Structure.enforce(
            sGraph, sGraph.newVertex(APU_NAME)
        );
        Visualizable.enforce(APU);
        sGraph.connect(platform, APU, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);

        int a53Cores = 4;
        for (int i = 0; i < a53Cores; i++) {
            var core = InstrumentedProcessingModule.enforce(
                sGraph, sGraph.newVertex(APU_NAME + ".C" + i)
            );
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(15 * 100000000L);
            core.modalInstructionsPerCycle(Map.of(
                "InstrType", Map.of(
                    "Flop", 2.0,    // arbitrary
                    "NonFlop", 1.0  // arbitrary
                )
            ));

            sGraph.connect(APU, core, EdgeTraits.StructuralContainment, EdgeTraits.VisualContainment);
            Visualizable.enforce(core);
        }

        this.APU = APU;
    }
    
    // Real-time Processing Unit (RPU): 2 cores @ 600 MHz
    // - ET differences for instructions
    // Connected to platform structurally
    public void AddRPU() {
        final String RPU_NAME = "MPSoC.RPU";
        StructureViewer RPU = Structure.enforce(
            sGraph, sGraph.newVertex(RPU_NAME)
        );
        Visualizable.enforce(RPU);
        sGraph.connect(platform, RPU, EdgeTraits.StructuralContainment, EdgeTraits.VisualConnection);

        int r5Cores = 2;
        for (int i = 0; i < r5Cores; i++) {
            var core = InstrumentedProcessingModule.enforce(
                sGraph, sGraph.newVertex(RPU_NAME + ".C" + i)
            );
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(6 * 100000000L);
            core.modalInstructionsPerCycle(Map.of(
                "InstrType", Map.of(
                    "Flop", 2.0,    // arbitrary
                    "NonFlop", 1.0  // arbitrary
                )
            ));

            sGraph.connect(RPU, core, EdgeTraits.StructuralContainment, EdgeTraits.VisualContainment);
            Visualizable.enforce(core);
        }

        this.RPU = RPU;
    }

    public void addFPGA() {
        //TODO
    }
}
