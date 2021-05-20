import buildingBlocks.MyController;
import validation.ValidationBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class ParseLasts {

    private static final File dir = new File("/Users/federicopigozzi/Downloads/all_results/");

    public static void main(String[] args) throws IOException {
        List<File> bestFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getPath().contains("best")) {
                bestFiles.add(file);
            }
        }
        for (File file : bestFiles) {
            String fileName = file.getPath();
            int seed = Integer.parseInt(fileName.split("\\.")[1]);
            Random random = new Random(seed);
            String shape = fileName.split("\\.")[2];
            ValidationBuilder validationBuilder = new ValidationBuilder(shape, "fixed", "rewiring");
            VisualizeBrain.writeBrainToFile((MyController) validationBuilder.parseIndividualFromFile(fileName, random),
                    "./lasts/" + fileName.replace("best", "last").split("/")[fileName.split("/").length - 1]);
        }
    }

}
