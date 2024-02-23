
package models.platform_model.components;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;

public class Zynq {

    public SystemGraph sGraph;

    public Zynq() {
        SystemGraph sGraph = new SystemGraph();

        var zynqDualCoreArm = GenericProcessingModule.enforce(
                sGraph, sGraph.newVertex("ZYNQ_ARM_x2"));
        zynqDualCoreArm.maximumComputationParallelism(2);
        zynqDualCoreArm.operatingFrequencyInHertz(650L * 1000000L);
        Visualizable.enforce(zynqDualCoreArm); // VISUAL

        var private_dram = GenericMemoryModule.enforce(sGraph, sGraph.newVertex("DRAM"));
        private_dram.operatingFrequencyInHertz(100L * 1000000L);
        Visualizable.enforce(private_dram); // VISUAL

        // connect dual core to the memory
        sGraph.connect(zynqDualCoreArm, private_dram, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);
        sGraph.connect(private_dram, zynqDualCoreArm, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);

        var ps7_0_axi_bus = GenericCommunicationModule.enforce(sGraph, sGraph.newVertex("ps7_0_axi"));
        ps7_0_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
        Visualizable.enforce(ps7_0_axi_bus);

        // the dual core and the first axi bus
        sGraph.connect(zynqDualCoreArm, ps7_0_axi_bus, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);
        sGraph.connect(ps7_0_axi_bus, zynqDualCoreArm, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);

        var hp_video_axi_bus = GenericCommunicationModule.enforce(sGraph,
                sGraph.newVertex("hp_video_axi"));
        hp_video_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
        Visualizable.enforce(hp_video_axi_bus);

        // connect the busses
        sGraph.connect(ps7_0_axi_bus, hp_video_axi_bus, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);
        sGraph.connect(hp_video_axi_bus, ps7_0_axi_bus, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);

        var bram_axi_bus = GenericCommunicationModule.enforce(sGraph, sGraph.newVertex("bram_axi"));
        bram_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
        Visualizable.enforce(bram_axi_bus);

        // connect the busses
        sGraph.connect(ps7_0_axi_bus, bram_axi_bus, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);
        sGraph.connect(bram_axi_bus, ps7_0_axi_bus, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);

        var shared_bram = GenericMemoryModule.enforce(sGraph, sGraph.newVertex("Shared_BRAM"));
        shared_bram.operatingFrequencyInHertz(100L * 1000000L);
        Visualizable.enforce(shared_bram);

        // connect the busses
        sGraph.connect(shared_bram, bram_axi_bus, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);
        sGraph.connect(bram_axi_bus, shared_bram, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);

        // make the acc
        var hp_video_ss = GenericProcessingModule.enforce(sGraph, sGraph.newVertex("hp_video_ss"));
        hp_video_ss.operatingFrequencyInHertz(142L * 1000000L);
        Visualizable.enforce(hp_video_ss);

        // connect to bus
        sGraph.connect(hp_video_ss, hp_video_axi_bus, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);
        sGraph.connect(hp_video_axi_bus, hp_video_ss, EdgeTraits.PhysicalConnection,
                EdgeTraits.VisualConnection);

        // now the micro blazes
        for (int i = 0; i < 3; i++) {
            var ublazei_tile = GreyBox.enforce(sGraph, sGraph.newVertex("uBlaze" + i + "_tile"));
            var ublazei = GenericProcessingModule.enforce(sGraph, sGraph.newVertex("uBlaze" + i));
            ublazei.operatingFrequencyInHertz(100L * 1000000L);
            ublazei_tile.addContained(Visualizable.enforce(ublazei));

            var localmemi = GenericMemoryModule.enforce(sGraph, sGraph.newVertex("OCM" + i));
            localmemi.operatingFrequencyInHertz(100L * 1000000L);
            ublazei_tile.addContained(Visualizable.enforce(localmemi));

            // connect dual core to the memory
            sGraph.connect(ublazei, localmemi, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(localmemi, ublazei, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);

            var ublazi_axi_bus = GenericCommunicationModule.enforce(sGraph,
                    sGraph.newVertex("uBlaze" + i + "_axi"));
            ublazi_axi_bus.operatingFrequencyInHertz(100L * 1000000L);
            ublazei_tile.addContained(Visualizable.enforce(ublazi_axi_bus));

            // connect bus
            sGraph.connect(ublazei, ublazi_axi_bus, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, ublazei, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            // and to other buses
            sGraph.connect(bram_axi_bus, ublazi_axi_bus, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, bram_axi_bus, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            // now visual (no semantic meaning, just visuals)
            ublazei_tile.addPorts("buses");
            sGraph.connect(bram_axi_bus, ublazei_tile, null, "buses", EdgeTraits.VisualConnection);
            sGraph.connect(ublazei_tile, ublazi_axi_bus, "buses", EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, ublazei_tile, null, "buses", EdgeTraits.VisualConnection);
            sGraph.connect(ublazei_tile, bram_axi_bus, "buses", EdgeTraits.VisualConnection);

            // now add message box
            var mboxi = GenericMemoryModule.enforce(sGraph, sGraph.newVertex("MBox" + i));
            mboxi.operatingFrequencyInHertz(100L * 1000000L);
            Visualizable.enforce(mboxi);
            // final connections
            sGraph.connect(mboxi, ublazi_axi_bus, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(ublazi_axi_bus, mboxi, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(mboxi, ps7_0_axi_bus, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(ps7_0_axi_bus, mboxi, EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
        }

        this.sGraph = sGraph;
    }
}
