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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static it.units.malelab.jgea.core.util.Args.a;


public class VisualizeBrain {

    private static final Logger L = Logger.getLogger(VideoMaker.class.getName());
    private static final String dir = "/Users/federicopigozzi/Downloads/mixed/";
    private static boolean saveBestModule;

    public static void main(String[] args) throws IOException, InterruptedException {
        //get params
        String[] inputFileKind = a(args, "input", null).split("-");
        saveBestModule = Boolean.parseBoolean(a(args, "module", null));
        for (File file : Files.walk(Paths.get(dir)).filter(p -> Files.isRegularFile(p) && Arrays.stream(inputFileKind).allMatch(s -> p.toString().contains(s))).map(Path::toFile).collect(Collectors.toList())) {
            visualizBrainFromFile(file.getPath());
        }
    }

    private static void visualizBrainFromFile(String inputFileName) throws IOException, InterruptedException {
        int numDirs = inputFileName.split("/").length;
        String outputFileName = "./brain_visualizations/brain." + inputFileName.split("/")[numDirs - 1].replace("csv", "png");
        SerializationUtils.Mode mode = SerializationUtils.Mode.valueOf(SerializationUtils.Mode.GZIPPED_JSON.name().toUpperCase());
        //read data
        Reader reader = null;
        List<CSVRecord> records = null;
        try {
            reader = new FileReader(inputFileName);
            CSVParser csvParser = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(reader);
            records = csvParser.getRecords();
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
        Robot<?> target = RobotUtils.buildRobotTransformation("identity", new Random(0))
                .apply(SerializationUtils.deserialize(records.get(records.size() - 1).get("best→solution→serialized"), Robot.class, mode));
        // TODO: maybe add specific column best yes/no
        try {
            writeBrainToFile(target, "./temp_brain.txt");
            if (saveBestModule) {
                writeBestModuleToFile((MyController) target.getController(), "./best_module.txt", inputFileName.split("/")[numDirs - 1].split("\\.")[2]);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        Process p = Runtime.getRuntime().exec("python python/visualize_brain.py " + "./temp_brain.txt" + " " + outputFileName);
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

    public static void writeBestModuleToFile(MyController controller, String outputName, String shape) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputName));
        List<Pair<Integer, Integer>> bestModule = TopologicalMutation.selectBestModule(controller, 2, 5);
        writer.write("x,y,shape\n");
        for (Pair<Integer, Integer> v : bestModule) {
            writer.write(v.getFirst() + "," + v.getSecond() + "," + shape + "\n");
        }
        writer.close();
    }

    private static Map<Integer, Pair<Integer, Integer>> postProcessNeuronPosition(MyController controller) {
        Map<Integer, Pair<Integer, Integer>> res = new HashMap<>();
        Map<Pair<Integer, Integer>, Integer> occurences = new HashMap<>();
        Map<Integer, List<MyController.Edge>> outgoingEdges = controller.getOutgoingEdges();
        for (MyController.Neuron neuron : controller.getNodeSet()) {
            occurences.clear();
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
            if (occurences.containsKey(new Pair<>(neuron.getX(), neuron.getY()))) {
                continue;
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
        }
        return res;
    }

}