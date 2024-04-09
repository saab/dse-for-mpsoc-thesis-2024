package models.application_model;

import forsyde.io.core.SystemGraph;
import models.application_model.applicationrepo.Application;

public class ApplicationHandler {
    final static String APP_NAME = "ToySDF";

    final static String ACTOR_A_NAME = "Actor_A";
    final static String ACTOR_B_NAME = "Actor_B";

    public static SystemGraph ToySDFGraph() {
        Application app = new Application(APP_NAME);
        SystemGraph sGraph = app.AddActor();
        return sGraph;
    }

}
