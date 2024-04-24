package models.application_model;

import forsyde.io.core.SystemGraph;
import models.utils.Requirements;
import models.utils.Units;

import java.util.*;


public class ApplicationHandler {
    
    // reference applications
    final static String VC1_APP_NAME = "VC1";
    final static String VC2_APP_NAME = "VC2";
    final static String AIC1_APP_NAME = "AIC1";
    final static String AIC2_APP_NAME = "AIC2";
    final static String PIXEL1 = "PIXEL1";
    final static String PIXEL2 = "PIXEL2";
    final static String STENCIL = "STENCIL";
    final static String NN = "NN";
    
    // toy application
    final static String TOY_SDF_APP_NAME = "ToySDF";
    final static String ACTOR_A_NAME = "Actor_A";
    final static String ACTOR_B_NAME = "Actor_B";

    public static SystemGraph ToySDFGraph() {
        var app = new ApplicationBuilder(TOY_SDF_APP_NAME);

        // ACTOR A
        app.AddActor(ACTOR_A_NAME);
        app.AddSWImplementation(
            ACTOR_A_NAME, 
            Map.of(Requirements.INT_ADD, 1000L), 
            40000
        );
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
     * Video Application Chain 1, including actors:
     * 'pixel1', 'pixel2' and 'stencil'.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph VC1Graph() {
        var app = new ApplicationBuilder(VC1_APP_NAME);
        var a1_name = VC1_APP_NAME + "_" + PIXEL1;
        var a2_name = VC1_APP_NAME + "_" + PIXEL2;
        var a3_name = VC1_APP_NAME + "_" + STENCIL;

        app.AddActor(a1_name);
        app.AddSWImplementation(
            a1_name, 
            Map.of(Requirements.FLOP, 600L), // "6"
            6000
        );
        app.AddHWImplementation(
            a1_name, 
            100 * Units.CLOCK_CYCLE, //TODO find reasonble value
            1000 * Units.CLB //TODO find reasonble value
        );

        app.AddActor(a2_name);
        app.AddSWImplementation(
            a2_name, 
            Map.of(Requirements.FLOP, 2400L), // "24"
            24000
        );
        app.AddHWImplementation(
            a2_name,
            240 * Units.CLOCK_CYCLE, //TODO find reasonble value
            2400 * Units.CLB); //TODO find reasonble value

        app.AddActor(a3_name);
        app.AddSWImplementation(
            a3_name, 
            Map.of(Requirements.FLOP, 600L), // "6"
            6000
        );
        app.AddHWImplementation(
            a3_name,
            60 * Units.CLOCK_CYCLE, //TODO find reasonble value
            600 * Units.CLB //TODO find reasonble value
        );

        app.SetInputChannel(a1_name, 1);
        app.CreateChannel(a1_name, a2_name, 1, 5);
        app.CreateChannel(a2_name, a3_name, 5, 1);
        app.SetOutputChannel(a3_name, 1);

        return app.GetGraph();
    }

    /**
     * Video Application Chain 2, including actors:
     * 'pixel1', 'pixel2' and 'stencil'.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph VC2Graph() {
        var app = new ApplicationBuilder(VC2_APP_NAME);
        var a1_name = VC2_APP_NAME + "_" + PIXEL1;
        var a2_name = VC2_APP_NAME + "_" + PIXEL2;
        var a3_name = VC2_APP_NAME + "_" + STENCIL;

        app.AddActor(a1_name);
        app.AddSWImplementation(
            a1_name, 
            Map.of(Requirements.FLOP, 600L), // "6"
            1
        );

        app.AddActor(a2_name);
        app.AddSWImplementation(
            a2_name, 
            Map.of(Requirements.FLOP, 2400L), // "24"
            1
        );

        app.AddActor(a3_name);
        app.AddSWImplementation(
            a3_name, 
            Map.of(Requirements.FLOP, 200L), // "6"
            1
        );

        app.SetInputChannel(a1_name, 1);
        app.CreateChannel(a1_name, a2_name, 1, 5);
        app.CreateChannel(a1_name, a3_name, 1, 1);
        app.CreateChannel(a2_name, a3_name, 5, 1);
        app.SetOutputChannel(a3_name, 1);

        return app.GetGraph();
    }

    /**
     * AI Chain 1.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph AIC1() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * AI Chain 2.
     * @return SystemGraph representing the application.
     */
    public static SystemGraph AIC2() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
