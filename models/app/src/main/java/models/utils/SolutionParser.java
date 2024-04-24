package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.MemoryMapped;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.Scheduled;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.AnalyzedBehavior;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.BoundedBufferLike;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.SuperLoopRuntime;

import java.io.IOException;
import java.io.FileWriter;


public class SolutionParser {
    private final String BOLD = "\033[1m";
    private final String STOPBOLD = "\033[0m";
    private SystemGraph graph;
    private StringBuilder memoryMappings;
    private StringBuilder schedules;
    private StringBuilder superLoops;
    private StringBuilder actorThroughputs;
    private StringBuilder boundedBuffers;
    private String solution;

    public SolutionParser(SystemGraph g) {
        this.graph = g;
        this.memoryMappings = new StringBuilder(
            "\n" + BOLD + "Mappings: Actor/Buffer <--> Memory" + STOPBOLD + "\n" 
        );
        this.schedules = new StringBuilder(
            "\n" + BOLD + "Schedules:" + STOPBOLD + "\n"
        );
        this.superLoops = new StringBuilder(
            "\n" + BOLD + "Superloops:" + STOPBOLD + "\n"
        );
        this.actorThroughputs = new StringBuilder(
            "\n" + BOLD + "Actor throughput:" + STOPBOLD + "\n"
        );
        this.boundedBuffers = new StringBuilder(
            "\n" + BOLD + "Buffers:" + STOPBOLD + "\n"
        );
    }
    
    /**
     * Parses the DSE solution given in <graph>, including:
     * <p>
     * - Memory mappings - Where actors and data buffers are placed in memory
     * <p>
     * - Schedules - Where an actor should be executed (processing unit)
     * <p>
     * - Analyzed behaviors
     * <p>
     * - Bounded buffers
     * <p>
     * - Super loop runtimes
     */
    public void ParseSolution() {
        graph.vertexSet().forEach(v -> {
            MemoryMapped.tryView(graph, v).ifPresent(mm -> {
                var mappedTo = mm.mappingHost();
                memoryMappings.append(
                    v.getIdentifier() + " --> " + mappedTo.getIdentifier()
                + "\n");
            });
            Scheduled.tryView(graph, v).ifPresent(s -> {
                schedules.append(
                    v.getIdentifier() + " --> " + s.runtimeHost().getIdentifier()
                + "\n");
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
                var num = ab.throughputInSecsNumerator();
                var den = ab.throughputInSecsDenominator();
                actorThroughputs.append(
                    ab.getIdentifier()  + ": " + 
                    num + "/" + den + " (" + num / den + ")"
                + "\n");
            });
            BoundedBufferLike.tryView(graph, v).ifPresent(bb -> {
                boundedBuffers.append(
                    bb.getIdentifier() + " --> " + bb.maxElements() + 
                    " elements * " + bb.elementSizeInBits() + " bits\n"
                );
            });
        });
        
        this.solution = memoryMappings
            .append(schedules)
            .append(superLoops)
            .append(actorThroughputs)
            .append(boundedBuffers).toString();
    }

    public void PrintSolution() {
        assert solution != null : "Solution must be parsed first.";
        System.out.println(solution);
    }

    public void WriteSolution(String outPath) {
        assert solution != null : "Solution must be parsed first.";
        var stripped = solution
            .replace(BOLD, "**")
            .replace(STOPBOLD, "**");
        try {
            var writer = new FileWriter(outPath);
            writer.write(stripped);
            writer.close();
            System.out.println("Solution written to " + outPath);
        } catch (IOException e) {
            System.err.println("Failed to write solution to " + outPath);
        }
    }

}
