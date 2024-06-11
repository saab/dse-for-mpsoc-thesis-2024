package models.application_model;

import forsyde.io.core.SystemGraph;
import models.utils.Requirements;
import models.utils.Units;

import java.util.*;


public class ApplicationHandler {


    /**
     * Test case 1 from the thesis.
     * Goal: Make sure that applications can map both to hardware and software.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph TC1() {
        final String APP_NAME = "TC1";
        final String ACTOR_1 = "Actor_1";
        final String ACTOR_2 = "Actor_2";
        
        var app = new ApplicationBuilder(APP_NAME);
        
        app.AddActor(ACTOR_1);
        app.AddHWImplementation(
            ACTOR_1, 
            10 * Units.CLOCK_CYCLE, 
            300 * Units.MHz,
            2 * Units.kB * Units.BYTES_TO_BITS,
            110 * Units.CLB
        );
        
        app.AddActor(ACTOR_2);
        app.AddSWImplementation(
            ACTOR_2,
            Map.of(Requirements.FLOP, 8L), 
            4 * Units.kB * Units.BYTES_TO_BITS
        );
  
        app.SetInputChannel(ACTOR_1, 5);
        app.CreateChannel(ACTOR_1, ACTOR_2, 15, 3);
        app.SetOutputChannel(ACTOR_2, 1);
        
        return app.GetGraph();
    }

    /**
     * Test case 2 from the thesis.
     * Goal: Make sure that applications map to hardware based on favorable 
     * hardware implementations.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph TC2() {
        final String APP_NAME = "TC2";
        final String ACTOR_1 = "Actor_1";
        final String ACTOR_2 = "Actor_2";
        
        var app = new ApplicationBuilder(APP_NAME);

        app.AddActor(ACTOR_1);
        app.AddHWImplementation(
            ACTOR_1, 
            10 * Units.CLOCK_CYCLE, 
            200 * Units.MHz,
            2 * Units.kB * Units.BYTES_TO_BITS,
            110 * Units.CLB
        );
        app.AddSWImplementation(
            ACTOR_1,
            Map.of(Requirements.FLOP, 9000L), 
            4 * Units.kB * Units.BYTES_TO_BITS
        );

        app.AddActor(ACTOR_2);
        app.AddHWImplementation(
            ACTOR_2,
            10 * Units.CLOCK_CYCLE,
            200 * Units.MHz,
            2 * Units.kB * Units.BYTES_TO_BITS,
            90 * Units.CLB
        );
        app.AddSWImplementation(
            ACTOR_2,
            Map.of(Requirements.FLOP, 8000L), 
            4 * Units.kB * Units.BYTES_TO_BITS
        );
                
        app.SetInputChannel(ACTOR_1, 5);
        app.CreateChannel(ACTOR_1, ACTOR_2, 15, 3);
        app.SetOutputChannel(ACTOR_2, 1);
        
        return app.GetGraph();
    }
    
    /**
     * Test case 3 from the thesis.
     * Goal: Make sure that the FPGA's resource constraints are respected by
     * specifying more BRAM and CLBs than available.
     * @return
     */
    public static SystemGraph TC3() {
        final String APP_NAME = "TC3";
        final String ACTOR_1 = "Actor_1";
        final String ACTOR_2 = "Actor_2";
        
        var app = new ApplicationBuilder(APP_NAME);
        
        app.AddActor(ACTOR_1);
        app.AddHWImplementation(
            ACTOR_1, 
            10 * Units.CLOCK_CYCLE, 
            200 * Units.MHz,
            1 * Units.GB * Units.BYTES_TO_BITS,
            10 * Units.CLB
        );
        
        app.AddActor(ACTOR_2);
        app.AddHWImplementation(
            ACTOR_2, 
            15 * Units.CLOCK_CYCLE, 
            200 * Units.MHz,
            2 * Units.kB * Units.BYTES_TO_BITS,
            600100 * Units.CLB
        );
        
        app.SetInputChannel(ACTOR_1, 5);
        app.CreateChannel(ACTOR_1, ACTOR_2, 15, 3);
        app.SetOutputChannel(ACTOR_2, 1);
        
        return app.GetGraph();
    }
        
