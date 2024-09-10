// MIT License

// Copyright (c) 2024 Saab AB

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

package models.application_model;

import forsyde.io.core.SystemGraph;
import models.utils.Requirements;
import models.utils.Units;

import java.util.*;


/**
 * Class for simple creation of SDF application specifications. Includes 5 test
 * cases, a realistic video streaming application and parameterized creation of
 * a sequential SDF application used for benchmarking performance of IDeSyDe.
 */
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
     * @return SystemGraph representing the application.
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
     * @return SystemGraph representing the application.
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
        final int FRAME_SIZE = 3840*2160;
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

    /**
     * Parameterized SDF application with a sequential actor network and identical
     * productions and consumptions. 
     * @param name The name of the application.
     * @param actors The total number of actors.
     * @param hwImplActors How many actors supporting a hardware implementation.
     * @return SystemGraph representing the application
     */
    public static SystemGraph SequentialSDF(String name, int actors, int hwImplActors) {
        final int MIN_ACTORS = 2;
        var app = new ApplicationBuilder(name);

        // time for 2-10 actors where 1-10 specifies hw impl
        for (int a = MIN_ACTORS; a < actors + MIN_ACTORS; a++) {
            String actorName = "Actor_" + (a-1);
            app.AddActor(actorName);
            app.AddSWImplementation(
                actorName,
                Map.of(Requirements.FLOP, 80L), 
                4 * Units.BYTES_TO_BITS
            );
            if (a - MIN_ACTORS < hwImplActors) {
                app.AddHWImplementation(
                    actorName, 
                    10 * Units.CLOCK_CYCLE, 
                    200 * Units.MHz,
                    2 * Units.BYTES_TO_BITS,
                    110 * Units.CLB
                );
            }
            if (a > MIN_ACTORS) {
                app.CreateChannel("Actor_" + (a-2), actorName, 5, 5);
            } else {
                app.SetInputChannel(actorName, 5);
            }
        }
        app.SetOutputChannel("Actor_" + (actors-1), 5);

        return app.GetGraph();
    }
}
