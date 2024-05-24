
package models;

import forsyde.io.core.SystemGraph;

import models.application_model.*;
import models.platform_model.*;
// import models.utils.FPGATransformer;
import models.utils.Paths;
import models.utils.Printer;
import models.utils.SolutionParser;


public class App {

    private static void SystemExit() {
        final String USAGE = """
            Usage: gradle run --args=\"[
                build <platformType> <applicationType> |
                to_kgt <path> |
                fpga_transform <pathPlatform> <pathApplication> |
                parse_dse_results <path>
            ]\"
            \t<platformType>: 'MPSoC', 
            \t<applicationType>: 'ToySDF'
        """;
        System.out.println(USAGE);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            SystemExit();

        String action = args[0];
        if (action.equals("build")) {
            CreateBuildSpecification(args);
        } else if (action.equals("to_kgt")) {
            ConvertFiodlToKGT(args);
        } else if (action.equals("parse_solution")) {
            ParseDseSolution(args);
        } else {
            SystemExit();
        }
    }

    // @Deprecated
    // private static void FpgaTranform(String[] args) throws Exception {
    //     if (args.length < 3) 
    //         SystemExit();
        
    //     String platformPath = args[1];
    //     String applicationPath = args[2];
    //     assert platformPath.endsWith(Printer.FIODL_EXT): "Must provide a .fiodl file.";
    //     assert applicationPath.endsWith(Printer.FIODL_EXT): "Must provide a .fiodl file.";

    //     SystemGraph gPlatform = new Printer(platformPath).Read();
    //     SystemGraph gApplication = new Printer(applicationPath).Read();
        
    //     Map<String, SystemGraph> transformedGraphs = Map.of(
    //         "platform", gPlatform, 
    //         "application", gApplication
    //     );

    //     var transformer = new FPGATransformer(gPlatform, gApplication);
    //     if (transformer.ShouldTransform()) {
    //         transformedGraphs = transformer.Transform();
    //     } else {
    //         System.out.println(
    //             "Both FPGAs and HW actors must exist, no transformation needed."
    //         );
    //     }

    //     var printer = new Printer(platformPath);
    //     printer.AppendToFileName("_Intermediate");
    //     printer.PrintFIODL(transformedGraphs.get("platform"));
        
    //     printer = new Printer(applicationPath);
    //     printer.AppendToFileName("_Intermediate");
    //     printer.PrintFIODL(transformedGraphs.get("application"));
    // }

    private static void ConvertFiodlToKGT(String[] args) throws Exception {
        if (args.length < 2)
            SystemExit();

        String path = args[1];
        assert path.endsWith(Printer.FIODL_EXT): "Must provide a .fiodl file.";
        System.out.println("Converting " + path + " to KGT format.");

        Printer printer = new Printer(path);
        SystemGraph g = printer.Read();
        printer.PrintKGT(g);
    }
    
    private static void CreateBuildSpecification(String[] args) throws Exception {
        if (args.length < 3)
            SystemExit();
        
        String platformType = args[1];
        SystemGraph gPlatform = switch (platformType.toLowerCase()) {
            case "mpsoc" -> PlatformHandler.MPSoCGraph();
            case "zynq" -> PlatformHandler.ZynqGraph();
            case "mm" -> PlatformHandler.MMGraph();
            default -> throw new IllegalStateException(
                "Unknown platform: " + platformType + " (mpsoc, zynq, mm)"
            );
        };
            
        String platformPath = 
            Paths.ARTIFACTS_DIR + "/" + platformType + Printer.FIODL_EXT;
        Printer platformPrinter = new Printer(platformPath);
        platformPrinter.PrintFIODL(gPlatform);
        
        String applicationType = args[2];
        SystemGraph gApplication = switch (applicationType.toLowerCase()) {
            case "tc1" -> ApplicationHandler.TC1();
            case "tc2" -> ApplicationHandler.TC2();
            case "tc3" -> ApplicationHandler.TC3();
            case "tc45" -> ApplicationHandler.TC4And5();
            case "real" -> ApplicationHandler.Realistic();
            default -> throw new IllegalStateException(
                "Unknown application: " + applicationType + 
                " (tc1, tc2, tc3, tc45, real)"
            );
        };
            
        String applicationPath = 
            Paths.ARTIFACTS_DIR + "/" + applicationType + Printer.FIODL_EXT;
        Printer applicationPrinter = new Printer(applicationPath);
        applicationPrinter.PrintFIODL(gApplication);
    }
        
    private static void ParseDseSolution(String[] args) throws Exception {
        if (args.length < 2)
            SystemExit();
        
        String path = args[1];
        assert path.endsWith(Printer.FIODL_EXT): "Must provide a .fiodl file.";

        var parser = new SolutionParser(new Printer(path).Read());
        parser.ParseSolution();
        parser.PrintSolution();
        parser.WriteSolution(
            path.substring(0, path.lastIndexOf('.'))
            + ".txt"
        );
    }
}
    