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

package models.application_model;

import java.util.*;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.Vertex;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFActorViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;
import models.utils.Requirements;


/**
 * Class for managing an arbitrary SDF application specification. Supports 
 * specification of actors, software implementations, hardware
 * implementations, connection of actors with production and consumption, and 
 * setting the input and output channels of the entire application.
 */
public class ApplicationBuilder {
	private SystemGraph sGraph;
    private GreyBoxViewer greyBox;
    private Map<String, VertexViewer> viewers = new HashMap<>();

	public ApplicationBuilder(String name) {
		SystemGraph sGraph = new SystemGraph();

		this.greyBox = GreyBox.enforce(Visualizable.enforce(sGraph, sGraph.newVertex(name)));
		this.sGraph = sGraph;
	}

	/**
     * Create an application from an existing graph, by extraction of the viewers.
     * @param g The existing system graph to use as application base.
     */
	public ApplicationBuilder(String name, SystemGraph g) {
        this.sGraph = g;
		this.greyBox = g.vertexSet()
            .stream()
            .flatMap(v -> GreyBoxViewer.tryView(g, v).stream())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No GreyBox for visualization found in the given graph."
            ));
		
		for (var v : g.vertexSet()) {
			SDFActor.tryView(g, v).ifPresent(
				actor -> this.viewers.put(v.getIdentifier(), actor)
			);
			SDFChannel.tryView(g, v).ifPresent(
				chan -> this.viewers.put(v.getIdentifier(), chan)
			);
		}
	}

	/**
	 * Get the current state of the application graph.
	 * @return
	 */
	public SystemGraph GetGraph() {
		return this.sGraph;
	}

	/**
	 * Add an actor to the application.
	 * @param name Name of the actor
	 * @param prod Production rates of the actor
	 * @param cons Consumption rates of the actor
	 * @param instrs Computational requirements of the actor
	 */
	public void AddActor(String name) {
		var a = SDFActor.enforce(sGraph, sGraph.newVertex(name));
		this.greyBox.addContained(Visualizable.enforce(a));
		this.viewers.put(name, a);
		a.production(Map.of());
		a.consumption(Map.of());
	}

	/**
	 * Get the actor with the given name.
	 * @param name Name of the actor
	 * @return The actor's viewer
	 * @throws RuntimeException if the actor is not created
	 */
	private SDFActorViewer GetActor(String name) {
		try {
			return SDFActor.tryView(viewers.get(name))
				.orElseThrow(() -> 
					new RuntimeException("Actor not found: " + name)
				);
		} catch (NullPointerException e) {
			throw new RuntimeException("Actor not found: " + name);
		}
	}

	/**
	 * Connect two components visually.
	 * @param V1 First component
	 * @param V2 Second component
	 * @param V1ToV2Port Port on the first actor
	 * @param V2ToV1Port Port on the second actor
	 */
	private void Connect(
		VertexViewer V1, VertexViewer V2, String V1ToV2Port, String V2ToV1Port
	) {
		sGraph.connect(
			V1, V2, V1ToV2Port, V2ToV1Port, 
			EdgeTraits.SDFNetworkEdge,
			EdgeTraits.VisualConnection
		);
	}

	/**
	 * Set the production or consumption from the input Map.
	 * @param current The current Map of production and consumption.
	 * @param port What the new production/consumption entry should be named.
	 * @param numTokens How many tokens to specify as production or consumption. 
	 * @return The updated Map of production and consumption specifications.
	 * @exception RuntimeException If port for prod/cons already exists.
	 */
	private Map<String, Integer> GetUpdatedProdOrCons(
		Map<String, Integer> current, String port, int numTokens
	) {
		if (current.get(port) != null) {
			throw new RuntimeException(
				"Consumption/Production port already exists: " + port
			);
		} else {
			current = new LinkedHashMap<String, Integer>(current);
			current.put(port, numTokens);
		}

		return current;
	}

	/**
	 * Add software implementation alternative to an actor.
	 * @param actorName Name of the actor (must exist).
	 * @param instrs Software instructions.
	 * @param codeSizeInBits Implementation size in bits.
	 */
	public void AddSWImplementation(
		String actorName, Map<String, Long> instrs, long codeSizeInBits
	) {
		var actor = GetActor(actorName);
		var sw = InstrumentedSoftwareBehaviour.enforce(
			sGraph, actor.getViewedVertex()
		);

		assert instrs.size() > 0 : "Must require at least one instruction type";
		assert codeSizeInBits > 0 : "Must specify a code size > 0";

		if (instrs != null) {
			sw.computationalRequirements(
				Map.of(Requirements.SW_INSTRUCTIONS, instrs)
			);
		}
		
		sw.maxSizeInBits(Map.of(Requirements.SW_INSTRUCTIONS, codeSizeInBits));
	}

	/**
	 * Add hardware implementation alternative to an actor.
	 * @param actorName Name of the actor (must exist).
	 * @param clockCycles Clock cycles to run the hardware implementation.
	 * @param requiredArea Required area for hardware implementation.
	 * @param frequencyInMHz Frequency of the hardware implementation.
	 * @param requiredBramInBits Required block ram size in bits for the implementation.
	 * @throws AssertionError if the logic area, or block ram size is not specified.
	 */
	public void AddHWImplementation(
		String actorName, long clockCycles, long frequencyInMHz,
		long requiredBramInBits, long requiredArea
	) {
		var actor = GetActor(actorName);
		var hw = InstrumentedHardwareBehaviour.enforce(
			sGraph, actor.getViewedVertex()
		);

		assert requiredArea > 0 : "Logic area must be >= 1";
		assert clockCycles > 0 : "Clock cycles must be >= 1";
		assert frequencyInMHz > 0 : "Frequency must be >= 1";

		hw.resourceRequirements(
			Map.of(
				Requirements.FPGA, Map.of(
					Requirements.AREA, requiredArea,
					Requirements.BRAM, requiredBramInBits
				)
			)
		);
		hw.latencyInSecsNumerators(Map.of(Requirements.FPGA, clockCycles));
		hw.latencyInSecsDenominators(Map.of(Requirements.FPGA, frequencyInMHz));
	}

	/**
	 * Create a channel between two actors.
	 * @param srcActorName Name of the first actor (must exist)
	 * @param dstActorName Name of the second actor (must exist)
	 * @param numProd Number of tokens produced by the source actor
	 * @param numCons Number of tokens consumed by the destination actor
	 */
	public void CreateChannel(
		String srcActorName, String dstActorName, int numProd, int numCons
	) {
		SDFActorViewer srcActor = GetActor(srcActorName);
		SDFActorViewer dstActor = GetActor(dstActorName);
		String chanName = "CH_" + srcActorName + "_" + dstActorName;
		Vertex chanVertex = sGraph.newVertex(chanName);
		var chan = SDFChannel.enforce(
			sGraph, chanVertex
		);
		this.greyBox.addContained(Visualizable.enforce(chan));
		this.viewers.put(chanName, chan);
		chan.numInitialTokens(0);
		chan.producer(srcActor);
		chan.consumer(dstActor);
		
		var bufLike = BufferLike.enforce(
			sGraph, chanVertex
		);
		bufLike.elementSizeInBits(8L);

		String prodPortName = "to_" + dstActorName;
		var newProd = this.GetUpdatedProdOrCons(
			srcActor.production(), prodPortName, numProd
		);
		srcActor.production(newProd);
		
		String consPortName = "from_" + srcActorName;
		var newCons = this.GetUpdatedProdOrCons(
			dstActor.consumption(), consPortName, numCons
		);
		dstActor.consumption(newCons);
			
		chan.addPorts(prodPortName, consPortName);
		srcActor.addPorts(prodPortName);
		dstActor.addPorts(consPortName);
		this.Connect(srcActor, chan, prodPortName, "to_" + srcActorName);
		this.Connect(chan, dstActor, "to_" + dstActorName, consPortName);
	}

	/**
	 * Assign a new input channel with static consumption to the given actor.
	 * @param actorName Name of the actor 
	 * @param numCons Number of tokens that the actor consumes from the input
	 * channel.
	 */
	public void SetInputChannel(String actorName, int numCons) {
		SDFActorViewer actor = GetActor(actorName);
		String inName = "in_" + actorName; //? need numbering if more than 1?
		actor.addPorts(inName);
		var currCons = new LinkedHashMap<String, Integer>(actor.consumption());
		currCons.put(inName, numCons);
		actor.consumption(currCons);
	}

	/**
	 * Assign a new out channel with static production to the given actor.
	 * @param actorName Name of the actor 
	 * @param numCons Number of tokens that the actor produces for the output
	 * channel.
	 */
	public void SetOutputChannel(String actorName, int numProd) {
		SDFActorViewer actor = GetActor(actorName);
		String outName = "out_" + actorName;  //? need numbering if more than 1?
		actor.addPorts(outName);
		var currProd = new LinkedHashMap<String, Integer>(actor.production());
		currProd.put(outName, numProd);
		actor.production(currProd);
	}
}

