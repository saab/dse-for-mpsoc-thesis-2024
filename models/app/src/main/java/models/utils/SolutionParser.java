package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.MemoryMapped;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.Scheduled;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.AnalyzedActor;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.AnalyzedBehavior;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.BoundedBufferLike;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.SuperLoopRuntime;

public class SolutionParser {

    private SystemGraph graph;

    public SolutionParser(SystemGraph g) {
        this.graph = g;
    }
    
    /**
     * Parse the solution of the DSE. Included data:
     * <p>
     * - Memory mappings 
     * <p>
     * - Schedules
     * <p>
     * - Analyzed actors
     * <p>
     * - Analyzed behaviors
     * <p>
     * - Bounded buffers
     * <p>
     * - Super loop runtimes
     * @return A string representation of the solution.
     */
    public String ParseSolution() {
        StringBuilder memoryMappings = new StringBuilder("\nMappings:\n");
        StringBuilder schedules = new StringBuilder("\nSchedules:\n");

        graph.vertexSet().forEach(v -> {
            MemoryMapped.tryView(graph, v).ifPresent(mm -> {
                var memModule = mm.mappingHost();
                memoryMappings.append(
                    v.getIdentifier() + " --> " + memModule.getIdentifier() + "\n"
                );
            });
            Scheduled.tryView(graph, v).ifPresent(s -> {
                schedules.append(
                    v.getIdentifier() + " --> " + s.getIdentifier() + "\n"
                );
            });
            AnalyzedActor.tryView(graph, v).ifPresent(aa -> {
                // var actor = aa.;
                System.out.println("Analyzed actor " + aa.getIdentifier());
            });
            AnalyzedBehavior.tryView(graph, v).ifPresent(ab -> {
                // var behavior = ab.;
                System.out.println("Analyzed behavior " + ab.getIdentifier());
            });
            BoundedBufferLike.tryView(graph, v).ifPresent(bb -> {
                // var buffer = bb.;
                System.out.println("Bounded buffer " + bb.getIdentifier());
            });
            SuperLoopRuntime.tryView(graph, v).ifPresent(sl -> {
                // var runtime = sl.;
                var entries = sl.superLoopEntries();
                if (entries.size() > 0) {
                    System.out.println(sl.getIdentifier() + " SUPERLOOP: " + entries);
                }
            });
        });
        
        var combined = memoryMappings.append("\n\n").append(schedules);
        System.out.println(combined);

        return combined.toString();
    }

}
