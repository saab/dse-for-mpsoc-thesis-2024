
package application.ApplicationRepo;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFActorViewer;
import forsyde.io.lib.hierarchy.behavior.moc.sdf.SDFChannelViewer;

import java.util.Map;


public class ToySDF {
    
    public ToySDF() {

    }

    public SystemGraph build() {
        SystemGraph sGraph = new SystemGraph();
        
        SDFActorViewer a = SDFActor.enforce(sGraph, sGraph.newVertex("Actor A"));
        a.consumption(Map.of("s_in", 2));
        a.production(Map.of("s1", 2));
        a.addPorts("s_in", "s1");
        Visualizable.enforce(a);

        SDFActorViewer b = SDFActor.enforce(sGraph, sGraph.newVertex("Actor B"));
        b.consumption(Map.of("s1", 3));
        b.production(Map.of("s_out", 1));
        b.addPorts("s1", "s_out");
        Visualizable.enforce(b);
        
        SDFChannelViewer ab_chan = SDFChannel.enforce(sGraph, sGraph.newVertex("Channel A-B"));
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
