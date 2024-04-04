
package models;

import forsyde.io.core.SystemGraph;

import models.application_model.*;
import models.platform_model.*;
import models.utils.FPGATransformer;
import models.utils.Paths;
import models.utils.Printer;

public class App {

    private enum ModelTargetType {
        Zynq, MPSoC, ToyPlatform, // platforms
        ToySDF, // applications
        DseResult // "UNKNOWN"
    }

    //TODO: extend to N platforms and applications (fpga_transform)
    private static String USAGE = """
        Usage: gradle run --args=\"[
            build |
            to_kgt <path> |
            fpga_transform <pathPlatform> <pathApplication> |
            parse_results <relativeMainPath/fullPath>
        ]\"""";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println(USAGE);
            System.exit(1);
        }

        String action = args[0];
        if (action.equals("build")) {
            //? set via CLI args?
            ModelTargetType Platform = ModelTargetType.MPSoC;
            ModelTargetType Application = ModelTargetType.ToySDF;
            CreateBuildSpecification(Platform, Application);
        } else {
            if (args.length < 2) {
                System.out.println(USAGE);
                System.exit(1);
            }
            String path1 = args[1];

            if (action.equals("to_kgt")) {
                ConvertFiodlToKGT(path1);
            } else if (action.equals("fpga_transform")) {
                if (args.length < 3) {
                    System.out.println(USAGE);
                    System.exit(1);
                }
                String path2 = args[2];
                FpgaTranform(path1, path2);
            } else {
                System.out.println(USAGE);
                System.exit(1);
            }
        }
    }

    private static void FpgaTranform(
        String platformPath, String applicationPath
    ) throws Exception {
        if (!platformPath.endsWith(Printer.FIODL_EXT) || 
                !applicationPath.endsWith(Printer.FIODL_EXT)) {
            throw new Exception("Must provide .fiodl files.");
        }
        SystemGraph gPlatform = Printer.Read(platformPath);
        SystemGraph gApplication = Printer.Read(applicationPath);
        boolean can_transform = FPGATransformer.ShouldTransform(gPlatform);
        if (!can_transform) {
            System.out.println(
                "No FPGA found, no platform transformation needed."
            );
            System.exit(0);
        }

        var graphs = FPGATransformer.Transform(gPlatform, gApplication);
        String fileDir = Printer.GetFileDir(platformPath); // same for app
        String platformFileName = Printer.GetFileName(platformPath);
        platformPath = fileDir + "/" + platformFileName + 
                        "_Transformed" + Printer.FIODL_EXT;
        Printer.Print(graphs.get("platform"), platformPath);
        
        String applicationFileName = Printer.GetFileName(applicationPath);
        applicationPath = fileDir + "/" + applicationFileName + 
                            "_Transformed" + Printer.FIODL_EXT;
        Printer.Print(graphs.get("application"), applicationPath);
    }

    private static void ConvertFiodlToKGT(String path) throws Exception {
        if (!path.endsWith(Printer.FIODL_EXT)) {
            throw new Exception("Must provide a .fiodl file.");
        }
        SystemGraph g = Printer.Read(path);
        String fileName = Printer.GetFileName(path);
        Printer.Print(g, Paths.ARTIFACTS_DIR + "/" + fileName + Printer.KGT_EXT);
    }

    private static void CreateBuildSpecification(
        ModelTargetType Platform, ModelTargetType Application
    ) throws Exception {
        SystemGraph gPlatform = switch (Platform) {
            case MPSoC -> PlatformHandler.MPSoCGraph();
            case Zynq -> PlatformHandler.ZynqGraph();
            default -> throw new IllegalStateException(
                "Unknown platform: " + Platform
            );
        };

        //? deprecate KGT print in favor of 'to_kgt'?
        String platformPath = Paths.ARTIFACTS_DIR + "/" + Platform;
        Printer.Print(gPlatform, platformPath + Printer.KGT_EXT); 
        Printer.Print(gPlatform, platformPath + Printer.FIODL_EXT);

        SystemGraph gApplication = switch (Application) {
            case ToySDF -> ApplicationHandler.ToySDFGraph();
            default -> throw new IllegalStateException(
                "Unknown application: " + Application
            );
        };
        //? deprecate KGT print in favor of 'to_kgt'?
        String applicationPath = Paths.ARTIFACTS_DIR + "/" + Application;
        Printer.Print(gApplication, applicationPath + Printer.KGT_EXT);
        Printer.Print(gApplication, applicationPath + Printer.FIODL_EXT);
    }
}
