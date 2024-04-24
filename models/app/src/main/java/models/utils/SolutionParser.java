package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.MemoryMapped;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.Scheduled;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.AnalyzedActor;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.AnalyzedBehavior;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.BoundedBufferLike;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.SuperLoopRuntime;

public class SolutionParser {

    private final String BOLD = "\033[1m";
    private final String STOPBOLD = "\033[0m";

    private SystemGraph graph;

    public SolutionParser(SystemGraph g) {
        this.graph = g;
    }
    
    /**
     * Parse the solution of the DSE. Included data:
     * <p>
     * - Memory mappings - Where actors and data buffers are placed in memory
     * <p>
     * - Schedules - Where an actor should be executed (processing unit)
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
        StringBuilder memoryMappings = new StringBuilder(
            "\n" + BOLD + "Mappings:" + STOPBOLD + "\n" 
        );
        StringBuilder schedules = new StringBuilder(
            "\n" + BOLD + "Schedules:" + STOPBOLD + "\n"
        );
        StringBuilder superLoops = new StringBuilder(
            "\n" + BOLD + "Superloops:" + STOPBOLD + "\n"
        );
        StringBuilder analyzedBehavior = new StringBuilder(
            "\n" + BOLD + "Analyzed Behavior:" + STOPBOLD + "\n"
        );
        StringBuilder boundedBuffer = new StringBuilder(
            "\n" + BOLD + "Buffers:" + STOPBOLD + "\n"
        );

        graph.vertexSet().forEach(v -> {
            MemoryMapped.tryView(graph, v).ifPresent(mm -> {
                var mappedTo = mm.mappingHost();
                memoryMappings.append(
                    v.getIdentifier() + " --> " + mappedTo.getIdentifier()
                + "\n");
            });
            Scheduled.tryView(graph, v).ifPresent(s -> {
                schedules.append(
                    v.getIdentifier() + " --> " + s.runtimeHost().getIdentifier() + "\n");
            });
            SuperLoopRuntime.tryView(graph, v).ifPresent(sl -> {
                var entries = sl.superLoopEntries();
                if (entries.size() > 0) {
                    superLoops.append(
                        sl.getIdentifier() + ": \n\t" + entries + "\n"
                    );
                }
            });
            AnalyzedBehavior.tryView(graph, v).ifPresent(ab -> {
                analyzedBehavior.append(
                    "Analyzed behavior " + ab.getIdentifier()  + ": " + 
                    ab.throughputInSecsNumerator() + "/" + ab.throughputInSecsDenominator()
                    + " (" + ab.throughputInSecsNumerator() / ab.throughputInSecsDenominator() + ")"
                + "\n");
            });
            BoundedBufferLike.tryView(graph, v).ifPresent(bb -> {
                boundedBuffer.append(BOLD + 
                    "Bounded buffer " + bb.getIdentifier() 
                + STOPBOLD);
            });
        });
        
        var combined = memoryMappings
            .append("\n")
            .append(schedules)
            .append(superLoops)
            .append(analyzedBehavior);
        System.out.println(combined);

        return combined.toString();
    }

}
