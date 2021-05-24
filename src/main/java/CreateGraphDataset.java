import buildingBlocks.MyController;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import validation.ValidationBuilder;

import java.io.*;
import java.util.*;


public class CreateGraphDataset {

    private static final File dir = new File("/Users/federicopigozzi/Downloads/all_results/");
    private static final int numGraphs = 10000;
    private static final int numIterations = 3125;

    public static void main(String[] args) throws IOException {
        List<File> bestFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getPath().contains("best")) {
                bestFiles.add(file);
            }
        }
        int numPerFile = numGraphs / (bestFiles.size());
        List<CSVRecord> records;
        for (File file : bestFiles) {
            String fileName = file.getPath();
            int seed = Integer.parseInt(fileName.split("\\.")[1]);
            Random random = new Random(seed);
            ValidationBuilder validationBuilder = new ValidationBuilder(fileName.split("\\.")[2], "fixed", "rewiring");
            int[] indexes = random.ints(0, numIterations).limit(numPerFile).toArray();
            FileReader reader = new FileReader(fileName);
            CSVParser csvParser = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(reader);
            records = csvParser.getRecords();
            for (int idx : indexes) {
                String name = "./dataset/" + fileName.replace("best", idx + "." + records.get(idx).get("best→fitness→as[Outcome]→velocity")).split("/")[fileName.split("/").length - 1];
                VisualizeBrain.writeBrainToFile((MyController) validationBuilder.parseIndividualFromFile(fileName, random), name);
            }
            reader.close();
            records.clear();
        }
    }

}