    /**
     * Test case 4 and 5 from the thesis.
     * Goal: Make sure that communication bandwidths throughout the platform
     * are respected. Thus specifying one favorable hardware implementation and
     * one favorable hardware implementation for each actor, the communication
     * bandwidth between the PL and PS side.
     * @return
     */
    public static SystemGraph TC4And5() {
        final String APP_NAME = "TC4And5";
        final String ACTOR_1 = "Actor_1";
        final String ACTOR_2 = "Actor_2";

        var app = new ApplicationBuilder(APP_NAME);

        app.AddActor(ACTOR_1);
        app.AddSWImplementation(
            ACTOR_1, 
            Map.of(Requirements.FLOP, 1L), 
            4 * Units.kB * Units.BYTES_TO_BITS
        );
        app.AddHWImplementation(
            ACTOR_1, 
            200000000 * Units.CLOCK_CYCLE, 
            200 * Units.MHz,
            2 * Units.MB * Units.BYTES_TO_BITS,
            110 * Units.CLB
        );

        app.AddActor(ACTOR_2);
        app.AddSWImplementation(
            ACTOR_2, 
            Map.of(Requirements.FLOP, 8000000L), 
            (long) 1.8 * Units.kB * Units.BYTES_TO_BITS
        );
        app.AddHWImplementation(
            ACTOR_2, 
            15 * Units.CLOCK_CYCLE, 
            200 * Units.MHz,
            2 * Units.kB * Units.BYTES_TO_BITS,
            90 * Units.CLB
        );

        app.SetInputChannel(ACTOR_1, 5);
        app.CreateChannel(ACTOR_1, ACTOR_2, 15, 3);
        app.SetOutputChannel(ACTOR_2, 1);

        return app.GetGraph();
    }

