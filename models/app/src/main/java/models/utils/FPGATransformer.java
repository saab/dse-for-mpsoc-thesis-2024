
package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.LogicProgrammableModuleViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.InstrumentedHardwareBehaviour;
import forsyde.io.lib.hierarchy.implementation.functional.InstrumentedHardwareBehaviourViewer;
import forsyde.io.lib.hierarchy.implementation.functional.InstrumentedSoftwareBehaviourViewer;
import models.platform_model.PlatformBuilder;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;


public class FPGATransformer {
    private SystemGraph platform;
    private SystemGraph application;


    public FPGATransformer(SystemGraph platform, SystemGraph application) {
        this.platform = platform;
        this.application = application;
    }

    /**
     * Get all instances of `LogicProgrammableModule` in the graph.
     * @return A list of `LogicProgrammableModule` instances.
     */
    private List<LogicProgrammableModuleViewer> GetLPViewers() {
        return platform.vertexSet().stream()
            .flatMap(v -> LogicProgrammableModule.tryView(platform, v).stream())
            .collect(Collectors.toList());
    }

    /**
     * Get all actors which have defined hardware instructions and hardware area.
     * @return A list of hardware actor instances.
     */
    private List<InstrumentedHardwareBehaviourViewer> GetHWActors() {
        return application.vertexSet().stream()
            .flatMap(v -> InstrumentedHardwareBehaviour.tryView(application, v).stream())
            .collect(Collectors.toList());
    }

    /**
     * Get all actors who have defined software instructions.
     * @return A list of software actor instances.
     */
    private List<InstrumentedSoftwareBehaviourViewer> GetSWActors () {
        return application.vertexSet().stream()
            .flatMap(v -> InstrumentedSoftwareBehaviourViewer.tryView(application, v).stream())
            .collect(Collectors.toList());        
    }

    /**
     * Check if the graph contains FPGAs and hardware actors, i.e. instances 
     * of `LogicProgrammableModule` and `InstrumentedHardwareBehavior`.
     * @return True if the graph contains both viewers, false otherwise.
     */
    public boolean ShouldTransform() {
        long fpgaCount = GetLPViewers().size();
        long hwActorCount = GetHWActors().size();
        
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
     * @return A map containing transformed input graphs.
     */
    public Map<String, SystemGraph> Transform() {
        var lpms = GetLPViewers();

        var switches = platform.vertexSet().stream().flatMap(v -> 
            InstrumentedCommunicationModule.tryView(platform, v)
                .stream()
            ).collect(Collectors.toList());

        var lpmSwitchConnections = lpms.stream().collect(
            Collectors.toMap(
                lpm -> lpm.getIdentifier(), 
                lpm -> switches.stream().filter(sw -> 
                    platform.hasConnection(lpm, sw)
                ).collect(Collectors.toList())
            )
        );

        if (lpmSwitchConnections.values().stream().anyMatch(c -> c.size() == 0)) {
            throw new IllegalArgumentException(
                "All Logic Programmable Modules must connect to a switch."
            );
        }

        PlatformBuilder builder = new PlatformBuilder(
            "TransformedPlatform", platform
        );

        var hwActors = GetHWActors();
        var swActors = GetSWActors();
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
                var lpmFreq = lpm.operatingFrequencyInHertz();

                assert lpmFreq > 0 : "LPM must have a defined frequency > 0.";
                
                // extract actor's HW requirements and convert into pu instructions
                var hwInstrsPu = hwReqs.keySet().stream()
                    .collect(Collectors.toMap(
                        instr -> lpm.getIdentifier() + "_" + instr, 
                        instr -> 1.0 
                    ));

                // match instruction names for tailored processing module
                var updatedHwReqs = hwReqs.entrySet().stream()
                    .collect(Collectors.toMap(
                        e -> lpm.getIdentifier() + "_" + e.getKey(), 
                        e -> e.getValue()
                    ));

                // update "software" instructions with hardware instructions
                swReqs.put(hwInstrsName, updatedHwReqs);
                correspondingSWActor.computationalRequirements(swReqs);
                
                // add tailored processing module for actor <actorName>
                String hwImplName = 
                    lpm.getIdentifier() + "_" + "HW_Impl_" + actorName;
                int cores = 1;
                builder.AddCPU(
                    hwImplName, 
                    cores, 
                    lpmFreq, 
                    Map.of(hwInstrsName, hwInstrsPu)
                );

                // connect tailored processing module to switch
                var sw = lpmSwitchConnections.get(lpm.getIdentifier()).get(0);
                builder.Connect(hwImplName, sw.getIdentifier());
            }
        });

        System.out.println(
            "Transformed " + hwActors.size() + " actors: " + 
            hwActors.stream().map(a -> 
                a.getIdentifier()
            ).collect(Collectors.toList())
        );
        
        return Map.of(
            "platform", builder.GetGraph(),
            "application", application
        );
    }
}
