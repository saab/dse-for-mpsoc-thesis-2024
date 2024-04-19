
package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedCommunicationModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.LogicProgrammableModuleViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.InstrumentedHardwareBehaviour;
import forsyde.io.lib.hierarchy.implementation.functional.InstrumentedHardwareBehaviourViewer;
import forsyde.io.lib.hierarchy.implementation.functional.InstrumentedSoftwareBehaviourViewer;
import models.platform_model.components.Platform;

import java.util.Map;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;


public class FPGATransformer {

    /**
     * Get all instances of `LogicProgrammableModule` in the graph.
     * @param g The graph to search for `LogicProgrammableModule` instances.
     * @return A list of `LogicProgrammableModule` instances.
     */
    private static List<LogicProgrammableModuleViewer> GetLPViewers(SystemGraph g) {
        return g.vertexSet().stream()
            .flatMap(v -> LogicProgrammableModule.tryView(g, v).stream())
            .collect(Collectors.toList());
    }

    /**
     * Get all actors which have defined hardware instructions and hardware area.
     * @param g The applicaton graph to search for hardware actors.
     * @return A list of hardware actor instances.
     */
    private static List<InstrumentedHardwareBehaviourViewer> GetHWActors(SystemGraph g) {
        return g.vertexSet().stream()
            .flatMap(v -> InstrumentedHardwareBehaviour.tryView(g, v).stream())
            .collect(Collectors.toList());
    }

    /**
     * Get all actors who have defined software instructions.
     * @param g The graph to search for the actor.
     * @param actorName The name of the actor to search for.
     * @return A list of software actor instances.
     */
    private static List<InstrumentedSoftwareBehaviourViewer> GetSWActors (SystemGraph g) {
        return g.vertexSet().stream()
            .flatMap(v -> InstrumentedSoftwareBehaviourViewer.tryView(g, v).stream())
            .collect(Collectors.toList());        
    }

    /**
     * Check if the graph contains FPGAs and hardware actors, i.e. instances 
     * of `LogicProgrammableModule` and `InstrumentedHardwareBehavior`.
     * @param gPlatform The platform graph which may include `LogicProgrammableModule`
     * @param gApplication The application graph which may include `InstrumentedHardwareBehavior`
     * @return True if the graph contains both viewers, false otherwise.
     */
    public static boolean ShouldTransform(
        SystemGraph gPlatform, SystemGraph gApplication
    ) {
        long fpgaCount = GetLPViewers(gPlatform).size();
        long hwActorCount = GetHWActors(gApplication).size();
        
        return fpgaCount > 0 && hwActorCount > 0;
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
        var lpms = GetLPViewers(platformGraph);

        var switches = platformGraph.vertexSet()
            .stream()
            .flatMap(v -> InstrumentedCommunicationModule.tryView(platformGraph, v).stream())
            .collect(Collectors.toList());

        var lpmSwitchConnections = lpms.stream().collect(
            Collectors.toMap(
                lpm -> lpm.getIdentifier(), 
                lpm -> switches.stream().filter(sw -> 
                    platformGraph.hasConnection(lpm, sw)
                ).collect(Collectors.toList())
            )
        );

        if (lpmSwitchConnections.values().stream().anyMatch(c -> c.size() == 0)) {
            throw new IllegalArgumentException(
                "All Logic Programmable Modules must connect to a switch."
            );
        }

        var hwActors = GetHWActors(applicationGraph);
        var swActors = GetSWActors(applicationGraph);

        Platform platform = new Platform(
            "TransformedPlatform", platformGraph
        );
        hwActors.forEach(a -> {
            String actorName = a.getViewedVertex().getIdentifier();
            var hwReqs = a.resourceRequirements()
                .get(Requirements.HW_INSTRUCTIONS);

            var correspondingSWActor = swActors.stream()
                .filter(swA -> swA.getIdentifier().equals(actorName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "No corresponding software implementation found for " + 
                        actorName
                ));
            var swReqs = correspondingSWActor.computationalRequirements();

            for (var lpm : lpms) {
                var hwInstrsName = lpm.getIdentifier() + "_" +
                    Requirements.HW_INSTRUCTIONS + "_" + actorName;
                
                // extract actor's HW requirements and convert into pu instructions
                var hwInstrsPu = hwReqs
                    .keySet()
                    .stream()
                    .collect(Collectors.toMap(
                        // could influence instr cycle req by some "speed grade" (xilinx)
                        instr -> lpm.getIdentifier() + "_" + instr, instr -> 1.0 
                    ));

                // match instruction names for tailored processing module
                var updatedHwReqs = hwReqs.entrySet().stream()
                    .collect(Collectors.toMap(
                        e -> lpm.getIdentifier() + "_" + e.getKey(), 
                        e -> e.getValue()
                    ));

                // update software instructions with hardware instructions
                swReqs.put(hwInstrsName, updatedHwReqs);
                correspondingSWActor.computationalRequirements(swReqs);
                
                // add tailored processing module for actor <actorName>
                String hwImplName = lpm.getIdentifier() + 
                    "_" + "HW_Impl_" + actorName;
                int cores = 1;
                platform.AddCPU(
                    hwImplName, 
                    cores, 
                    100 * Units.MHz, 
                    Map.of(hwInstrsName, hwInstrsPu)
                );

                // connect tailored processing module to switch
                var sw = lpmSwitchConnections.get(lpm.getIdentifier()).get(0);
                platform.Connect(hwImplName, sw.getIdentifier());
                // System.out.println(
                //     "Connected: " + lpm.getIdentifier() + ":" + sw.getIdentifier()
                // );
            }
        });

        System.out.println(
            "Transformed " + hwActors.size() + " actors: " + 
            hwActors.stream()
                .map(a -> a.getIdentifier())
                .collect(Collectors.toList())
        );
        
        return Map.of(
            "platform", platform.GetGraph(),
            "application", applicationGraph
        );
    }
}
