package models.utils;

import forsyde.io.core.ModelHandler;
import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.visual.kgt.drivers.KGTDriver;

public class Printer {
    public static final String FIODL_EXT = ".fiodl";
    public static final String KGT_EXT = ".kgt";

    private static ModelHandler handler = new ModelHandler()
            .registerTraitHierarchy(new ForSyDeHierarchy())
            .registerDriver(new KGTDriver());

    private static String GetExtension(String path) {
        return path.substring(path.lastIndexOf('.'));
    }

    public static String GetFileDir(String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }

    public static String GetFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
    }

    public static void Print(SystemGraph g, String outPath) throws Exception {
        handler.writeModel(g, outPath);

        String fileName = GetFileName(outPath);

        // extract extension from outPath
        String extension = GetExtension(outPath);

        if (extension.equals(FIODL_EXT)) {
            System.out.println(
                "Design model '" + fileName + "' written to '" + outPath + "'"
            );
        } else if (extension.equals(KGT_EXT)) {
            System.out.println(
                "Visualization of '" + fileName + "' model written to '" + outPath + "'"
            );
        } else {
            System.out.println("Unknown file extension: " + extension);
        }
    }

    public static SystemGraph Read(String path) throws Exception {
        var model = handler.loadModel(path);
        return model;
    }
}
