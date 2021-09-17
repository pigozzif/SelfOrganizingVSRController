
import buildingBlocks.MyController;
import geneticOps.TopologicalMutation;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import validation.ValidationBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class CountCuts {

    private static final String dir = "/Users/federicopigozzi/Downloads/all_results/";
    private static final int windowSize = 1;

    public static void main(String[] args) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("cuts.csv", false));
        writer.write(String.join(";", "perc_connections", "perc_weights", "sum_connections", "sum_weights", "performance_before_donator", "performance_before_receiver",
                "performance_after", "shape", "configuration", "time") + "\n");

        for (File entry : Files.walk(Paths.get(dir)).filter(p -> Files.isRegularFile(p) && p.toString().contains("transfer") && !p.toString().contains("validation") && !p.toString().contains("segregated")).map(Path::toFile).collect(Collectors.toList())) {
            String transferPath = entry.getPath();
            int seed = Integer.parseInt(transferPath.split("\\.")[1]);
            Random random = new Random(seed);
            String shape = transferPath.split("\\.")[2];
            String conf = transferPath.split("\\.")[3];
            ValidationBuilder validationBuilder = new ValidationBuilder(shape, "fixed", "rewiring");
            int receiverSeed = getReceiverSeed(seed);
            String receiverFile = entry.getPath().replace("." + seed, "." + receiverSeed).replace("transfer", "best");
            String donatorFile = entry.getPath().replace("transfer", "best");
            MyController receiverController = (MyController) validationBuilder.parseIndividualFromFile(receiverFile, random);
            MyController donatorController = (MyController) validationBuilder.parseIndividualFromFile(donatorFile, random);

            double totalEdgeWeights = receiverController.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum() + donatorController.getEdgeSet().stream().mapToDouble(e -> Math.abs(e.getParams()[0]) + Math.abs(e.getParams()[1])).sum();
            double totalEdges = receiverController.getEdgeSet().size() + donatorController.getEdgeSet().size();

            double[] dataReceiver = getEdgeData(receiverController, shape);
            double[] dataDonator = getEdgeData(donatorController, shape);
            double percWeights = (dataReceiver[0] + dataDonator[0]) / totalEdgeWeights;
            double percConnections = (dataReceiver[1] + dataDonator[1]) / totalEdges;
            double perfBeforeReceiver = getLastMedianPerformance(receiverFile);
            double percBeforeDonator = getLastMedianPerformance(donatorFile);
            for (double valPerc : new double[]{0.001, 0.025, 0.05, 0.1, 0.25, 0.5, 0.75, 0.95}) {
                double perfAfter = getLastMedianPerformance(transferPath, valPerc, true);
                writer.write(String.join(";", String.valueOf(percConnections), String.valueOf(percWeights),
                            String.valueOf(dataDonator[1] + dataReceiver[1]), String.valueOf(dataDonator[0] + dataReceiver[0]),
                            String.valueOf(percBeforeDonator), String.valueOf(perfBeforeReceiver), String.valueOf(perfAfter), shape, conf, String.valueOf(valPerc)) + "\n");
            }
        }

        writer.close();
    }

    private static double getLastMedianPerformance(String fileName) {
        return getLastMedianPerformance(fileName, -1.0, false);
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

    private static double[] getEdgeData(MyController controller, String shape) {
        double[] ans = new double[] {0.0, 0.0};
        for (MyController.Edge edge : controller.getEdgeSet()) {
            MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
            MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
            if (!TopologicalMutation.isNotCrossingEdge(shape, source, target)) {
                ans[0] += Math.abs(edge.getParams()[0]) + Math.abs(edge.getParams()[1]);
                ans[1] += 1;
            }
        }
        return ans;
    }

    private static int getReceiverSeed(int seed) {
        if (seed == 4) {
            return 0;
        }
        else if (seed == 14) {
            return 10;
        }
        else {
            return seed + 1;
        }
    }

}
