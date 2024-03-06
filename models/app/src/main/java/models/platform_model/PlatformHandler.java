
package models.platform_model;

import java.util.Map;

import forsyde.io.core.SystemGraph;
import models.platform_model.components.MPSoC;
import models.platform_model.components.Zynq;
import models.utils.Units;

public class PlatformHandler {

    public static SystemGraph MPSoCGraph() throws Exception {
        MPSoC platform = new MPSoC();

        platform.AddProcessingSystemModule(
                "APU",
                4,
                (long) 1.5 * Units.GHZ, // 1.5GHz
                Map.of(
                        "economy", Map.of(
                                "FloatOp", 0.04)));

        platform.AddProcessingSystemModule(
                "RPU",
                2,
                (long) 600 * Units.MHZ, // 600MHz
                Map.of(
                        "economy", Map.of(
                                "FloatOp", 0.43)));

        platform.AddMemoryToPS(600 * Units.MHZ, 4 * Units.GB * Units.BYTES_TO_BITS);


        return platform.sGraph;
    }

    public static SystemGraph ZynqGraph() throws Exception {
        Zynq zynq = new Zynq();
        return zynq.sGraph;
    }
}
