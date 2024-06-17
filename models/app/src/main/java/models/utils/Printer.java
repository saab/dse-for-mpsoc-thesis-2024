package models.utils;

import forsyde.io.core.ModelHandler;
import forsyde.io.core.SystemGraph;
import forsyde.io.lib.hierarchy.ForSyDeHierarchy;
import forsyde.io.visual.kgt.drivers.KGTDriver;


/**
 * Class for managing reads and writes of files in the file system.
 */
public class Printer {
    public static final String FIODL_EXT = ".fiodl";
    public static final String KGT_EXT = ".kgt";

    private String filePath;
    private String fileName;
    private String fileDir;

    /**
     * Create a new instance of the Printer.
     * @param filePath The path to the file to use.
     */
    public Printer(String filePath) {
        this.filePath = filePath;
        SetFileName();
        SetFileDir();
    }

    private static ModelHandler handler = new ModelHandler()
            .registerTraitHierarchy(new ForSyDeHierarchy())
            .registerDriver(new KGTDriver());

    /**
     * Identify the containing folder of file in <filePath>.
     */
    private void SetFileDir() {
        this.fileDir = filePath.substring(0, filePath.lastIndexOf('/'));
    }

    /**
     * Identify the file name from the given <filePath>.
     */
    private void SetFileName() {
        this.fileName = filePath.substring(
            filePath.lastIndexOf('/') + 1, filePath.lastIndexOf('.')
        );
    }

    /**
     * Set where the file should be saved to.
     * @param outDir
     */
    public void SetOutDir(String outDir) {
        this.fileDir = outDir;
    }

    /**
     * Append text to the file name.
     * @param append
     */
    public void AppendToFileName(String append) {
        this.fileName += append;
    }

    /**
     * Write the SystemGraph as a .fiodl file.
     * @param g
     * @throws Exception
     */
    public void PrintFIODL(SystemGraph g) throws Exception {
        String outPath = fileDir + "/" + fileName + FIODL_EXT;
        handler.writeModel(g, outPath);
        System.out.println(
            "Design model '" + fileName + "' written to '" + outPath + "'"
        );
    }

    /**
     * Write the SystemGraph as a .kgt file.
     */
    public void PrintKGT(SystemGraph g) throws Exception {
        String outPath = fileDir + "/" + fileName + KGT_EXT;
        handler.writeModel(g, outPath);
        System.out.println(
            "Visualization of '" + fileName + "' model written to '" + outPath + "'"
        );
    }

    /**
     * Read the system specification <filePath> from the file system.
     * @return The specification converted to a SystemGraph.
     * @throws Exception If the file can't be read.
     */
    public SystemGraph Read() throws Exception {
        return handler.loadModel(filePath);
    }
}
