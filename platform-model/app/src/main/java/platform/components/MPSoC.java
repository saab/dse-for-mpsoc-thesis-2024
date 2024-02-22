
package platform.components;

import java.util.*;

import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy.*;
import forsyde.io.lib.hierarchy.platform.hardware.StructureViewer;
import forsyde.io.lib.hierarchy.platform.hardware.GenericMemoryModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedProcessingModuleViewer;
import forsyde.io.lib.hierarchy.platform.hardware.InstrumentedCommunicationModuleViewer;
import forsyde.io.lib.hierarchy.visualization.GreyBoxViewer;
import platform.utils.Units;

public class MPSoC {
    public SystemGraph sGraph;
    private StructureViewer platform = null;
    private GreyBoxViewer platformGreyBox = null;
    private StructureViewer PS = null;
    private GreyBoxViewer psGreyBox = null;
    private InstrumentedCommunicationModuleViewer OCMSwitch = null;
    private StructureViewer APU = null;
    private StructureViewer RPU = null;
    private Map<InstrumentedProcessingModuleViewer, String> PSCores = new HashMap<>();

    private final String PORT_TO_OCMSWITCH = "portToOCMSwitch";

    public MPSoC() {
        SystemGraph sGraph = new SystemGraph();

        StructureViewer platform = Structure.enforce(
                sGraph, sGraph.newVertex("MPSoC"));
        var platformGreyBox = GreyBox.enforce(Visualizable.enforce(platform));

        StructureViewer PS = Structure.enforce(sGraph, sGraph.newVertex("MPSoC.PS"));
        sGraph.connect(platform, PS, EdgeTraits.StructuralContainment);
        platformGreyBox.addContained(Visualizable.enforce(PS));
        var psGreyBox = GreyBox.enforce(PS);

        this.sGraph = sGraph;
        this.platform = platform;
        this.platformGreyBox = platformGreyBox;
        this.PS = PS;
        this.psGreyBox = psGreyBox;
    }

    public void AddProcessingSystemModule(String name, int cores, long frequency,
            Map<String, Map<String, Double>> modalInstructions) {

        if (this.PS == null) {
            throw new IllegalStateException("Processing system structure is not added yet.");
        }

        String moduleName = "MPSoC.PS." + name;
        StructureViewer module = Structure.enforce(sGraph, sGraph.newVertex(moduleName));
        sGraph.connect(PS, module, EdgeTraits.StructuralContainment);
        GreyBoxViewer box = GreyBox.enforce(Visualizable.enforce(module));

        for (int i = 0; i < cores; i++) {
            var core = InstrumentedProcessingModule.enforce(
                    sGraph, sGraph.newVertex(moduleName + ".C" + i));
            core.maximumComputationParallelism(1);
            core.operatingFrequencyInHertz(frequency);
            core.modalInstructionsPerCycle(modalInstructions);

            core.addPorts(PORT_TO_OCMSWITCH);

            sGraph.connect(module, core, EdgeTraits.StructuralContainment);
            box.addContained(Visualizable.enforce(core));

            this.PSCores.put(core, moduleName);
        }

        this.psGreyBox.addContained(box);
    }

    // Memory: 4GB, 600 MHz
    public void AddOCM(long frequency, long spaceInBits) {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        final String PORT_TO_OCM = "PortToOCM";

        GenericMemoryModuleViewer ocm = GenericMemoryModule.enforce(
                sGraph, sGraph.newVertex("MPSoC.PS.OCM"));
        ocm.operatingFrequencyInHertz(frequency);
        ocm.spaceInBits(spaceInBits);

        ocm.addPorts(PORT_TO_OCMSWITCH);
        sGraph.connect(
            this.OCMSwitch, 
            ocm, 
            PORT_TO_OCM, 
            PORT_TO_OCMSWITCH,
            EdgeTraits.PhysicalConnection,
            EdgeTraits.VisualConnection);
        sGraph.connect(
            ocm, 
            this.OCMSwitch, 
            PORT_TO_OCMSWITCH, 
            PORT_TO_OCM,
            EdgeTraits.PhysicalConnection,
            EdgeTraits.VisualConnection);

        sGraph.connect(this.platform, ocm, EdgeTraits.StructuralContainment);
        this.platformGreyBox.addContained(Visualizable.enforce(ocm));
    }

    public void AddOCMSwitch() {
        if (this.platform == null) {
            throw new IllegalStateException("Platform must be added first");
        }

        InstrumentedCommunicationModuleViewer OCMSwitch = InstrumentedCommunicationModule.enforce(
                sGraph, sGraph.newVertex("MPSoC.PS.OCMSwitch"));
        sGraph.connect(this.platform, OCMSwitch, EdgeTraits.StructuralContainment);
        this.platformGreyBox.addContained(Visualizable.enforce(OCMSwitch));

        // round robin??
        ForSyDeHierarchy.TimeDivisionMultiplexingRuntime.enforce(
                sGraph, sGraph.newVertex("MPSoC.PS.OCMSwitch"));

        OCMSwitch.operatingFrequencyInHertz(600 * Units.MHZ);
        OCMSwitch.initialLatency(0L);
        OCMSwitch.flitSizeInBits(8L); // one flit is 1 byte
        OCMSwitch.maxCyclesPerFlit(1); // cycles to send 1 byte
        OCMSwitch.maxConcurrentFlits(1); // could match ports below with = 4

        OCMSwitch.addPorts("OCMSwitchPort1", "OCMSwitchPort2", "OCMSwitchPort3", "OCMSwitchPort4", "PortToOCM");

        this.PSCores.forEach((core, moduleName) -> {
            sGraph.connect(
                    core,
                    OCMSwitch,
                    PORT_TO_OCMSWITCH,
                    "OCMSwitchPort1",
                    EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);
            sGraph.connect(
                    OCMSwitch,
                    core,
                    "OCMSwitchPort1", // could be any port, Rodolfo: doesn't affect IDeSyDe 
                    PORT_TO_OCMSWITCH,
                    EdgeTraits.PhysicalConnection,
                    EdgeTraits.VisualConnection);

        });

        this.OCMSwitch = OCMSwitch;
    }

    public void AddFPGA() {
        // TODO
    }
}
