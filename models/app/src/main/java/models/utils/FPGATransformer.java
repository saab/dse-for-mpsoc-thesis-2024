
package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.LogicProgrammableModuleViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.InstrumentedHardwareBehaviour;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.InstrumentedSoftwareBehaviour;
import forsyde.io.lib.hierarchy.implementation.functional.InstrumentedHardwareBehaviourViewer;
import forsyde.io.lib.hierarchy.implementation.functional.InstrumentedSoftwareBehaviourViewer;
import models.application_model.ApplicationBuilder;
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
     * is added to the platform to represent the actor's hardware implementation. 
     * The actor's hardware requirements are then placed as instructions with 
     * cycle requirement of one (1). Processing Module's instructions which results
     * in an exclusively available Processing Module for the actor.
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

        PlatformBuilder platformBuilder = new PlatformBuilder(
            "TransformedPlatform", platform
        );
        ApplicationBuilder applicationBuilder = new ApplicationBuilder(
            "TransformedApplication", application
        );

        var hwActors = GetHWActors();
        var swActors = GetSWActors();
        for (var lpm : lpms) {                
            var lpmFreq = lpm.operatingFrequencyInHertz();
            var bramSize = lpm.blockRamSizeInBits();

            assert lpmFreq > 1L :
                "LPM must have a defined frequency (not default of 1L).";
            assert bramSize > 0 :
                "LPM must have a defined BRAM size > 0.";

            var sw = lpmSwitchConnections.get(lpm.getIdentifier()).get(0);

            var bramName = lpm.getIdentifier() + "_BRAM";
            var bramSw = bramName + "_BRAM";

            platformBuilder.AddMemory(bramName, lpmFreq, bramSize); 
            platformBuilder.AddSwitch(bramSw, lpmFreq, sw.flitSizeInBits()); // match flit size

            hwActors.forEach(a -> {
                String actorName = a.getViewedVertex().getIdentifier();
                var hwReqs = a.resourceRequirements()
                    .get(Requirements.HW_INSTRUCTIONS);

                var correspondingSWActor = swActors.stream()
                    .filter(swA -> swA.getIdentifier().equals(actorName))
                    .findFirst()
                    .orElse(null);
                      
                if (correspondingSWActor == null) {
                    applicationBuilder.AddSWImplementation(actorName, null, 0);
                    swActors.clear();
                    swActors.addAll(GetSWActors());
                    correspondingSWActor = swActors.stream()
                        .filter(swA -> swA.getIdentifier().equals(actorName))
                        .findFirst()
                        .orElseThrow();
                }
                var swReqs = correspondingSWActor.computationalRequirements();

                var hwInstrsName = lpm.getIdentifier() + "_" +
                    Requirements.HW_INSTRUCTIONS + "_" + actorName;
                
                // convert hw requirements into sw instructions
                var hwInstrsPu = hwReqs.keySet().stream()
                    .collect(Collectors.toMap(
                        instr -> lpm.getIdentifier() + "_" + actorName + "_" + instr, 
                        instr -> 1
                    ));

                // match instruction names for tailored processing module
                var updatedHwReqs = hwReqs.entrySet().stream()
                    .collect(Collectors.toMap(
                        e -> lpm.getIdentifier() + "_" + actorName + "_" + e.getKey(), 
                        e -> e.getValue()
                    ));

                // update "software" instructions with hardware instructions
                swReqs.put(hwInstrsName, updatedHwReqs);
                correspondingSWActor.computationalRequirements(swReqs);
                
                // add tailored processing module for actor <actorName>
                String hwImplName = 
                    lpm.getIdentifier() + "_" + "HW_Impl_" + actorName;
                int cores = 1;
                platformBuilder.AddCPU(
                    hwImplName, 
                    cores, 
                    lpmFreq, 
                    Map.of(hwInstrsName, hwInstrsPu)
                );

                // connect tailored processing module to switch
                platformBuilder.ConnectTwoWay(hwImplName, sw.getIdentifier());
                // connect tailored processing module to BRAM
                platformBuilder.ConnectTwoWay(hwImplName, bramSw);
            });
        };

        System.out.println(
            "Transformed " + hwActors.size() + " actors: " + 
            hwActors.stream().map(a -> 
                a.getIdentifier()
            ).collect(Collectors.toList())
        );
        
        return Map.of(
            "platform", platformBuilder.GetGraph(),
            "application", applicationBuilder.GetGraph()
        );
    }
}
