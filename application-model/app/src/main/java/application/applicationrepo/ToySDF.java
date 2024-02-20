
package application.applicationrepo;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.behavior.FunctionLikeEntity;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFActorViewer;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFChannelViewer;

import java.util.Map;


public class ToySDF {
    
    public ToySDF() {

    }

    public SystemGraph build() {
        SystemGraph sGraph = new SystemGraph();
        
        SDFActorViewer a = SDFActor.enforce(sGraph, sGraph.newVertex("Actor_A"));
        a.consumption(Map.of("s_in", 2));
        a.production(Map.of("s1", 2));
        a.addPorts("s_in", "s1");
        a.getViewedVertex().putProperty("LogicArea", 100); // "worth on FPGA"
        // a.addCombFunctions(); // what is this?
        Visualizable.enforce(a);

        SDFActorViewer b = SDFActor.enforce(sGraph, sGraph.newVertex("Actor_B"));
        b.consumption(Map.of("s1", 3));
        b.production(Map.of("s_out", 1));
        b.addPorts("s1", "s_out");
        a.getViewedVertex().putProperty("LogicArea", 10000); // "not worth on FPGA"
        // a.addCombFunctions(); // what is this?
        Visualizable.enforce(b);
        
        SDFChannelViewer ab_chan = SDFChannel.enforce(sGraph, sGraph.newVertex("Channel_A_B"));
        ab_chan.numInitialTokens(0);
        sGraph.connect(
            a, 
            ab_chan,
            EdgeTraits.SDFNetworkEdge, 
            EdgeTraits.VisualConnection
        );
        sGraph.connect(
            ab_chan,
            b, 
            EdgeTraits.SDFNetworkEdge, 
            EdgeTraits.VisualConnection
        );
        Visualizable.enforce(ab_chan);

        return sGraph;
    }
}
