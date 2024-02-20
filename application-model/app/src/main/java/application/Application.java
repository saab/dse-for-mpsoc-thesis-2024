
package application;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.ModelHandler;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.visual.kgt.drivers.KGTDriver;

import application.applicationrepo.*;

public class Application {

    private enum ApplicationType {
        ToySDF
    }

    private static final ApplicationType APPLICATION = ApplicationType.ToySDF;
    private static final String APP_DIR = System.getProperty("user.home") + 
        "/Documents/degree-project/dse-for-mpsoc-thesis-2024/application-model";
    private static final String ARTIFACTS_DST = 
        APP_DIR + "/app/src/main/java/application/artifacts";

    public static void main(String[] args) throws Exception {
        var sGraph = switch (APPLICATION) {
            default -> ToySDFGraph();
        };

        var handlerWithRegistrations = new ModelHandler()
                .registerTraitHierarchy(new ForSyDeHierarchy())
                .registerDriver(new KGTDriver());

        handlerWithRegistrations.writeModel(sGraph, 
            ARTIFACTS_DST + "/" + APPLICATION + ".fiodl"
        );
        handlerWithRegistrations.writeModel(sGraph, 
            ARTIFACTS_DST + "/" + APPLICATION + ".kgt"
        );
	}
    
        private static SystemGraph ToySDFGraph() {
            ToySDF toySDF = new ToySDF();
            SystemGraph sGraph = toySDF.build();
            return sGraph;
        }
}
