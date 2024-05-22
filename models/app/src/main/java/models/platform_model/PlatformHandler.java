
package models.platform_model;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import forsyde.io.core.SystemGraph;
import models.utils.Units;
import models.utils.Requirements;

public class PlatformHandler {
    final static String PLATFORM_NAME = "MPSoC";

    // Real-time Processor Unit
    final static String RPU_NAME = "RPU";
    final static String RPU_SWITCH_NAME = "RPU_SWITCH";
    final static String TCM_RPU_NAME = "TCM_RPU";
    final static int RPU_CORES = 2;

    // Application Processor Unit
    final static String APU_NAME = "APU";
    final static String APU_SWITCH_NAME = "APU_SWITCH";
    final static int APU_CORES = 4;
    
    // FPGA
    final static String FPGA_NAME = "FPGA";
    final static String FPGA_SWITCH_NAME = "FPGA_SWITCH";
    final static String FPGA_BRAM_NAME = "FPGA_BRAM";

    // Interconnect switches
    final static String CCI_SWITCH_NAME = "CCI_SWITCH";
    final static String FPD_SWITCH_NAME = "FPD_SWITCH";
    final static String LPD_SWITCH_NAME = "LPD_SWITCH";

    final static String PL_DDR4_NAME = "PL_DDR4";
    final static String PL_DDR4_SWITCH_NAME = "PL_DDR4_SWITCH";
    final static String PS_DDR4_NAME = "PS_DDR4";
    final static String PS_DDR4_SWITCH_NAME = "PS_DDR4_SWITCH";
    final static String OCM_NAME = "OCM";
    final static String OCM_SWITCH_NAME = "OCM_SWITCH";

