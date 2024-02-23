package models.application_model;

import forsyde.io.core.SystemGraph;
import models.application_model.applicationrepo.ToySDF;

public class ApplicationHandler {

    public static SystemGraph ToySDFGraph() {
        ToySDF toySDF = new ToySDF();
        SystemGraph sGraph = toySDF.build();
        return sGraph;
    }

}
