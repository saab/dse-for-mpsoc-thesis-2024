
package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import models.platform_model.components.Platform;

import java.util.Map;
import java.util.stream.Collectors;


public class FPGATransformer {
    /**
     * Check if the graph contains FPGAs, i.e. instances of 
     * `ProgrammableLogicStructure`.
     * @param g The graph to check.
     * @return True if the graph contains an FPGA, false otherwise.
     */
    public static boolean ShouldTransform(SystemGraph g) {
        long fpgaCount = g.vertexSet()
            .stream()
            .flatMap(v -> 
                Structure.tryView(g, v).stream()
            ).filter(v -> 
                v.getIdentifier().equals("FPGA")
            ).count();
        
        return fpgaCount > 0;
    }

    public static Map<String, SystemGraph> Transform(
        SystemGraph platformGraph, SystemGraph applicationGraph
    ) {
        // List<SDFActorViewer> actors = g.vertexSet()
        //     .stream()
        //     .flatMap(v -> SDFActorViewer.tryView(g, v).stream())
        //     .collect(Collectors.toList());
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
            
            // extract actor's HW requirements and create instructions for them
            var hwPuInstrs = instrs
                .get(Instructions.HW_INSTRUCTIONS)
                .keySet()
                .stream()
                .collect(Collectors.toMap(
                    instr -> instr, instr -> 1.0 // default cycle req. of 1
                ));

            // update key for actor's hw requirements
            instrs.put(
                hwInstrsName,
                instrs.get(Instructions.HW_INSTRUCTIONS)
            );
            instrs.remove(Instructions.HW_INSTRUCTIONS);
            r.computationalRequirements(instrs);
            
            String hwImplName = "HW_Impl_" + actorName;
            int cores = 1;
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
