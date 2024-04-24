package models.utils;

import org.antlr.v4.parse.ANTLRParser.optionsSpec_return;

import forsyde.io.core.ModelHandler;
import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.visual.kgt.drivers.KGTDriver;

public class Printer {
    public static final String FIODL_EXT = ".fiodl";
    public static final String KGT_EXT = ".kgt";

    private String filePath;
    private String fileName;
    private String fileDir;
    private String extension;

    public Printer(String filePath) {
        this.filePath = filePath;
        SetFileName();
        SetFileDir();
    }

    private static ModelHandler handler = new ModelHandler()
            .registerTraitHierarchy(new ForSyDeHierarchy())
            .registerDriver(new KGTDriver());

    private void SetFileDir() {
        this.fileDir = filePath.substring(0, filePath.lastIndexOf('/'));
    }

    private void SetFileName() {
        this.fileName = filePath.substring(
            filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('.')
        );
    }

    public void SetOutDir(String outDir) {
        this.fileDir = outDir;
    }

    public void AppendToFileName(String append) {
        this.fileName += append;
    }

    public void PrintFIODL(SystemGraph g) throws Exception {
        String outPath = fileDir + "/" + fileName + FIODL_EXT;
        handler.writeModel(g, outPath);
        System.out.println(
            "Design model '" + fileName + "' written to '" + outPath + "'"
        );
    }

    public void PrintKGT(SystemGraph g) throws Exception {
        String outPath = fileDir + "/" + fileName + KGT_EXT;
        handler.writeModel(g, outPath);
        System.out.println(
            "Visualization of '" + fileName + "' model written to '" + outPath + "'"
        );
    }

    public SystemGraph Read() throws Exception {
        return handler.loadModel(filePath);
    }
}
