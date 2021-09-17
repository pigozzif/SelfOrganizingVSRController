import buildingBlocks.MyController;
import geneticOps.TopologicalMutation;
import org.apache.commons.math3.util.Pair;
import validation.ValidationBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;


public class TestPartition {

    private static final String dir = "/Users/federicopigozzi/Downloads/mega_experiment/";

    public static void main(String[] args) throws IOException, InterruptedException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("modules.csv", false));
        writer.write("cross;non_cross;shape;is_best\n");
        for (File file : Files.walk(Paths.get(dir)).filter(p -> Files.isRegularFile(p) && p.toString().contains("false") && p.toString().contains("1500")).map(Path::toFile).collect(Collectors.toList())) {
            String path = file.getPath();
            String shape = path.split("\\.")[2];
            Random random = new Random(Integer.parseInt(path.split("\\.")[1]));
            ValidationBuilder validationBuilder = new ValidationBuilder(shape, "fixed", "rewiring");
            MyController controller = (MyController) validationBuilder.parseIndividualFromFile(path, 1500, random);
            List<List<Pair<Integer, Integer>>> modules = TopologicalMutation.enumeratePossibleModules(controller, 2, 5);
            List<Pair<Integer, Integer>> bestModule = TopologicalMutation.selectBestModule(controller, 2, 5);
            System.out.println(path);
            System.out.println(bestModule);
            for (List<Pair<Integer, Integer>> module : modules) {
                writer.write(String.join(";", String.valueOf(TopologicalMutation.countCrossingEdgesWeight(module, controller)),
                        String.valueOf(TopologicalMutation.countNotCrossingEdgesWeight(module, controller)), shape, (module.equals(bestModule)) ? "1" : "0") + "\n");
            }
            /*int numDirs = path.split("/").length;
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
            System.out.println(module);*/
        }
        writer.close();
    }

}
