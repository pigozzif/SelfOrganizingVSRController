import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import validation.ValidationBuilder;

import java.io.*;
import java.util.*;

import static morphologies.Morphology.sensorsFactory;


public class CountCuts {

    private static final File dir = new File("./output/biped_short_mixed/");
    private static final String shape = "biped-4x3";
    private static final String exp = "minimal";
    private static final String isMixed = "true";
    private static final String sensorConfig = "spinedTouchSighted-f-f-0.01";

    public static void main(String[] args) throws IOException {
        Map<Integer, File> bestFiles = new HashMap<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getPath().contains("best")) {
                bestFiles.put(Integer.parseInt(file.getPath().split("\\.")[2]), file);
            }
        }
        Grid<Boolean> booleanGrid = RobotUtils.buildShape(shape);
        Grid<? extends SensingVoxel> body = sensorsFactory(sensorConfig).apply(booleanGrid);
        ValidationBuilder validationBuilder = new ValidationBuilder("fixed", "rewiring");
        BufferedWriter writer = new BufferedWriter(new FileWriter("cut.csv", true));

        for (Map.Entry<Integer, File> entry : bestFiles.entrySet()) {
            int seed = entry.getKey();
            Random random = new Random(seed);
            String file1 = entry.getValue().getPath();
            String file2 = entry.getValue().getPath().replace("." + entry.getKey(), "." + ((seed == 4) ? 0 : seed + 1));
            MyController controller1 = (MyController) validationBuilder.parseIndividualFromFile(file1, random);
            MyController controller2 = (MyController) validationBuilder.parseIndividualFromFile(file2, random);
            //MyController hybrid = validationBuilder.buildValidation(file1, file2, body, random);

            String val = file1.replace("best", "validation");
            double totalEdges = controller1.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum() + controller2.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum();
            Grid<Boolean> grid = validationBuilder.toBeCut(body);
            writer.write(String.join(",", String.valueOf((countCrossingEdgesWeight(controller1, grid) + countCrossingEdgesWeight(controller2, grid)) / totalEdges),
                    String.valueOf(getLastMeanPerformance(file1)), String.valueOf(getLastMeanPerformance(val)), shape, exp, isMixed));
            writer.write("\n");
        }

        writer.close();
    }

    private static double getLastMeanPerformance(String fileName) {
        List<CSVRecord> records;
        List<String> headers;
        try {
            FileReader reader = new FileReader(fileName);
            CSVParser csvParser = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(reader);
            records = csvParser.getRecords();
            headers = csvParser.getHeaderNames();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Cannot read file: %s", fileName));
        }
        if (!headers.contains("best→solution→serialized")) {
            throw new RuntimeException(String.format("input file %s does not contain serialization column", fileName));
        }
        return records.stream().skip(records.size() - 10).mapToDouble(r -> Double.parseDouble(r.get("best→fitness→as[Outcome]→velocity"))).average().getAsDouble();
    }

    private static double countCrossingEdgesWeight(MyController controller, Grid<Boolean> booleanGrid) {
        double sum = 0.0;
        for (MyController.Edge edge : controller.getEdgeSet()) {
            MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
            MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
            boolean whatAboutSource = booleanGrid.get(source.getX(), source.getY());
            boolean whatAboutTarget = booleanGrid.get(target.getX(), target.getY());
            if ((whatAboutSource && !whatAboutTarget) || (!whatAboutSource && whatAboutTarget)) {
                sum += Math.abs(edge.getParams()[0]) + Math.abs(edge.getParams()[1]);
            }
        }
        return sum;
    }

}