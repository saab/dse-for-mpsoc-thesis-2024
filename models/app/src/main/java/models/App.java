
package models;

import forsyde.io.core.SystemGraph;

import models.application_model.*;
import models.platform_model.*;
import models.utils.Paths;
import models.utils.Printer;
import models.utils.SolutionParser;

/**
 * The entry point to the ForSyDe IO interfacing application.
 */
public class App {

    /**
     * Default printout showing application usage.
     */
    private static void SystemExit() {
        final String USAGE = """
            Usage: gradle run --args=\"[
                build <platformType> <applicationType> |
                to_kgt <path> |
                parse_solution <solutionPath> |
                build_bench_application <numActors> <numHwImpls>
            ]\"

            \033[4mbuild\033[0m - build system specifications:
            \t<platformType>: 'mpsoc', 'zynq', 'mm' 
            \t<applicationType>: 'tc1', tc2', 'tc3', 'tc45', 'real'
            \033[4mto_kgt\033[0m - convert fiodl to kgt (visualization format)
            \033[4mparse_solution\033[0m - extract concise information from a solution
            \t<solutionPath>: path to the solution file (fiodl)
            \033[4mbuild_bench_application\033[0m - create sequential SDF application
            \t<numActors>: number of actors
            \t<numHwImpls>: how many actors having hw and sw implementations
        """;
        System.out.println(USAGE);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            SystemExit();

        String action = args[0];
        System.out.println(action);
        if (action.equals("build")) {
            CreateBuildSpecification(args);
        } else if (action.equals("to_kgt")) {
            ConvertFiodlToKGT(args);
        } else if (action.equals("parse_solution")) {
            ParseDseSolution(args);
        } else if (action.equals("build_bench_application")) {
            CreateBenchApplication(args);
        } else {
            SystemExit();
        }
    }

    /**
     * Create a visualizable format (.kgt) from an input system 
     * specification (.fiodl).
     * @param args The path to the .fiodl file.
     * @throws Exception If the .fiodl file can't be read or .kgt file
     * can't be written.
     */
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
    
    /**
     * Create platform and application specifications based on command line
     * arguments.
     * @param args The platform and application type
     * @throws Exception If the file can't be written.
     */
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
            
        String platformPath = 
            Paths.ARTIFACTS_DIR + "/" + platformType + Printer.FIODL_EXT;
        Printer platformPrinter = new Printer(platformPath);
        platformPrinter.PrintFIODL(gPlatform);
            
        String applicationPath = 
            Paths.ARTIFACTS_DIR + "/" + applicationType + Printer.FIODL_EXT;
        Printer applicationPrinter = new Printer(applicationPath);
        applicationPrinter.PrintFIODL(gApplication);
    }

    /**
     * Create a benchmark sequential SDF benchmark application.
     * @param args The path to the solution .fiodl file
     * @throws Exception If the file can't be written.
     */
    private static void CreateBenchApplication(String[] args) throws Exception {
        if (args.length != 4)
            SystemExit();
        int actors = Integer.parseInt(args[1]);
        int hwImpls = Integer.parseInt(args[2]);
        String dir = args[3];

        String appName = "A" + actors + "_" + "HW" + hwImpls;
        SystemGraph g = ApplicationHandler.SequentialSDF(
            appName, actors, hwImpls
        );
        String outPath = dir + "/" + appName + Printer.FIODL_EXT;
        new Printer(outPath).PrintFIODL(g);
    }
        
    /**
     * Parses a DSE solution produced by IDeSyDe into a text file.
     * @param args The path to the solution .fiodl file
     * @throws Exception If the file can't be read.
     */
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
    