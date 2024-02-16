package platform.components;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;

public class Zynq {

    public SystemGraph sGraph;
    
    public Zynq() {
        SystemGraph sGraph = new SystemGraph();

        var zynqDualCoreArm = ForSyDeHierarchy.GenericProcessingModule.enforce(
            sGraph, sGraph.newVertex("ZYNQ_ARM_x2")
        );
        zynqDualCoreArm.maximumComputationParallelism(2);
        zynqDualCoreArm.operatingFrequencyInHertz(650L * 1000000L);
        ForSyDeHierarchy.Visualizable.enforce(zynqDualCoreArm); // VISUAL

        var private_dram = ForSyDeHierarchy.GenericMemoryModule.enforce(sGraph, sGraph.newVertex("DRAM"));
        private_dram.operatingFrequencyInHertz(100L * 1000000L);
        ForSyDeHierarchy.Visualizable.enforce(private_dram); // VISUAL

        // connect dual core to the memory
        sGraph.connect(zynqDualCoreArm, private_dram, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
        sGraph.connect(private_dram, zynqDualCoreArm, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);

        var ps7_0_axi_bus = ForSyDeHierarchy.GenericCommunicationModule.enforce(sGraph, sGraph.newVertex("ps7_0_axi"));
        ps7_0_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
        ForSyDeHierarchy.Visualizable.enforce(ps7_0_axi_bus);

        // the dual core and the first axi bus
        sGraph.connect(zynqDualCoreArm, ps7_0_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
        sGraph.connect(ps7_0_axi_bus, zynqDualCoreArm, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);

        var hp_video_axi_bus = ForSyDeHierarchy.GenericCommunicationModule.enforce(sGraph, sGraph.newVertex("hp_video_axi"));
        hp_video_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
        ForSyDeHierarchy.Visualizable.enforce(hp_video_axi_bus);

        // connect the busses
        sGraph.connect(ps7_0_axi_bus, hp_video_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
        sGraph.connect(hp_video_axi_bus, ps7_0_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);

        var bram_axi_bus = ForSyDeHierarchy.GenericCommunicationModule.enforce(sGraph, sGraph.newVertex("bram_axi"));
        bram_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
        ForSyDeHierarchy.Visualizable.enforce(bram_axi_bus);

        // connect the busses
        sGraph.connect(ps7_0_axi_bus, bram_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
        sGraph.connect(bram_axi_bus, ps7_0_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);

        var shared_bram = ForSyDeHierarchy.GenericMemoryModule.enforce(sGraph, sGraph.newVertex("Shared_BRAM"));
        shared_bram.operatingFrequencyInHertz(100L * 1000000L);
        ForSyDeHierarchy.Visualizable.enforce(shared_bram);

        // connect the busses
        sGraph.connect(shared_bram, bram_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
        sGraph.connect(bram_axi_bus, shared_bram, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);

        // make the acc
        var hp_video_ss = ForSyDeHierarchy.GenericProcessingModule.enforce(sGraph, sGraph.newVertex("hp_video_ss"));
        hp_video_ss.operatingFrequencyInHertz(142L * 1000000L);
        ForSyDeHierarchy.Visualizable.enforce(hp_video_ss);

        // connect to bus
        sGraph.connect(hp_video_ss, hp_video_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
        sGraph.connect(hp_video_axi_bus, hp_video_ss, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);

        // now the micro blazes
        for (int i = 0; i < 3; i++) {
            var ublazei_tile = ForSyDeHierarchy.GreyBox.enforce(sGraph, sGraph.newVertex("uBlaze" + i + "_tile"));
            var ublazei = ForSyDeHierarchy.GenericProcessingModule.enforce(sGraph, sGraph.newVertex("uBlaze" + i));
            ublazei.operatingFrequencyInHertz(100L * 1000000L);
            ublazei_tile.addContained(ForSyDeHierarchy.Visualizable.enforce(ublazei));


            var localmemi = ForSyDeHierarchy.GenericMemoryModule.enforce(sGraph, sGraph.newVertex("OCM" + i));
            localmemi.operatingFrequencyInHertz(100L * 1000000L);
            ublazei_tile.addContained(ForSyDeHierarchy.Visualizable.enforce(localmemi));

            // connect dual core to the memory
            sGraph.connect(ublazei, localmemi, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(localmemi, ublazei, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);

            var ublazi_axi_bus = ForSyDeHierarchy.GenericCommunicationModule.enforce(sGraph, sGraph.newVertex("uBlaze" + i + "_axi"));
            ublazi_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
            ublazei_tile.addContained(ForSyDeHierarchy.Visualizable.enforce(ublazi_axi_bus));

            // connect bus
            sGraph.connect(ublazei, ublazi_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, ublazei, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            // and to other buses
            sGraph.connect(bram_axi_bus, ublazi_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, bram_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            // now visual (no semantic meaning, just visuals)
            ublazei_tile.addPorts("buses");
            sGraph.connect(bram_axi_bus, ublazei_tile, null, "buses", ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(ublazei_tile, ublazi_axi_bus, "buses", ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, ublazei_tile, null, "buses", ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(ublazei_tile, bram_axi_bus, "buses", ForSyDeHierarchy.EdgeTraits.VisualConnection);

            // now add message box
            var mboxi = ForSyDeHierarchy.GenericMemoryModule.enforce(sGraph, sGraph.newVertex("MBox" + i));
            mboxi.operatingFrequencyInHertz(100L * 1000000L);
            ForSyDeHierarchy.Visualizable.enforce(mboxi);
            // final connections
            sGraph.connect(mboxi, ublazi_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, mboxi, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(mboxi, ps7_0_axi_bus, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
            sGraph.connect(ps7_0_axi_bus, mboxi, ForSyDeHierarchy.EdgeTraits.PhysicalConnection, ForSyDeHierarchy.EdgeTraits.VisualConnection);
        }

		this.sGraph = sGraph;
    }
}
