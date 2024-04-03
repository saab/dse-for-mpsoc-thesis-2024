
package models.platform_model;

import java.util.Map;

import forsyde.io.core.SystemGraph;
import models.platform_model.components.Platform;
import models.platform_model.components.Zynq;
import models.utils.Units;
import models.utils.Instructions;


public class PlatformHandler {

    public static SystemGraph MPSoCGraph() throws Exception {
        final String PLATFORM_NAME = "MPSoC";
        final String RPU_NAME = "RPU";
        final int RPU_CORES = 2;
        final String APU_NAME = "APU";
        final int APU_CORES = 4;
        final String OCM_NAME = "OCM";
        final String PS_DDR4_NAME = "PS_DDR4";
        final String TCM_NAME = "TCM_RPU";

        Platform platform = new Platform(PLATFORM_NAME);
        
        platform.AddMemory(
            OCM_NAME,
            600 * Units.MHz, 
            256 * Units.kB * Units.BYTES_TO_BITS
        );

        platform.AddMemory(
            PS_DDR4_NAME,
            600 * Units.MHz, 
            4 * Units.GB * Units.BYTES_TO_BITS
        );

        platform.AddCPU(
            APU_NAME,
            APU_CORES,
            (long) 1.5 * Units.GHz,
            Map.of(
                Instructions.SW_INSTRUCTIONS, Map.of(
                    Instructions.FLOP, 0.04
                )
            )
        );
        platform.ConnectPUsToMemory(APU_NAME, OCM_NAME);
        platform.ConnectPUsToMemory(APU_NAME, PS_DDR4_NAME);

        platform.AddCPU(
            RPU_NAME,
            RPU_CORES,
            (long) 6 * Units.MHz,
            Map.of(
                Instructions.SW_INSTRUCTIONS, Map.of(
                    Instructions.FLOP, 0.4
                )
            )
        );
        platform.ConnectPUsToMemory(RPU_NAME, OCM_NAME);
        platform.ConnectPUsToMemory(RPU_NAME, PS_DDR4_NAME);

        //! UNSURE WHY THIS WON'T WORK
        // for (int i = 0; i < RPU_CORES; i++) {
        //     String rpuName = RPU_NAME + "_C" + i;
        //     String tcmName = TCM_NAME + "_C" + i;
        //     platform.AddMemory(
        //         tcmName,
        //         600 * Units.MHz, 
        //         128 * Units.kB * Units.BYTES_TO_BITS
        //     );
        //     platform.ConnectPUsToMemory(rpuName, tcmName);
        // }

        return platform.GetGraph();
    }

    public static SystemGraph ZynqGraph() throws Exception {
        Zynq zynq = new Zynq();
        return zynq.sGraph;
    }
}
