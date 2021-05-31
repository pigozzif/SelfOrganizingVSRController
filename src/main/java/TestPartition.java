import buildingBlocks.MyController;
import geneticOps.TopologicalMutation;
import org.apache.commons.math3.util.Pair;
import validation.ValidationBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class TestPartition {

    private static final File dir = new File("/Users/federicopigozzi/Downloads/mega_experiment/");

    public static void main(String[] args) throws IOException, InterruptedException {
        List<File> bestFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getPath().contains("false") && file.getPath().contains("1500") && file.getPath().contains("biped-4x3")) {
                bestFiles.add(file);
            }
        }
        for (File file : bestFiles) {
            String path = file.getPath();
            Random random = new Random(Integer.parseInt(path.split("\\.")[1]));
            ValidationBuilder validationBuilder = new ValidationBuilder("biped-4x3", "fixed", "rewiring");
            MyController controller = (MyController) validationBuilder.parseIndividualFromFile(path, 1500, random);
            List<Pair<Integer, Integer>> module = TopologicalMutation.selectBestModule(controller, 2, 5);
            int numDirs = path.split("/").length;
            String intermediateFileName = path.split("/")[numDirs - 1].replace("csv", "txt");
            String outputFileName = "./brain_visualizations/brain." + intermediateFileName.replace("txt", "png");
            try {
                VisualizeBrain.writeBrainToFile(controller, intermediateFileName);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            Process p = Runtime.getRuntime().exec("python python/visualize_brain.py " + intermediateFileName + " " + outputFileName);
            p.waitFor();
            System.out.println(path);
            System.out.println(module);
        }
    }

}
