package models.application_model;

import forsyde.io.core.SystemGraph;
import models.application_model.applicationrepo.Application;

import models.utils.Requirements;

import java.util.*;


public class ApplicationHandler {
    final static String APP_NAME = "ToySDF";

    final static String ACTOR_A_NAME = "Actor_A";
    final static String ACTOR_B_NAME = "Actor_B";

    public static SystemGraph ToySDFGraph() {
        Application app = new Application(APP_NAME);

        app.AddActor(ACTOR_A_NAME);
        app.SetInput(ACTOR_A_NAME, 1);
        app.InstrumentSoftware(
            ACTOR_A_NAME, 
            Map.of(
                Requirements.FLOP, 1000L
            ), 
            40000
        );
        // app.InstrumentHardware(
        //     ACTOR_A_NAME,
        //     Map.of(
        //         Requirements.FLOP, 100L
        //     ), 
        //     Map.of(
        //         Requirements.LOGIC_AREA, 3200L
        //     )
        // );

        app.AddActor(ACTOR_B_NAME);
        app.SetOutput(ACTOR_B_NAME, 1);
        app.InstrumentSoftware(
            ACTOR_B_NAME, 
            Map.of(
                Requirements.FLOP, 1000L
            ), 
            32000
        );
        // app.InstrumentHardware(
        //     ACTOR_B_NAME,
        //     Map.of(
        //         Requirements.FLOP, 100L
        //     ), 
        //     Map.of(
        //         Requirements.LOGIC_AREA, 3200L
        //     )
        // );

        app.CreateChannel(ACTOR_A_NAME, ACTOR_B_NAME, 2, 3);

        return app.GetGraph();
    }
}
