// MIT License

// Copyright (c) 2024 Saab AB

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

package models.utils;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.MemoryMapped;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.Scheduled;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.AnalyzedBehavior;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.BoundedBufferLike;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.LogicProgrammableSynthetized;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.SuperLoopRuntime;

import java.io.IOException;
import java.io.FileWriter;


/**
 * Functionality to derive concise information from DSE solutions.
 */
public class SolutionParser {
    private final String BOLD = "\033[1m";
    private final String STOPBOLD = "\033[0m";
    private SystemGraph graph;
    private StringBuilder memoryMappings;
    private StringBuilder schedules;
    private StringBuilder plMappings;
    private StringBuilder superLoops;
    private StringBuilder actorThroughputs;
    private StringBuilder boundedBuffers;
    private String solution;

    /**
     * Create a new instance of the parser.
     * @param g The DSE solution as a SystemGraph.
     */
    public SolutionParser(SystemGraph g) {
        this.graph = g;
        this.memoryMappings = new StringBuilder(
            "\n" + BOLD + "Mappings: Actor/Buffer <--> Memory" + STOPBOLD + "\n" 
        );
        this.schedules = new StringBuilder(
            "\n" + BOLD + "Schedules:" + STOPBOLD + "\n"
        );
        this.plMappings = new StringBuilder(
            "\n" + BOLD + "PL Mappings:" + STOPBOLD + "\n"
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
     * Parses the DSE solution given in <graph>, producing:
     * <p>
     * - Memory mappings: Where application code and SDF channels are placed in memory
     * <p>
     * - Schedules: Where actors mapped to software (CPU) should be executed
     * <p>
     * - Execuction mappings: Where an actor should be executed (processing unit)
     * <p>
     * - Analyzed behaviors: How many tokens the actor can produce per second
     * <p>
     * - Bounded buffers: Calculated capacity requirement for the SDF channels
     * <p>
     * - Super loop runtimes
     */
    public void ParseSolution() {
        graph.vertexSet().forEach(v -> {
            LogicProgrammableSynthetized.tryView(graph, v).ifPresent(lps -> {
                var mappedTo = lps.hostLogicProgrammableModule();
                plMappings.append(
                    v.getIdentifier() + " --> " + mappedTo.getIdentifier()
                + "\n");
            });
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
                    ab.getIdentifier()  + " = " + num + "/" + den + " tokens/sec"
                + "\n");
            });
            BoundedBufferLike.tryView(graph, v).ifPresent(bb -> {
                var tokens = bb.maxElements();
                var bits = bb.elementSizeInBits();
                boundedBuffers.append(
                    bb.getIdentifier() + ": " + tokens + 
                    " tokens * " + bits + " bits (" 
                    + (tokens * bits / 8) + " Bytes)\n"
                );
            });
        });
        
        this.solution = memoryMappings
            .append(schedules)
            .append(plMappings)
            .append(superLoops)
            .append(actorThroughputs)
            .append(boundedBuffers).toString();
    }

    /**
     * Write the solution to stdout.
     */
    public void PrintSolution() {
        assert solution != null : "Solution must be parsed first.";
        System.out.println(solution);
    }

    /**
     * Write the solution to a file.
     * @param outPath Where to save the file to.
     */
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
        } 
        catch (IOException e) {
            System.err.println("Failed to write solution to " + outPath + ": " + e.getMessage());
        }
    }

}
