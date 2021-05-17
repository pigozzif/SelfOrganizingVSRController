import buildingBlocks.MyController;
import geneticOps.TopologicalMutation;
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

    private static final File dir = new File("/Users/federicopigozzi/Downloads/biped_very_long_fat_modular/");
    private static final String shape = "biped-4x3";
    private static final String conf = "modular";
    private static final String exp = "second";
    //private static final String sensorConfig = "spinedTouchSighted-f-f-0.01";

    public static void main(String[] args) throws IOException {
        Map<Integer, File> bestFiles = new HashMap<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getPath().contains("validation") && file.getPath().contains(exp)) {
                bestFiles.put(Integer.parseInt(file.getPath().split("\\.")[1]), file);
            }
        }
        //Grid<Boolean> booleanGrid = RobotUtils.buildShape(shape);
        //Grid<? extends SensingVoxel> body = sensorsFactory(sensorConfig).apply(booleanGrid);
        ValidationBuilder validationBuilder = new ValidationBuilder(shape, "fixed", "rewiring");
        BufferedWriter writer = new BufferedWriter(new FileWriter("cut.csv", true));

        for (Map.Entry<Integer, File> entry : bestFiles.entrySet()) {
            int seed = entry.getKey();
            Random random = new Random(seed);
            String validationFile = entry.getValue().getPath();
            String receiverFile = entry.getValue().getPath().replace("." + entry.getKey(), "." + ((seed == 4) ? 0 : seed + 1)).replace("validation", "best").replace("." + exp, "");
            String donatorFile = entry.getValue().getPath().replace("validation", "best").replace("." + exp, "");;
            MyController receiverController = (MyController) validationBuilder.parseIndividualFromFile(receiverFile, random);
            MyController donatorController = (MyController) validationBuilder.parseIndividualFromFile(donatorFile, random);
            //MyController hybrid = validationBuilder.buildValidation(receiverFile, donatorFile, body, random);

            //String val = file1.replace("best", "validation");
            double totalEdgeWeights = receiverController.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum() + donatorController.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum();
            double totalEdges = receiverController.getEdgeSet().size() + donatorController.getEdgeSet().size();
            //Grid<Boolean> grid = validationBuilder.toBeCut(body);
            double percWeights = (countCrossingEdgesWeight(receiverController) + countCrossingEdgesWeight(donatorController)) / totalEdgeWeights;
            double percConnections = (countCrossingConnections(receiverController) + countCrossingConnections(donatorController)) / totalEdges;
            writer.write(String.join(",", String.valueOf(percConnections), String.valueOf(percWeights),
                    String.valueOf(getLastMeanPerformance(receiverFile) - getLastMeanPerformance(validationFile)), shape, conf, exp));
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

    private static double countCrossingEdgesWeight(MyController controller) {//, Grid<Boolean> booleanGrid) {
        double sum = 0.0;
        for (MyController.Edge edge : controller.getEdgeSet()) {
            MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
            MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
            //boolean whatAboutSource = booleanGrid.get(source.getX(), source.getY());
            //boolean whatAboutTarget = booleanGrid.get(target.getX(), target.getY());
            //if ((whatAboutSource && !whatAboutTarget) || (!whatAboutSource && whatAboutTarget)) {
            if (!TopologicalMutation.isNotCrossingEdge(shape, source, target)) {
                sum += Math.abs(edge.getParams()[0]) + Math.abs(edge.getParams()[1]);
            }
        }
        return sum;
    }

    private static double countCrossingConnections(MyController controller) {
        double sum = 0.0;
        for (MyController.Edge edge : controller.getEdgeSet()) {
            MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
            MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
            if (!TopologicalMutation.isNotCrossingEdge(shape, source, target)) {
                ++sum;
            }
        }
        return sum;
    }

}
