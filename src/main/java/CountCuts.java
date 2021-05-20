import buildingBlocks.MyController;
import geneticOps.TopologicalMutation;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import validation.ValidationBuilder;

import java.io.*;
import java.util.*;


public class CountCuts {

    private static final File dir = new File("/Users/federicopigozzi/Downloads/all_results/");
    private static final int windowSize = 1;
    //private static final String shape = "biped-4x3";
    //private static final String conf = "modular";
    //private static final String exp = "second";
    //private static final String sensorConfig = "spinedTouchSighted-f-f-0.01";

    public static void main(String[] args) throws IOException {
        List<File> bestFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getPath().contains("validation")) {
                bestFiles.add(file);
            }
        }
        //Grid<Boolean> booleanGrid = RobotUtils.buildShape(shape);
        //Grid<? extends SensingVoxel> body = sensorsFactory(sensorConfig).apply(booleanGrid);
        BufferedWriter writer = new BufferedWriter(new FileWriter("cut.csv", true));

        for (double valPerc : new double[]{0.001, 0.025, 0.05, 0.1, 0.25, 0.5, 0.75, 0.95}) {
            for (File entry : bestFiles) {
                String validationFile = entry.getPath();
                int seed = Integer.parseInt(validationFile.split("\\.")[1]);
                Random random = new Random(seed);
                String shape = validationFile.split("\\.")[2];
                String conf = validationFile.split("\\.")[3];
                ValidationBuilder validationBuilder = new ValidationBuilder(shape, "fixed", "rewiring");
                String receiverFile = entry.getPath().replace("." + seed, "." + ((seed == 4) ? 0 : seed + 1)).replace("validation", "best");//.replace("." + exp, "");
                String donatorFile = entry.getPath().replace("validation", "best");//.replace("." + exp, "");
                MyController receiverController = (MyController) validationBuilder.parseIndividualFromFile(receiverFile, random);
                MyController donatorController = (MyController) validationBuilder.parseIndividualFromFile(donatorFile, random);
                //MyController hybrid = validationBuilder.buildValidation(receiverFile, donatorFile, body, random);

                //String val = file1.replace("best", "validation");
                double totalEdgeWeights = receiverController.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum() + donatorController.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum();
                double totalEdges = receiverController.getEdgeSet().size() + donatorController.getEdgeSet().size();
                //Grid<Boolean> grid = validationBuilder.toBeCut(body);
                double percWeights = (countCrossingEdgesWeight(receiverController, shape) + countCrossingEdgesWeight(donatorController, shape)) / totalEdgeWeights;
                double percConnections = (countCrossingConnections(receiverController, shape) + countCrossingConnections(donatorController, shape)) / totalEdges;
                double perfBefore = getLastMedianPerformance(receiverFile, valPerc, false);
                double percBeforeDonator = getLastMedianPerformance(donatorFile, valPerc, false);
                double perfAfter = getLastMedianPerformance(validationFile, valPerc, true);
                writer.write(String.join(",", String.valueOf(percConnections), String.valueOf(percWeights),
                        String.valueOf(percBeforeDonator), String.valueOf(perfBefore), String.valueOf(perfAfter), String.valueOf(perfAfter - perfBefore), shape, conf, String.valueOf(valPerc)));
                writer.write("\n");
            }
        }

        writer.close();
    }

    private static double getLastMedianPerformance(String fileName, double valPerc, boolean isValidation) {
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
        int at = (isValidation) ? (int) (records.size() * valPerc) : records.size() - windowSize;
        double[] vals = records.stream().skip(at).limit(windowSize).mapToDouble(r -> Double.parseDouble(r.get("best→fitness→as[Outcome]→velocity"))).toArray();
        if (vals.length == 1) {
            return vals[0];
        }
        Arrays.sort(vals);
        return (vals[5] + vals[4]) / 2.0;
    }

    private static double countCrossingEdgesWeight(MyController controller, String shape) {//, Grid<Boolean> booleanGrid) {
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

    private static double countCrossingConnections(MyController controller, String shape) {
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
