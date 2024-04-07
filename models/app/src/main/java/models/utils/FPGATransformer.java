
package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import models.platform_model.components.Platform;

import java.util.Map;
import java.util.stream.Collectors;


public class FPGATransformer {
    /**
     * Check if the graph contains FPGAs, i.e. instances of 
     * `LogicProgrammableModule`.
     * @param g The graph to check.
     * @return True if the graph contains an FPGA, false otherwise.
     */
    public static boolean ShouldTransform(SystemGraph g) {
        long fpgaCount = g.vertexSet()
            .stream()
            .flatMap(v -> 
                LogicProgrammableModule.tryView(g, v).stream()
            ).filter(v -> 
                v.getIdentifier().equals("FPGA")
            ).count();
        
        return fpgaCount > 0;
    }

    /**
     * Transforms the application and platform graphs to accomodate for actors
     * (InstrumentedBehavior view) that can be implemented in hardware. If an
     * actor has defined hardware computational requirements, a new CPU is added:
     * <pre>{@code
     * "computationalRequirements": { // "Actor_A"
     *     "HW_Instr": {
     *         "Add": 1000_l
     *     },
     *     ...
     * }
     * }</pre>
     * The actor's hardware requirements are derived and a new Processing Module
     * is added to the platform. The actor's hardware requirements are then 
     * updated to reflect the new Processing Module's instructions which results
     * in an exclusively available Processing Module for the actor.  
     * for the actor.
     * <pre>{@code
     * "modalInstructions": { // "HW_Impl_Actor_A" (Processing Module)
     *     "HW_Instr_Actor_A": {
     *         "Add": 1.0
     *     }
     * }
     * "computationalRequirements": { // "Actor_A"
     *     "HW_Instr_Actor_A": {
     *         "Add": 1000_l
     *     },
     *     ...
     * }
     * }</pre>
     * @param platformGraph The platform graph to transform.
     * @param applicationGraph The application graph to transform.
     * @return A map containing transformed input graphs.
     */
    public static Map<String, SystemGraph> Transform(
        SystemGraph platformGraph, SystemGraph applicationGraph
    ) {
        var actorRequirements = applicationGraph.vertexSet()
            .stream()
            .flatMap(v -> 
                InstrumentedBehaviour.tryView(applicationGraph, v).stream()
            )
            .filter(v -> v.computationalRequirements().containsKey(
                Instructions.HW_INSTRUCTIONS
            ))
            .collect(Collectors.toList());

        Platform platform = new Platform(
            "TransformedPlatform", platformGraph
        );
        actorRequirements.forEach(r -> {
            String actorName = r.getViewedVertex().getIdentifier();
            var instrs = r.computationalRequirements();
            var hwInstrsName = Instructions.HW_INSTRUCTIONS + "_" + actorName;
            System.out.println(instrs);
            // extract actor's HW requirements and create instructions for them
            var hwPuInstrs = instrs
                .get(Instructions.HW_INSTRUCTIONS)
                .keySet()
                .stream()
                .collect(Collectors.toMap(
                    instr -> instr, instr -> 1.0 // default cycle req. of 1
                ));
            System.out.println(Map.of(
                hwInstrsName, hwPuInstrs
            ));

            // update key name for actor's hw requirements
            instrs.put(
                hwInstrsName,
                instrs.get(Instructions.HW_INSTRUCTIONS)
            );
            instrs.remove(Instructions.HW_INSTRUCTIONS);
            r.computationalRequirements(instrs);
            
            String hwImplName = "HW_Impl_" + actorName;
            int cores = 1;
            // TODO: add as many cores as available FPGAs, currently only 1
            platform.AddCPU(
                hwImplName, 
                cores, 
                600 * Units.MHz, 
                Map.of(
                    hwInstrsName, hwPuInstrs
                )
            );
        });

        System.out.println(
            "Transformed " + actorRequirements.size() + " actors: " + 
            actorRequirements.stream()
                    .map(a -> a.getIdentifier())
                    .collect(Collectors.toList())
        );
        
        return Map.of(
            "platform", platform.GetGraph(),
            "application", applicationGraph
        );
    }
}
