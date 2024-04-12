
package models.platform_model;

import java.util.Map;

import forsyde.io.core.SystemGraph;
import models.platform_model.components.Platform;
import models.platform_model.components.Zynq;
import models.utils.Units;
import models.utils.Requirements;

public class PlatformHandler {
    final static String PLATFORM_NAME = "MPSoC";

    final static String RPU_NAME = "RPU";
    final static String RPU_SWITCH_NAME = "RPU_SWITCH";
    final static int RPU_CORES = 2;
    final static String APU_NAME = "APU";
    final static int APU_CORES = 4;
    final static String FPGA_NAME = "FPGA";
    final static String FPGA_SWITCH_NAME = "FPGA_SWITCH";
    final static String FPGA_BRAM_NAME = "FPGA_BRAM";

    final static String CCI_SWITCH_NAME = "CCI_SWITCH";
    final static String FPD_SWITCH_NAME = "FPD_SWITCH";
    final static String LPD_SWITCH_NAME = "LPD_SWITCH";

    final static String PL_DDR4_NAME = "PL_DDR4";
    final static String PL_DDR4_SWITCH_NAME = "PL_DDR4_SWITCH";
    final static String PL_SWITCH_NAME = "PL_SWITCH";
    final static String PS_DDR4_NAME = "PS_DDR4";
    final static String PS_DDR4_SWITCH_NAME = "PS_DDR4_SWITCH";
    final static String OCM_NAME = "OCM";
    final static String OCM_SWITCH_NAME = "OCM_SWITCH";
    final static String TCM_NAME = "TCM_RPU";

    public static SystemGraph MPSoCGraph() {
        Platform platform = new Platform(PLATFORM_NAME);

        // On-chip Memory
        platform.AddMemory(
            OCM_NAME,
            600 * Units.MHz,
            256 * Units.kB * Units.BYTES_TO_BITS
        );

        // Processing System DDR4 Memory
        platform.AddMemory(
            PS_DDR4_NAME,
            600 * Units.MHz,
            4 * Units.GB * Units.BYTES_TO_BITS
        );

        // Programmable Logic DDR4 Memory
        platform.AddMemory(
            PL_DDR4_NAME,
            600 * Units.MHz,
            4 * Units.GB * Units.BYTES_TO_BITS
        );

        // Processing System Application Processor Unit
        platform.AddCPU(
            APU_NAME,
            APU_CORES,
            (long) 1.5 * Units.GHz,
            Map.of(
                Requirements.SW_INSTRUCTIONS, Map.of(
                    Requirements.FLOP, 0.04
                )
            )
        );

        // Processing System Real-time Processor Unit
        platform.AddCPU(
            RPU_NAME,
            RPU_CORES,
            (long) 6 * Units.MHz,
            Map.of(
                Requirements.SW_INSTRUCTIONS, Map.of(
                    Requirements.FLOP, 0.4
                )
            )
        );

        // Processing System Real-time Processor Unit Switch
        platform.AddRouter(RPU_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(RPU_NAME, RPU_SWITCH_NAME);
        platform.ConnectToMemory(
            RPU_SWITCH_NAME, OCM_NAME, 600 * Units.MHz
        );
        platform.ConnectToMemory(
            RPU_SWITCH_NAME, PS_DDR4_NAME, 600 * Units.MHz
        );

        // Real-time Processor Unit Tightly Coupled Memory
        // ! UNSURE WHY THIS WON'T WORK
        // for (int i = 0; i < RPU_CORES; i++) {
        // String rpuName = RPU_NAME + "_C" + i;
        // String tcmName = TCM_NAME + "_C" + i;
        // String tcmSwitchName = TCM_NAME + "_C" + i + "_SWITCH";
        // platform.AddMemory(
        // tcmName,
        // 600 * Units.MHz,
        // 128 * Units.kB * Units.BYTES_TO_BITS
        // );
        // platform.AddSwitch(tcmSwitchName, 600 * Units.MHz);
        // platform.Connect(tcmName, tcmSwitchName);
        // platform.Connect(rpuName, tcmSwitchName);
        // }

        // Cache Coherent Interconnect (Switch)
        platform.AddRouter(CCI_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(APU_NAME, CCI_SWITCH_NAME);
        platform.ConnectToMemory(
            CCI_SWITCH_NAME, PS_DDR4_NAME, 600 * Units.MHz
        );

        // Full Power Domain Switch
        platform.AddRouter(FPD_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(FPD_SWITCH_NAME, CCI_SWITCH_NAME);
        platform.ConnectToMemory(
            FPD_SWITCH_NAME, OCM_NAME, 600 * Units.MHz
        );

        // Low Power Domain Switch
        platform.AddRouter(LPD_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(LPD_SWITCH_NAME, FPD_SWITCH_NAME);
        platform.Connect(LPD_SWITCH_NAME, RPU_SWITCH_NAME);

        // Programmable Logic Switch
        platform.AddRouter(PL_SWITCH_NAME, 600 * Units.MHz);
        platform.ConnectToMemory(
            PL_SWITCH_NAME, PL_DDR4_NAME, 600 * Units.MHz
        );
        platform.Connect(PL_SWITCH_NAME, FPD_SWITCH_NAME);
        platform.Connect(PL_SWITCH_NAME, LPD_SWITCH_NAME);
        platform.Connect(PL_SWITCH_NAME, CCI_SWITCH_NAME);

        // FPGA, FPGA Switch and BRAM memory
        // ! (PL switch connects the FPGA to the rest)
        platform.AddFPGA(FPGA_NAME, 600000 * Units.CLB);
        platform.AddRouter(FPGA_SWITCH_NAME, 600 * Units.MHz);
        platform.Connect(FPGA_NAME, PL_SWITCH_NAME);
        platform.Connect(FPGA_SWITCH_NAME, PL_SWITCH_NAME);
        // platform.AddMemory(
        //     FPGA_BRAM_NAME, 600 * Units.MHz, 4 * Units.MB * Units.BYTES_TO_BITS
        // );
        // platform.Connect(FPGA_SWITCH_NAME, FPGA_NAME);
        // platform.Connect(FPGA_SWITCH_NAME, FPGA_BRAM_NAME);

        // Information about the platform
        int numComponents = platform.viewers.size();
        System.out.println(numComponents + " components added to platform");

        return platform.GetGraph();
    }

    public static SystemGraph ZynqGraph() throws Exception {
        Zynq zynq = new Zynq();
        return zynq.sGraph;
    }
}
