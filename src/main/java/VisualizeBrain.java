import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

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

    private static void writeBrainToFile(Robot<?> robot, String outputName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputName));
        MyController controller = (MyController) robot.getController();
        writer.write("index,x,y,function,edges,type\n");
        controller.getNodeSet().forEach(n -> {
            try {
                writer.write(n.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
    }

}