    /**
     * Realistic embedded application graph.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph Realistic() {
        final String APP_NAME = "RealisticSDF";
        final String SPLIT_INPUT_ACTOR = "SplitInput";
        final String SYNC_AND_RESIZE_ACTOR = "SyncAndResize";
        final String GRAY = "Grayscale";
        final String SOBEL = "Sobel";
        final String CNN_OBJ_DET = "ObjectDetection";
        final int FRAME_SIZE = 1; //3840*2160;
        final int RGB_FRAME_SIZE = FRAME_SIZE*3;

        
        var app = new ApplicationBuilder(APP_NAME);
        
        // add split input actor
        app.AddActor(SPLIT_INPUT_ACTOR);
        app.AddSWImplementation(
            SPLIT_INPUT_ACTOR, 
            Map.of(Requirements.INTOP, 1L),
            3 * Units.BYTES_TO_BITS
        );
        app.AddHWImplementation(
            SPLIT_INPUT_ACTOR, 
            200 * Units.CLOCK_CYCLE,
            300 * Units.MHz,
            8,
            100 * Units.CLB
        );
        app.SetInputChannel(SPLIT_INPUT_ACTOR, RGB_FRAME_SIZE);

        app.AddActor(SYNC_AND_RESIZE_ACTOR);
        app.AddSWImplementation(
            SYNC_AND_RESIZE_ACTOR, 
            Map.of(Requirements.INTOP, 1000L),
            3 * Units.BYTES_TO_BITS
        );

        int par_grays = 5;
        for (int i = 0; i < par_grays; i++) {
            String grayName = GRAY + i;
            app.AddActor(grayName);
            app.AddSWImplementation(
                grayName, 
                Map.of(Requirements.FLOP, 8L * FRAME_SIZE / par_grays),
                100 * Units.BYTES_TO_BITS
            );
            app.AddHWImplementation(
                grayName, 
                2070000 * Units.CLOCK_CYCLE / par_grays,
                300 * Units.MHz,
                0,
                100 * Units.CLB
            );
            app.CreateChannel(SPLIT_INPUT_ACTOR, grayName, RGB_FRAME_SIZE / par_grays, RGB_FRAME_SIZE / par_grays);  
            
            String sobelName = SOBEL + i;
            app.AddActor(sobelName);
            app.AddSWImplementation(
                sobelName, 
                Map.of(Requirements.INTOP, 18L * FRAME_SIZE / par_grays),
                (long) 6.7 * Units.kB * Units.BYTES_TO_BITS
            );
            app.AddHWImplementation(
                sobelName,
                2250000 * Units.CLOCK_CYCLE,
                300 * Units.MHz,
                (long) 6.7 * Units.kB * Units.BYTES_TO_BITS,
                132 * Units.CLB
            ); 
            app.CreateChannel(grayName, sobelName, FRAME_SIZE / par_grays, FRAME_SIZE / par_grays);
            app.CreateChannel(sobelName, SYNC_AND_RESIZE_ACTOR, FRAME_SIZE / par_grays, FRAME_SIZE / par_grays);
        }

        app.AddActor(CNN_OBJ_DET);
        app.AddSWImplementation(
            CNN_OBJ_DET, 
            Map.of(Requirements.FLOP, 21000000L),
            756 * Units.kB * Units.BYTES_TO_BITS
        );
        app.AddHWImplementation(
            CNN_OBJ_DET,
            5600000 * Units.CLOCK_CYCLE,
            300 * Units.MHz,
            756 * Units.kB * Units.BYTES_TO_BITS,
            5650 * Units.CLB
        );

        app.CreateChannel(SYNC_AND_RESIZE_ACTOR, CNN_OBJ_DET, FRAME_SIZE, FRAME_SIZE);
        app.SetOutputChannel(CNN_OBJ_DET, 10);

        return app.GetGraph();
    }

    public static SystemGraph Test() {
        var app = new ApplicationBuilder("Test");

        int numActors = 10;
        // var actors = new String[numActors];
        // var flopValues = new Long[numActors];

        // for (int i = 0; i < numActors; i++) {
        //     actors[i] = "Actor_" + (i+1);
        //     flopValues[i] = 80L;
        // }

        String prevActorName = null;
        for (int i = 0; i < numActors; i++) {
            var name = "Actor_" + (i+1);
            app.AddActor(name);
            app.AddSWImplementation(
                name,
                Map.of(Requirements.FLOP, 80L), 
                4 * Units.BYTES_TO_BITS
            );
            app.AddHWImplementation(
                name, 
                10 * Units.CLOCK_CYCLE, 
                200 * Units.MHz,
                2 * Units.BYTES_TO_BITS,
                110 * Units.CLB
            );
            if (prevActorName != null) {
                app.CreateChannel(prevActorName, name, 5, 5);
            } else {
                app.SetInputChannel(name, 5);
            }
            prevActorName = name;
        }

        app.SetOutputChannel(prevActorName, 5);

        // var maxActors = 2;
        // String prevActorName = null;
        // for (int i = 0; i < maxActors; i++) {
        //     var name = "Actor_" + (i+1);
        //     app.AddActor(name);
        //     app.AddHWImplementation(
        //         name, 
        //         10 * Units.CLOCK_CYCLE, 
        //         200 * Units.MHz,
        //         2 * Units.kB * Units.BYTES_TO_BITS,
        //         110 * Units.CLB
        //     );
        //     app.AddSWImplementation(
        //         name,
        //         Map.of(Requirements.FLOP, 9000L), 
        //         4 * Units.kB * Units.BYTES_TO_BITS
        //     );

        //     if (prevActorName != null) {
        //         app.CreateChannel(prevActorName, name, 1, 1);
        //     } else {
        //         app.SetInputChannel(name, 1);
        //     }
        //     prevActorName = name;
        // }
        // app.SetOutputChannel(prevActorName, 1);

        return app.GetGraph();
    }
}
