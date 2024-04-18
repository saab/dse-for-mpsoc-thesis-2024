
package models.application_model.applicationrepo;

import java.util.*;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFActorViewer;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFChannelViewer;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;
import models.utils.Requirements;


public class Application {
	private SystemGraph sGraph;
    private GreyBoxViewer greyBox;
	private StructureViewer application;
    private Map<String, VertexViewer> viewers = new HashMap<>();

	public Application(String name) {
		SystemGraph sGraph = new SystemGraph();

		this.application = Structure.enforce(sGraph, sGraph.newVertex(name));
		this.greyBox = GreyBox.enforce(Visualizable.enforce(application));
		this.sGraph = sGraph;
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

	private Map<String, Integer> GetUpdatedProdOrCons(
		Map<String, Integer> current, String port, int numTokens
	) {
		if (current.get(port) != null) {
			throw new RuntimeException( // might be allowed tho?
				"Consumption port already exists: " + port
			);
		} else {
			current = new LinkedHashMap<String, Integer>(current);
			current.put(port, numTokens);
		}

		return current;
	}

	/**
	 * Add software instrumentation to the given actor.
	 * @param actorName Name of the actor (must exist)
	 * @param instrs Software instructions.
	 * @param sizeInBits Implementation size in bits.
	 */
	public void InstrumentSoftware(
		String actorName, Map<String, Long> instrs, long sizeInBits
	) { //TODO update the instrumentation
		var actor = GetActor(actorName);
		var sw = InstrumentedSoftwareBehaviour.enforce(
			sGraph, actor.getViewedVertex()
		);
		sw.computationalRequirements(
			Map.of(Requirements.SW_INSTRUCTIONS, instrs)
		);
		sw.maxSizeInBits(Map.of("codeSize", sizeInBits));
	}

	/**
	 * Add hardware instrumentation to the given actor.
	 * @param actorName Name of the actor (must exist)
	 * @param instrs Hardware instructions.
	 * @param extraReqs Extra requirements, i.e. area etc.
	 * @throws RuntimeException if the logic area is not specified
	 */
	public void InstrumentHardware(
		String actorName, Map<String, Long> instrs, Map<String, Long> extraReqs
	) {
		if (!extraReqs.containsKey(Requirements.LOGIC_AREA)) {
			throw new RuntimeException("Logic area must be specified.");
		}

		var actor = GetActor(actorName);
		var hw = InstrumentedHardwareBehaviour.enforce(
			sGraph, actor.getViewedVertex()
		);
		hw.resourceRequirements(
			Map.of(
				Requirements.HW_INSTRUCTIONS, instrs,
				Requirements.EXTRA_REQUREMENTS, extraReqs
			)
		);
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
		var chan = SDFChannel.enforce(
			sGraph, sGraph.newVertex(chanName)
		);
		this.greyBox.addContained(Visualizable.enforce(chan));
		this.viewers.put(chanName, chan);
		chan.numInitialTokens(0);
		chan.producer(srcActor);
		chan.consumer(dstActor);
		
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

