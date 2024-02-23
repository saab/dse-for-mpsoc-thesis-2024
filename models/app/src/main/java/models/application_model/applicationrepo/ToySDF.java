
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

    public SystemGraph build() {
        SystemGraph sGraph = new SystemGraph();

        var application = Structure.enforce(sGraph, sGraph.newVertex("Application"));
        var applicationGreyBox = GreyBox.enforce(Visualizable.enforce(application));

        Vertex V_actorA = sGraph.newVertex("Actor_A");
        SDFActorViewer a = SDFActor.enforce(sGraph, V_actorA); 
        sGraph.connect(application, a, EdgeTraits.StructuralContainment);
        applicationGreyBox.addContained(Visualizable.enforce(a));
        // a.consumption(Map.of("s_in", 2));
        // a.production(Map.of("s1", 2));
        // a.addPorts("s_in", "s1");
        // a.getViewedVertex().putProperty("LogicArea", 100); // "worth on FPGA"
        // a.addCombFunctions(); // what is this?

        // var viewer = InstrumentedBehaviour.enforce(sGraph, V_actorA);
        // sGraph.connect(applicationGreyBox, viewer, EdgeTraits.StructuralContainment);
        // applicationGreyBox.addContained(Visualizable.enforce(viewer));
        // viewer.computationalRequirements();

        SDFActorViewer b = SDFActor.enforce(sGraph, sGraph.newVertex("Actor_B"));
        sGraph.connect(application, b, EdgeTraits.StructuralContainment);
        applicationGreyBox.addContained(Visualizable.enforce(b));
        b.consumption(Map.of("s1", 3));
        b.production(Map.of("s_out", 1));
        b.addPorts("s1", "s_out");
        // a.getViewedVertex().putProperty("LogicArea", 10000); // "not worth on FPGA"
        // a.addCombFunctions(); // what is this?

        SDFChannelViewer ab_chan = SDFChannel.enforce(sGraph, sGraph.newVertex("Channel_A_B"));
        sGraph.connect(application, ab_chan, EdgeTraits.StructuralContainment);
        applicationGreyBox.addContained(Visualizable.enforce(ab_chan));
        ab_chan.numInitialTokens(0);

        sGraph.connect(
                a,
                ab_chan,
                EdgeTraits.SDFNetworkEdge,
                EdgeTraits.VisualConnection);
        sGraph.connect(
                ab_chan,
                b,
                EdgeTraits.SDFNetworkEdge,
                EdgeTraits.VisualConnection);

        return sGraph;
    }
}
