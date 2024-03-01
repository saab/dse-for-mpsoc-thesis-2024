
package models;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.ModelHandler;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.visual.kgt.drivers.KGTDriver;
import models.application_model.*;
import models.platform_model.*;
import models.utils.Paths;

public class App {

    private static final String FIODL_EXT = ".fiodl";
    private static final String KGT_EXT = ".kgt";

    private enum ModelTargetType {
        Zynq, MPSoC, ToyPlatform, // platforms
        ToySDF, // applications
        DseResult // "UNKNOWN"
    }

    private static class Printer {
        private static ModelHandler handler = new ModelHandler()
                .registerTraitHierarchy(new ForSyDeHierarchy())
                .registerDriver(new KGTDriver());

        private static void Print(SystemGraph g, String outPath) throws Exception {
            handler.writeModel(g, outPath);

            // extract file name from outPath without extension
            String description = outPath.substring(outPath.lastIndexOf('/') + 1, outPath.lastIndexOf('.'));

            // extract extension from outPath
            String extension = outPath.substring(outPath.lastIndexOf('.'));

            if (extension.equals(FIODL_EXT)) {
                System.out.println("Design model of '" + description + "' written to '" + outPath);
            } else if (extension.equals(KGT_EXT)) {
                System.out.println("Visualization of '" + description + "' model written to '" + outPath);
            } else {
                System.out.println("Unknown file extension: " + extension);
            }
        }

        private static SystemGraph Read(String path) throws Exception {
            var model = handler.loadModel(path);
            return model;
        }
    }

    private static String USAGE = "\nUsage: gradle run --args=\"[build|to_kgt <relativeMainPath/fullPath>]";

    public static void main(String[] args) throws Exception {
        // ? set via CLI args?
        ModelTargetType Platform = ModelTargetType.MPSoC;
        ModelTargetType Application = ModelTargetType.ToySDF;

        if (args.length < 1) {
            System.out.println();
            return;
        }

        String action = args[0];
        if (action.equals("build")) {
            CreateBuildSpecification(Platform, Application);
        } else if (action.equals("to_kgt")) {
            if (args.length < 2) {
                System.out.println(USAGE);
                return;
            }
            String path = args[1];
            ConvertFiodlToKGT(path);
        } else {
            System.out.println(USAGE);
        }
    }

    private static void ConvertFiodlToKGT(String fullPath) throws Exception {
        SystemGraph g = Printer.Read(fullPath);
        Printer.Print(g, Paths.ARTIFACTS_DIR + "/" + ModelTargetType.DseResult + KGT_EXT);
    }

    private static void CreateBuildSpecification(ModelTargetType Platform, ModelTargetType Application)
            throws Exception {
        // Platform
        SystemGraph gPlatform = switch (Platform) {
            case MPSoC -> PlatformHandler.MPSoCGraph();
            case Zynq -> PlatformHandler.ZynqGraph();
            default -> throw new IllegalStateException("Unknown platform: " + Platform);
        };
        Printer.Print(gPlatform, Paths.ARTIFACTS_DIR + "/" + Platform + KGT_EXT);
        Printer.Print(gPlatform, Paths.ARTIFACTS_DIR + "/" + Platform + FIODL_EXT);

        // Application
        SystemGraph gApplication = switch (Application) {
            case ToySDF -> ApplicationHandler.ToySDFGraph();
            default -> throw new IllegalStateException("Unknown application: " + Application);
        };
        Printer.Print(gApplication, Paths.ARTIFACTS_DIR + "/" + Application + KGT_EXT);
        Printer.Print(gApplication, Paths.ARTIFACTS_DIR + "/" + Application + FIODL_EXT);
    }
}
