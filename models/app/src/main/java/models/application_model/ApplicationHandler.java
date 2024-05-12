package models.application_model;

import forsyde.io.core.SystemGraph;
import models.utils.Requirements;
import models.utils.Units;

import java.util.*;


public class ApplicationHandler {
    // toy application
   

    public static SystemGraph ToySDFGraph() {
        final String TOY_SDF_APP_NAME = "ToySDF";
        final String ACTOR_A_NAME = "Actor_A";
        final String ACTOR_B_NAME = "Actor_B";

        var app = new ApplicationBuilder(TOY_SDF_APP_NAME);

        // ACTOR A
        app.AddActor(ACTOR_A_NAME);
        // app.AddSWImplementation(
        //     ACTOR_A_NAME, 
        //     Map.of(Requirements.FLOP, 1000L), 
        //     40000
        // );
        app.AddHWImplementation(
            ACTOR_A_NAME, 
            100 * Units.CLOCK_CYCLE, 
            6000 * Units.CLB
        );

        // ACTOR B
        app.AddActor(ACTOR_B_NAME);
        app.AddSWImplementation(
            ACTOR_B_NAME, 
            Map.of(Requirements.FLOP, 1000L), 
            32000
        );

        app.SetInputChannel(ACTOR_A_NAME, 1);
        app.CreateChannel(ACTOR_A_NAME, ACTOR_B_NAME, 1, 1);
        app.SetOutputChannel(ACTOR_B_NAME, 1);

        return app.GetGraph();
    }

    /**
     * Realistic embedded application graph.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph RealisticApplicationGraph() {
        final String REALISTIC_APP_NAME = "RealisticApp";
        final String GRAY = "Grayscale";
        final String SYNC_GRAY = "SyncGray";
        final String SOBEL = "Sobel";
        final String CNN_OBJ_DET = "Object Detection";

        
        var app = new ApplicationBuilder(REALISTIC_APP_NAME);
        var grays = new ArrayList<String>();
        
        for (int i = 0; i < 10; i++) {
            String name = GRAY + i;
            app.AddActor(name);
            app.AddSWImplementation(
                name, 
                Map.of(Requirements.FLOP, 8L),
                800
            );
            app.AddHWImplementation(
                name, 
                1 * Units.CLOCK_CYCLE,
                100 * Units.CLB
            );
            grays.add(name);
        }

        app.AddActor(SOBEL);
        app.AddSWImplementation(
            SOBEL, 
            Map.of(Requirements.INTOP, 18L),
            56300
        );
        app.AddHWImplementation(
            SOBEL,
            1 * Units.CLOCK_CYCLE,
            132 * Units.CLB
        );

        app.AddActor(CNN_OBJ_DET);
        app.AddSWImplementation(
            CNN_OBJ_DET, 
            Map.of(Requirements.FLOP, 21000000L),
            756000
        );
        app.AddHWImplementation(
            CNN_OBJ_DET,
            5600000 * Units.CLOCK_CYCLE,
            5650 * Units.CLB
        );

        app.AddActor(SYNC_GRAY);
        app.AddSWImplementation(
            SYNC_GRAY, 
            Map.of(Requirements.INTOP, 1L),
            3
        );

        grays.forEach(gray -> {
            app.SetInputChannel(gray, 1);
            app.CreateChannel(gray, SYNC_GRAY, 1, 3);
        });

        app.CreateChannel(SYNC_GRAY, SOBEL, 9, 9);
        app.CreateChannel(SOBEL, CNN_OBJ_DET, 1, 550*550);
        app.SetOutputChannel(CNN_OBJ_DET, 10);

        return app.GetGraph();
    }
}
