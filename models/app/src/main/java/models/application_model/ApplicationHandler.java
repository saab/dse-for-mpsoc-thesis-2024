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
        final String GRAY = "Grayscale";
        final String SYNC_GRAY = "SyncGray";
        final String SOBEL = "Sobel";
        final String CNN_OBJ_DET = "ObjectDetection";

        
        var app = new ApplicationBuilder(APP_NAME);
        var grays = new ArrayList<String>();
        
        for (int i = 0; i < 3; i++) {
            String name = GRAY + i;
            app.AddActor(name);
            app.AddSWImplementation(
                name, 
                Map.of(Requirements.FLOP, 8L),
                100 * Units.BYTES_TO_BITS
            );
            app.AddHWImplementation(
                name, 
                1 * Units.CLOCK_CYCLE,
                300 * Units.MHz,
                0,
                100 * Units.CLB
            );
            grays.add(name);
        }

        app.AddActor(SOBEL);
        app.AddSWImplementation(
            SOBEL, 
            Map.of(Requirements.INTOP, 18L),
            (long) 6.7 * Units.kB * Units.BYTES_TO_BITS
        );
        app.AddHWImplementation(
            SOBEL,
            1 * Units.CLOCK_CYCLE,
            300 * Units.MHz,
            (long) 6.7 * Units.kB * Units.BYTES_TO_BITS,
            132 * Units.CLB
        );

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

        app.AddActor(SYNC_GRAY);
        app.AddSWImplementation(
            SYNC_GRAY, 
            Map.of(Requirements.INTOP, 1L),
            3 * Units.BYTES_TO_BITS
        );

        grays.forEach(gray -> {
            app.SetInputChannel(gray, 1);
            app.CreateChannel(gray, SYNC_GRAY, 1, 1);
        });

        app.CreateChannel(SYNC_GRAY, SOBEL, 9, 9);
        // app.SetOutputChannel(SOBEL, 1);
        app.CreateChannel(SOBEL, CNN_OBJ_DET, 1, 240*240);
        app.SetOutputChannel(CNN_OBJ_DET, 10);

        return app.GetGraph();
    }
}
