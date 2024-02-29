
package models;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.ModelHandler;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.visual.kgt.drivers.KGTDriver;
import models.application_model.*;
import models.platform_model.*;
import models.utils.Paths;

public class App {

    private enum PrintType {
        FIODL, KGT
    }

    private enum ModelTargetType {
        Zynq, MPSoC, ToyPlatform, // platforms
        ToySDF, // applications
        DseResult // "UNKNOWN"
    }

    private static class Printer {
        private static ModelHandler handler = new ModelHandler()
                .registerTraitHierarchy(new ForSyDeHierarchy())
                .registerDriver(new KGTDriver());

        private static String ToExt(PrintType t) {
            return switch (t) {
                case FIODL -> ".fiodl";
                case KGT -> ".kgt";
            };
        }

        private static void Print(SystemGraph g, PrintType type, ModelTargetType name) throws Exception {
            String ext = ToExt(type);

            String dest = Paths.ARTIFACTS_DIR + "/" + name + ext;
            handler.writeModel(g, dest);

            System.out.println("Model (" + type + ") written to '" + dest + "'");
        }

        private static SystemGraph Read(String name) throws Exception {
            var model = handler.loadModel(Paths.ARTIFACTS_DIR + "/" + name);
            return model;
        }
    }

    private static String USAGE = "\nUsage: gradle run --args=\"[build|to_kgt <path>]";

    public static void main(String[] args) throws Exception {
        //TODO: set via CLI args
        ModelTargetType Platform = ModelTargetType.MPSoC;
        ModelTargetType Application = ModelTargetType.ToySDF;

        if (args.length < 1) {
            System.out.println();
            return;
        }
        
        String action = args[0];
        if (action.equals("build")) {
            BuildSpecification(Platform, Application);
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

    private static void ConvertFiodlToKGT(String path) throws Exception {
        SystemGraph g = Printer.Read(path);
        Printer.Print(g, PrintType.KGT, ModelTargetType.DseResult);
    }

    private static void BuildSpecification(ModelTargetType Platform, ModelTargetType Application) throws Exception {
        // Platform
        SystemGraph gPlatform = switch (Platform) {
            case MPSoC -> PlatformHandler.MPSoCGraph();
            case Zynq -> PlatformHandler.ZynqGraph();
            default -> throw new IllegalStateException("Unknown platform: " + Platform);
        };
        Printer.Print(gPlatform, PrintType.FIODL, Platform);
        Printer.Print(gPlatform, PrintType.KGT, Platform);

        // Application
        SystemGraph gApplication = switch (Application) {
            case ToySDF -> ApplicationHandler.ToySDFGraph();
            default -> throw new IllegalStateException("Unknown application: " + Application);
        };
        Printer.Print(gApplication, PrintType.FIODL, Application);
        Printer.Print(gApplication, PrintType.KGT, Application);
    }
}
