
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
        ToySDF // applications
    }

    private static class Printer {
        private static ModelHandler handlerWithRegistrations = new ModelHandler()
                .registerTraitHierarchy(new ForSyDeHierarchy())
                .registerDriver(new KGTDriver());

        private static void print(SystemGraph g, PrintType type, ModelTargetType name) throws Exception {
            String ext = switch (type) {
                case FIODL -> ".fiodl";
                case KGT -> ".kgt";
            };

            String dest = Paths.ARTIFACTS_DIR + "/" + name + ext;
            handlerWithRegistrations.writeModel(g, dest);

            System.out.println("Model (" + type + ") written to '" + dest +"'");
        }
    }

    public static void main(String[] args) throws Exception {
        //TODO: set via CLI args
        ModelTargetType Platform = ModelTargetType.MPSoC;
        ModelTargetType Application = ModelTargetType.ToySDF;

        // Platform
        SystemGraph gPlatform = switch (Platform) {
            case MPSoC -> PlatformHandler.MPSoCGraph();
            case Zynq -> PlatformHandler.ZynqGraph();
            default -> throw new IllegalStateException("Unknown platform: " + Platform);
        };
        Printer.print(gPlatform, PrintType.FIODL, Platform);
        Printer.print(gPlatform, PrintType.KGT, Platform);

        // Application
        SystemGraph gApplication = switch (Application) {
            case ToySDF -> ApplicationHandler.ToySDFGraph();
            default -> throw new IllegalStateException("Unknown application: " + Application);
        };
        Printer.print(gApplication, PrintType.FIODL, Application);
        Printer.print(gApplication, PrintType.KGT, Application);

    }
}
