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

package models;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import forsyde.io.core.SystemGraph;

import models.application_model.*;
import models.platform_model.*;
import models.utils.Printer;
import models.utils.SolutionParser;

/**
 * The entry point to the ForSyDe IO interfacing application.
 */
public class App {

    /**
     * Default printout showing application usage.
     */
    private static void SystemExit() {
        final String USAGE = """
            Usage: gradle run --args=\"[
                build <platformType> <applicationType> <outDir> |
                to_kgt <inPath> <outDir> |
                parse_solution <inPath> <outDir> |
                build_bench_application <numActors> <numHwImpls> <outDir>
            ]\"

            \033[4mbuild\033[0m - build system specifications:
            \t<platformType>: 'mpsoc', 'zynq', 'mm' 
            \t<applicationType>: 'tc1', tc2', 'tc3', 'tc45', 'real'
            \t<outDir>: where to store the resulting specification
            \033[4mto_kgt\033[0m - convert fiodl to kgt (visualization format)
            \t<inPath>: path to the solution file (fiodl)
            \t<outDir>: where to store the resulting specification
            \033[4mparse_solution\033[0m - extract concise information from a solution
            \t<inPath>: path to the solution file (fiodl)
            \t<outDir>: where to store the resulting specification
            \033[4mbuild_bench_application\033[0m - create sequential SDF application
            \t<numActors>: number of actors
            \t<numHwImpls>: how many actors having hw and sw implementations
            \t<outDir>: where to store the resulting specification
        """;
        System.out.println(USAGE);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2)
            SystemExit();

        String outDir;
        try {
            outDir = args[args.length - 1];
            Path.of(outDir); // simple check if the path is valid
        } catch (InvalidPathException e) {
            System.out.println("Invalid output directory.");
            System.exit(1);
            return;
        }
        String action = args[0];
        if (action.equals("build")) {
            CreateBuildSpecification(args, outDir);
        } else if (action.equals("to_kgt")) {
            ConvertFiodlToKGT(args, outDir);
        } else if (action.equals("parse_solution")) {
            ParseDseSolution(args, outDir);
        } else if (action.equals("build_bench_application")) {
            CreateBenchApplication(args, outDir);
        } else {
            SystemExit();
        }
    }

    /**
     * Create a visualizable format (.kgt) from an input system 
     * specification (.fiodl).
     * @param args The path to the .fiodl file.
     * @throws Exception If the .fiodl file can't be read or .kgt file
     * can't be written.
     */
    private static void ConvertFiodlToKGT(String[] args, String outDir) throws Exception {
        if (args.length < 3)
            SystemExit();

        String path = args[1];
        assert path.endsWith(Printer.FIODL_EXT): "Must provide a .fiodl file.";

        SystemGraph g = new Printer(path).Read();
        System.out.println("Converting " + path);
        new Printer(
            outDir + "/" + path.substring(path.lastIndexOf('/'), path.indexOf('.')) + Printer.KGT_EXT
        ).PrintKGT(g);
    }
    
    /**''
     * Create platform and application specifications based on command line
     * arguments.
     * @param args The platform and application type
     * @throws Exception If the file can't be written.
     */
    private static void CreateBuildSpecification(String[] args, String outDir) throws Exception {
        if (args.length < 4)
            SystemExit();

        String platformType = args[1];
        SystemGraph gPlatform = switch (platformType.toLowerCase()) {
            case "mpsoc" -> PlatformHandler.MPSoCGraph();
            case "zynq" -> PlatformHandler.ZynqGraph();
            case "mm" -> PlatformHandler.MMGraph();
            default -> throw new IllegalStateException(
                "Unknown platform: " + platformType + " (mpsoc, zynq, mm)"
            );
        }; 
        String applicationType = args[2];
        SystemGraph gApplication = switch (applicationType.toLowerCase()) {
            case "tc1" -> ApplicationHandler.TC1();
            case "tc2" -> ApplicationHandler.TC2();
            case "tc3" -> ApplicationHandler.TC3();
            case "tc45" -> ApplicationHandler.TC4And5();
            case "real" -> ApplicationHandler.Realistic();
            default -> throw new IllegalStateException(
                "Unknown application: " + applicationType + 
                " (tc1, tc2, tc3, tc45, real)"
            );
        };

        String platformPath = outDir + "/" + platformType + Printer.FIODL_EXT;
        new Printer(platformPath).PrintFIODL(gPlatform);

        String applicationPath = outDir + "/" + applicationType + Printer.FIODL_EXT;
        new Printer(applicationPath).PrintFIODL(gApplication);
    }

    /**
     * Create a benchmark sequential SDF benchmark application.
     * @param args The path to the solution .fiodl file
     * @throws Exception If the file can't be written.
     */
    private static void CreateBenchApplication(String[] args, String outDir) throws Exception {
        System.out.println(args.toString());
        if (args.length < 4)
            SystemExit();
        int actors = Integer.parseInt(args[1]);
        int hwImpls = Integer.parseInt(args[2]);

        String appName = "A" + actors + "_" + "HW" + hwImpls;
        SystemGraph g = ApplicationHandler.SequentialSDF(
            appName, actors, hwImpls
        );
        String outPath = outDir + "/" + appName + Printer.FIODL_EXT;
        new Printer(outPath).PrintFIODL(g);
    }
        
    /**
     * Parses a DSE solution produced by IDeSyDe into a text file.
     * @param args The path to the solution .fiodl file
     * @throws Exception If the file can't be read.
     */
    private static void ParseDseSolution(String[] args, String outDir) throws Exception {
        if (args.length < 2)
            SystemExit();
        
        String path = args[1];
        assert path.endsWith(Printer.FIODL_EXT): "Must provide a .fiodl file.";

        var parser = new SolutionParser(new Printer(path).Read());
        parser.ParseSolution();
        
        parser.PrintSolution();
        parser.WriteSolution(
            outDir + "/" +
            path.substring(path.lastIndexOf('/'), path.lastIndexOf('.'))
            + ".txt"
        );
    }
}
    