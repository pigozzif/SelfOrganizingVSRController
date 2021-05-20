import buildingBlocks.MyController;
import geneticOps.TopologicalMutation;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.util.Pair;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static it.units.malelab.jgea.core.util.Args.a;


public class VisualizeBrain {

    private static final Logger L = Logger.getLogger(VideoMaker.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        //get params
        String inputFileName = a(args, "input", null);
        int numDirs = inputFileName.split("/").length;
        String intermediateFileName = a(args, "output", inputFileName.split("/")[numDirs - 1].replace("csv", "txt"));
        String outputFileName = "./brain_visualizations/brain." + inputFileName.split("/")[numDirs - 1].replace("csv", "png");
        String serializedRobotColumn = a(args, "serializedRobotColumnName", "serialized");
        String transformationName = a(args, "transformation", "identity");
        SerializationUtils.Mode mode = SerializationUtils.Mode.valueOf(a(args, "deserializationMode", SerializationUtils.Mode.GZIPPED_JSON.name()).toUpperCase());
        //read data
        Reader reader = null;
        List<CSVRecord> records = null;
        List<String> headers = null;
        try {
            reader = new FileReader(inputFileName);
            CSVParser csvParser = CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader().parse(reader);
            records = csvParser.getRecords();
            headers = csvParser.getHeaderNames();
            reader.close();
        } catch (IOException e) {
            L.severe(String.format("Cannot read input data: %s", e));
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioException) {
                //ignore
                }
            }
            System.exit(-1);
        }
        if (records.size() != 1) {
            L.severe(String.format("Found more than one row in %s", inputFileName));
        }
        if (!headers.contains(serializedRobotColumn)) {
            L.severe(String.format("Cannot find serialized robot column %s in %s", serializedRobotColumn, headers));
            System.exit(-1);
        }
        //build grid
        Robot<?> target = RobotUtils.buildRobotTransformation(transformationName, new Random(0))
                .apply(SerializationUtils.deserialize(records.get(0).get(serializedRobotColumn), Robot.class, mode));
        // TODO: maybe add specific column best yes/no
        try {
            writeBrainToFile(target, intermediateFileName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        Process p = Runtime.getRuntime().exec("python python/visualize_brain.py " + intermediateFileName + " " + outputFileName);
        p.waitFor();
    }

    public static void writeBrainToFile(Robot<?> robot, String outputName) throws IOException {
        writeBrainToFile((MyController) robot.getController(), outputName);
    }

    public static void writeBrainToFile(MyController controller, String outputName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputName));
        TopologicalMutation.pruneIsolatedNeurons(controller);
        writer.write("index,x,y,function,edges,type\n");
        Map<Integer, Pair<Integer, Integer>> occ = postProcessNeuronPosition(controller);
        controller.getNodeSet().forEach(n -> {
            try {
                String[] line = n.toString().split(",");
                if (occ.containsKey(n.getIndex())) {
                    line[1] = String.valueOf(occ.get(n.getIndex()).getFirst());
                    line[2] = String.valueOf(occ.get(n.getIndex()).getSecond());
                }
                writer.write(String.join(",", line) + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
    }

    private static Map<Integer, Pair<Integer, Integer>> postProcessNeuronPosition(MyController controller) {
        Map<Integer, Pair<Integer, Integer>> res = new HashMap<>();
        Map<Pair<Integer, Integer>, Integer> occurences = new HashMap<>();
        Map<Integer, List<MyController.Edge>> outgoingEdges = controller.getOutgoingEdges();
        for (MyController.Neuron neuron : controller.getNodeSet()) {
            if (!neuron.isHidden()) {
                continue;
            }
            for (MyController.Edge edge : neuron.getIngoingEdges()) {
                MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
                Pair<Integer, Integer> pos = new Pair<>(source.getX(), source.getY());
                if (occurences.containsKey(pos)) {
                    occurences.put(pos, occurences.get(pos) + 1);
                }
                else {
                    occurences.put(pos, 1);
                }
            }
            if (outgoingEdges.containsKey(neuron.getIndex())) {
                for (MyController.Edge edge : outgoingEdges.get(neuron.getIndex())) {
                    MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
                    Pair<Integer, Integer> pos = new Pair<>(target.getX(), target.getY());
                    if (occurences.containsKey(pos)) {
                        occurences.put(pos, occurences.get(pos) + 1);
                    } else {
                        occurences.put(pos, 1);
                    }
                }
            }
            Map.Entry<Pair<Integer, Integer>,Integer> maxEntry = null;
            for (Map.Entry<Pair<Integer, Integer>, Integer> entry : occurences.entrySet()) {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                    maxEntry = entry;
                }
            }
            if (maxEntry != null) {
                Random random = new Random(0);
                Map.Entry<Pair<Integer, Integer>, Integer> finalMaxEntry = maxEntry;
                List<Pair<Integer, Integer>> candidates = occurences.entrySet().stream().filter(e -> e.getValue().equals(finalMaxEntry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
                res.put(neuron.getIndex(), candidates.get(random.nextInt(candidates.size())));
            }
            occurences.clear();
        }
        return res;
    }

}