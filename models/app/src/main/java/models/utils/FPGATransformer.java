
package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import models.platform_model.components.Platform;

import java.util.Map;
import java.util.stream.Collectors;


public class FPGATransformer {
    /**
     * Check if the graph contains FPGAs, i.e. instances of `LogicProgrammableModule`.
     * @param g The graph to inspect.
     * @return True if the graph contains an FPGA, false otherwise.
     */
    public static boolean ShouldTransform(SystemGraph g) {
        long fpgaCount = g.vertexSet()
            .stream()
            .flatMap(v -> 
                LogicProgrammableModule.tryView(g, v).stream()
            ).count();
        
        return fpgaCount > 0;
    }

    /**
     * Transforms the application and platform graphs to accomodate for actors
     * (InstrumentedHardwareBehavior view) that can be implemented in hardware. 
     * It is necessary that the ProgrammableLogicModule connects to a switch that
     * new processing modules can use as adapter to the rest of the platform.
     * 
     * If an actor has defined hardware computational requirements, a new 
     * CPU is added:
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
        var lpms = platformGraph.vertexSet()
            .stream()
            .flatMap(v -> 
                LogicProgrammableModule.tryView(platformGraph, v).stream()
            )
            .collect(Collectors.toList());

        var switches = platformGraph.vertexSet()
            .stream()
            .flatMap(v -> 
                InstrumentedCommunicationModule.tryView(platformGraph, v).stream()
            )
            .collect(Collectors.toList());

        var lpmSwitchConnections = lpms.stream().collect(
            Collectors.toMap(
                lpm -> lpm.getIdentifier(), 
                lpm -> switches.stream().filter(sw -> 
                    platformGraph.hasConnection(lpm, sw)
            ).collect(Collectors.toList()))
        );

        System.out.println(
            "Found " + lpms.size() + " logic programmable modules: " + 
            lpms.stream()
                .map(lpm -> lpm.getIdentifier())
                .collect(Collectors.toList()));
        System.out.println(
            "Found " + switches.size() + " switches: " + 
            switches.stream()
                .map(sw -> sw.getIdentifier())
                .collect(Collectors.toList()));
        System.out.println(
            "Connects to switch: " + lpmSwitchConnections);

        if (lpmSwitchConnections.values().stream().anyMatch(
            c -> c.size() == 0
        )) {
            throw new IllegalArgumentException(
                "All Logic Programmable Modules must connect to a switch."
            );
        }

        var actorRequirements = applicationGraph.vertexSet()
            .stream()
            .flatMap(v -> 
                InstrumentedBehaviour.tryView(applicationGraph, v).stream()
            )
            .filter(v -> v.computationalRequirements().containsKey(
                Requirements.HW_INSTRUCTIONS
            ))
            .collect(Collectors.toList());

        Platform platform = new Platform(
            "TransformedPlatform", platformGraph
        );
        actorRequirements.forEach(r -> {
            String actorName = r.getViewedVertex().getIdentifier();
            var instrs = r.computationalRequirements();
            var hwInstrsName = Requirements.HW_INSTRUCTIONS + "_" + actorName;
            System.out.println(instrs);
            // extract actor's HW requirements and create instructions for them
            var hwPuInstrs = instrs
                .get(Requirements.HW_INSTRUCTIONS)
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
                instrs.get(Requirements.HW_INSTRUCTIONS)
            );
            instrs.remove(Requirements.HW_INSTRUCTIONS);
            r.computationalRequirements(instrs);
            
            String hwImplName = "HW_Impl_" + actorName;
            int cores = 1;
            // TODO: add as many "CPUs" as available FPGAs, currently only 1
            platform.AddCPU(
                hwImplName, 
                cores, 
                600 * Units.MHz, 
                Map.of(
                    hwInstrsName, hwPuInstrs
                )
            );
            var sw = lpmSwitchConnections.get(
                lpmSwitchConnections.keySet().stream().findFirst().get()
            ).stream().findFirst().get();
            platform.Connect(hwImplName, sw.getIdentifier());
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
