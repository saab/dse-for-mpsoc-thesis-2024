
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
        final String RPU_SWITCH_NAME = "RPU_SWITCH";
        final int RPU_CORES = 2;
        final String APU_NAME = "APU";
        final int APU_CORES = 4;
        final String OCM_NAME = "OCM";
        final String OCM_SWITCH_NAME = "OCM_SWITCH";
        final String PS_DDR4_NAME = "PS_DDR4";
        final String PS_DDR4_SWITCH_NAME = "PS_DDR4_SWITCH";
        final String TCM_NAME = "TCM_RPU";

        Platform platform = new Platform(PLATFORM_NAME);
        
        // On-chip Memory
        platform.AddMemory(
            OCM_NAME,
            600 * Units.MHz, 
            256 * Units.kB * Units.BYTES_TO_BITS
        );
        platform.AddSwitch(OCM_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(OCM_NAME, OCM_SWITCH_NAME);

        // Processing System DDR4 Memory
        platform.AddMemory(
            PS_DDR4_NAME,
            600 * Units.MHz, 
            4 * Units.GB * Units.BYTES_TO_BITS
        );
        platform.AddSwitch(PS_DDR4_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(PS_DDR4_NAME, PS_DDR4_SWITCH_NAME);

        // Processing System Application Processor Unit
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
        platform.Connect(APU_NAME, OCM_SWITCH_NAME);
        platform.Connect(APU_NAME, PS_DDR4_SWITCH_NAME);

        // Processing System Real-time Processor Unit
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
        platform.AddSwitch(RPU_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(RPU_NAME, RPU_SWITCH_NAME);
        platform.Connect(RPU_SWITCH_NAME, OCM_SWITCH_NAME);
        platform.Connect(RPU_SWITCH_NAME, PS_DDR4_SWITCH_NAME);

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
