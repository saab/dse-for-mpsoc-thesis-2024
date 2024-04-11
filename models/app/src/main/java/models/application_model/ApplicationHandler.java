package models.application_model;

import forsyde.io.core.SystemGraph;
import models.application_model.applicationrepo.Application;

import models.utils.Requirements;

import java.util.*;


public class ApplicationHandler {
    final static String APP_NAME = "ToySDF";

    final static String ACTOR_A_NAME = "Actor_A";
    final static String ACTOR_B_NAME = "Actor_B";
    final static String ACTOR_C_NAME = "Actor_C";
    final static String ACTOR_D_NAME = "Actor_D";

    public static SystemGraph ToySDFGraph() {
        Application app = new Application(APP_NAME);

        // ACTOR A
        app.AddActor(ACTOR_A_NAME);
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

        // ACTOR B
        app.AddActor(ACTOR_B_NAME);
        app.InstrumentSoftware(
            ACTOR_B_NAME, 
            Map.of(
                Requirements.FLOP, 1000L
            ), 
            32000
        );

        // ACTOR C
        app.AddActor(ACTOR_C_NAME);
        app.InstrumentSoftware(
            ACTOR_C_NAME, 
            Map.of(
                Requirements.FLOP, 150L
            ), 
            13400
        );
          
        // ACTOR D
        app.AddActor(ACTOR_D_NAME);
        app.InstrumentSoftware(
            ACTOR_D_NAME, 
            Map.of(
                Requirements.FLOP, 130L
            ), 
            13400
        );

        app.SetInputChannel(ACTOR_A_NAME, 1);
        app.CreateChannel(ACTOR_A_NAME, ACTOR_B_NAME, 3, 3);
        app.CreateChannel(ACTOR_B_NAME, ACTOR_C_NAME, 3, 6);
        app.CreateChannel(ACTOR_C_NAME, ACTOR_D_NAME, 3, 2);
        app.SetOutputChannel(ACTOR_C_NAME, 1);

        return app.GetGraph();
    }
}
