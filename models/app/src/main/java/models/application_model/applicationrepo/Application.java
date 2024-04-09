
package models.application_model.applicationrepo;

import java.util.*;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.VertexViewer;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
// import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFChannelViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;



public class Application {
	private SystemGraph sGraph;
    private GreyBoxViewer greyBox;
    public Map<String, VertexViewer> viewers = new HashMap<>();


	public Application(String name) {
		SystemGraph sGraph = new SystemGraph();

		var application = Structure.enforce(sGraph, sGraph.newVertex(name));
		this.viewers.put(name, application);
		this.greyBox = GreyBox.enforce(Visualizable.enforce(application));
		this.sGraph = sGraph;
	}

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
	public void AddActor(
		String name, Map<String, Integer> prod, Map<String, Integer> cons,
		Map<String, Map<String, Long>> instrs
	) {
		var v = sGraph.newVertex(name);
		var a = SDFActor.enforce(sGraph, v);
		this.greyBox.addContained(Visualizable.enforce(a));
		this.viewers.put(name, a);
		a.production(prod);
		a.consumption(cons);

		var instrA = InstrumentedBehaviour.enforce(sGraph, v);
		instrA.computationalRequirements(instrs);
		instrA.maxSizeInBits(Map.of("default", 32L)); //? size of sw code?

	}

	private void Connect(
		VertexViewer a, VertexViewer b, String aToBPort, String bToAPort
	) {
		sGraph.connect(
			a, b, aToBPort, bToAPort,
			EdgeTraits.SDFNetworkEdge,
			EdgeTraits.VisualConnection
		);
	}

	/**
	 * Create an SDF channel between two actors.
	 * @param srcActorName Name of the first actor
	 * @param dstActorName Name of the second actor
	 */
	public void CreateChannel(String srcActorName, String dstActorName) {
		String chanName = srcActorName + "_" + dstActorName;
		var chan = SDFChannel.enforce(
			sGraph, sGraph.newVertex(chanName)
		);
		this.greyBox.addContained(Visualizable.enforce(chan));
		
		var srcActor = SDFActor.tryView(viewers.get(srcActorName))
			.orElseThrow(() -> 
				new RuntimeException("Actor not found: " + srcActorName)
			);
		var dstActor = SDFActor.tryView(viewers.get(dstActorName))
			.orElseThrow(() -> 
				new RuntimeException("Actor not found: " + dstActorName)
			);
		chan.numInitialTokens(0);
		chan.producer(srcActor);
		chan.consumer(dstActor);

		String srcToChan = srcActorName + "_to_" + chanName;
		String chanToSrc = chanName + "_to_" + srcActorName;
		String dstToChan = dstActorName + "_to_" + chanName;
		String chanToDst = chanName + "_to_" + dstActorName;
		
		this.Connect(srcActor, chan, srcToChan, chanToSrc);
		this.Connect(chan, dstActor, chanToDst, dstToChan);
	}

	public void AddHWImplementation(String actorName) {
		var actor = SDFActor.tryView(viewers.get(actorName))
			.orElseThrow(() -> 
				new RuntimeException("Actor not found: " + actorName)
			);
		actor.implementation(implName);
	}

	public SystemGraph Build() {
		// sGraph.connect(application, srcActor, EdgeTraits.StructuralContainment);
		// srcActor.addPorts("s_in", "s1");
		// a.getViewedVertex().putProperty("LogicArea", 100); // "worth on FPGA"

		// sGraph.connect(application, chan, EdgeTraits.StructuralContainment);

		

		return sGraph;
	}
}
