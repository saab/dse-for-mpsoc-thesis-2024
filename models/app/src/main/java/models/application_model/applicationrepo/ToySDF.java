
package models.application_model.applicationrepo;

import forsyde.io.core.SystemGraph;
import forsyde.io.core.Vertex;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFActorViewer;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFChannelViewer;
// import forsyde.io.lib.hierarchy.implementation.functional.InstrumentedBehaviourViewer;

import java.util.Map;

public class ToySDF {

    public ToySDF() {
    }

    public SystemGraph Build() {
        SystemGraph sGraph = new SystemGraph();

        var application = Structure.enforce(sGraph, sGraph.newVertex("Application"));
        var applicationGreyBox = GreyBox.enforce(Visualizable.enforce(application));

        Vertex V_actorA = sGraph.newVertex("Actor A");
        SDFActorViewer actorA = SDFActor.enforce(sGraph, V_actorA);
        sGraph.connect(application, actorA, EdgeTraits.StructuralContainment);
        applicationGreyBox.addContained(Visualizable.enforce(actorA));
        actorA.consumption(Map.of("s_in", 2));
        actorA.production(Map.of("s1", 2));
        actorA.addPorts("s_in", "s1");
        // a.getViewedVertex().putProperty("LogicArea", 100); // "worth on FPGA"
        var instrumentedActorA = InstrumentedBehaviour.enforce(sGraph, V_actorA);
        // applicationGreyBox.addContained(Visualizable.enforce(instrumentedActorA));
        instrumentedActorA.computationalRequirements(Map.of(
                "default", Map.of(
                        "FloatOp", 1000L)));
        instrumentedActorA.maxSizeInBits(Map.of("default", 32L));

        Vertex V_actorB = sGraph.newVertex("Actor_B");
        SDFActorViewer actorB = SDFActor.enforce(sGraph, V_actorB);
        sGraph.connect(application, actorB, EdgeTraits.StructuralContainment);
        applicationGreyBox.addContained(Visualizable.enforce(actorB));
        actorB.consumption(Map.of("s1", 3));
        actorB.production(Map.of("s_out", 1));
        actorB.addPorts("s1", "s_out");
        // a.getViewedVertex().putProperty("LogicArea", 10000); // "not worth on FPGA"
        var instrumentedActorB = InstrumentedBehaviour.enforce(sGraph, V_actorB);
        applicationGreyBox.addContained(Visualizable.enforce(instrumentedActorB));
        instrumentedActorB.computationalRequirements(Map.of(
                "default", Map.of(
                        "FloatOp", 100L)));
        instrumentedActorB.maxSizeInBits(Map.of("default", 32L));

        SDFChannelViewer ab_chan = SDFChannel.enforce(sGraph, sGraph.newVertex("Channel_A_B"));
        sGraph.connect(application, ab_chan, EdgeTraits.StructuralContainment);
        applicationGreyBox.addContained(Visualizable.enforce(ab_chan));
        ab_chan.numInitialTokens(0);
        ab_chan.producer(actorA);
        ab_chan.consumer(actorB);

        sGraph.connect(
                actorA,
                ab_chan,
                // "s1",
                // "consumer",
                EdgeTraits.SDFNetworkEdge,
                EdgeTraits.VisualConnection);
        sGraph.connect(
                ab_chan,
                actorB,
                "producer",
                "s1",
                EdgeTraits.SDFNetworkEdge,
                EdgeTraits.VisualConnection);

        return sGraph;
    }
}