    /**
     * Platform graph for the Xilinx UltraScale+ ZCU102 MPSoC.
     * @return SystemGraph representing the platform.
     */
    public static SystemGraph MPSoCGraph() {
        PlatformBuilder platform = new PlatformBuilder(PLATFORM_NAME);

        // On-chip Memory
        platform.AddMemory(
            OCM_NAME,
            600 * Units.MHz,
            256 * Units.kB * Units.BYTES_TO_BITS
        );
        platform.AddSwitch(OCM_SWITCH_NAME, 200 * Units.MHz, 64 * Units.BIT);
        platform.ConnectTwoWay(OCM_SWITCH_NAME, OCM_NAME);

        // Processing System DDR4 Memory
        platform.AddMemory(
            PS_DDR4_NAME,
            600 * Units.MHz,
            4 * Units.GB * Units.BYTES_TO_BITS
        );
        platform.AddSwitch(PS_DDR4_SWITCH_NAME, 333 * Units.MHz, 64 * Units.BIT);
        platform.ConnectTwoWay(PS_DDR4_SWITCH_NAME, PS_DDR4_NAME);

        // Programmable Logic DDR4 Memory
        platform.AddMemory(
            PL_DDR4_NAME,
            600 * Units.MHz,
            512 * Units.MB * Units.BYTES_TO_BITS
        );
        platform.AddSwitch(PL_DDR4_SWITCH_NAME, 333 * Units.MHz, 16 * Units.BIT);
        platform.ConnectTwoWay(PL_DDR4_NAME, PL_DDR4_SWITCH_NAME);

        // Processing System Application Processor Unit (APU)
        platform.AddCPU(
            APU_NAME,
            APU_CORES,
            1500 * Units.MHz,
            Map.of(
                Requirements.SW_INSTRUCTIONS,
                Map.of(
                    Requirements.FLOP, 2,
                    Requirements.INTOP, 1
                )
            )
        );
        platform.AddSwitch(APU_SWITCH_NAME, 200 * Units.MHz, 128 * Units.BIT);
        platform.ConnectTwoWay(APU_NAME, APU_SWITCH_NAME);

        // Processing System Real-time Processor Unit (RPU)
        platform.AddCPU(
            RPU_NAME,
            RPU_CORES,
            (long) 600 * Units.MHz,
            Map.of(
                Requirements.SW_INSTRUCTIONS,
                Map.of(
                    Requirements.FLOP, 2,
                    Requirements.INTOP, 1
                )
            )
        );
        platform.AddSwitch(RPU_SWITCH_NAME, 200 * Units.MHz, 64 * Units.BIT);
        platform.ConnectTwoWay(RPU_NAME, RPU_SWITCH_NAME);
        
        // Real-time Processor Unit's Tightly Coupled Memory (TCM)
        for (int i = 0; i < RPU_CORES; i++) {
            String tcmName = TCM_RPU_NAME + "_C" + i; //! hardcoding...
            platform.AddMemory(
                tcmName,
                600 * Units.MHz,
                128 * Units.kB * Units.BYTES_TO_BITS
                );
            var tcmSwName = tcmName + "_SWITCH";
            platform.AddSwitch(tcmSwName, 200 * Units.MHz, 64 * Units.BIT);
            platform.ConnectTwoWay(tcmSwName, tcmName);
            platform.ConnectTwoWay(tcmSwName, RPU_NAME + "_C" + i);
        }

        // FPGA (with embedded BRAM memory)
        platform.AddFPGA(
            FPGA_NAME, 
            600000 * Units.CLB, 
            4 * (int) Units.MB * Units.BYTES_TO_BITS, // Block RAM
            128 * (int) Units.BIT,
            200 * Units.MHz // "Clock region" frequency
        );
        

        // Interconnect switches
        platform.AddSwitch(CCI_SWITCH_NAME, 200 * Units.MHz, 128 * Units.BIT);
        platform.AddSwitch(FPD_SWITCH_NAME, 200 * Units.MHz, 128 * Units.BIT);
        platform.AddSwitch(LPD_SWITCH_NAME, 200 * Units.MHz, 128 * Units.BIT);

            
        // Connections now: Uni-directional or bi-directional
        // FPGA Connections
        platform.ConnectTwoWay(FPGA_NAME, FPD_SWITCH_NAME);
        platform.ConnectTwoWay(FPGA_NAME, LPD_SWITCH_NAME);
        platform.ConnectTwoWay(FPGA_NAME, PL_DDR4_SWITCH_NAME);
        platform.ConnectOneWay(FPGA_NAME, CCI_SWITCH_NAME);
        platform.ConnectOneWay(FPGA_NAME, APU_SWITCH_NAME);
        
        // CCI Switch Connections
        platform.ConnectTwoWay(CCI_SWITCH_NAME, APU_SWITCH_NAME);
        platform.ConnectTwoWay(CCI_SWITCH_NAME, FPD_SWITCH_NAME);
        platform.ConnectTwoWay(CCI_SWITCH_NAME, PS_DDR4_SWITCH_NAME);
        platform.ConnectOneWay(LPD_SWITCH_NAME, CCI_SWITCH_NAME);

        // Non categorizable switches
        platform.ConnectTwoWay(RPU_SWITCH_NAME, LPD_SWITCH_NAME);
        platform.ConnectTwoWay(RPU_SWITCH_NAME, OCM_SWITCH_NAME);
        platform.ConnectTwoWay(RPU_SWITCH_NAME, PS_DDR4_SWITCH_NAME);
        platform.ConnectTwoWay(FPD_SWITCH_NAME, LPD_SWITCH_NAME);
        platform.ConnectTwoWay(FPD_SWITCH_NAME, OCM_SWITCH_NAME);


        // Port connections for OCM Switch
        platform.AddInternalSwitchRoutes(OCM_SWITCH_NAME, 
            Map.of(
                RPU_SWITCH_NAME, new ArrayList<String>(
                    List.of(OCM_NAME)
                ),
                FPD_SWITCH_NAME, new ArrayList<String>(
                    List.of(OCM_NAME)
                )
            ));

        // Port connections for PL DDR4 Switch
        platform.AddInternalSwitchRoutes(PS_DDR4_SWITCH_NAME, 
            Map.of(
                CCI_SWITCH_NAME, new ArrayList<String>(
                    List.of(PS_DDR4_NAME)
                ),
                RPU_SWITCH_NAME, new ArrayList<String>(
                    List.of(PS_DDR4_NAME)
                )
            ));

        // Information about the platform
        int numComponents = platform.viewers.size();
        System.out.println(numComponents + " components added to platform");

        return platform.GetGraph();
    }

    // minimal platform for the MemoryMappableMultiCore decision model
    public static SystemGraph MMGraph() {
        var platform = new PlatformBuilder("Minimal_MM");

        platform.AddCPU(
            "CPU1", 
            1, 
            1 * Units.GHz, 
            Map.of(
                Requirements.SW_INSTRUCTIONS, 
                Map.of(
                    Requirements.INTOP, 1,
                    Requirements.FLOP, 4
                )
            )
        );

        platform.AddCPU(
            "CPU2", 
            1, 
            1 * Units.GHz, 
            Map.of(
                Requirements.SW_INSTRUCTIONS, 
                Map.of(
                    Requirements.INTOP, 1,
                    Requirements.FLOP, 2
                )
            )
        );

        platform.AddMemory(
            "Memory",
            600 * Units.MHz,
            4 * Units.GB * Units.BYTES_TO_BITS
        );
        platform.AddSwitch("MemorySwitch", APU_CORES, 64 * Units.BIT);
        
        platform.ConnectTwoWay("CPU1", "MemorySwitch");
        platform.ConnectTwoWay("CPU2", "MemorySwitch");

        return platform.GetGraph();
    }

    public static SystemGraph ZynqGraph() throws Exception {
        Zynq zynq = new Zynq();
        return zynq.sGraph;
    }
}
