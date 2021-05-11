
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.malelab.jgea.core.util.Args;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class SurrogateValidator {

    private static final String[] terrains = {"flat", "hilly-1-10-0", "hilly-1-10-1", "hilly-1-10-2",
            "steppy-1-10-0", "steppy-1-10-1", "steppy-1-10-2", "uphill-10", "uphill-20", "downhill-10", "downhill-20"};
    private static final String[] header = {"validation.terrain", "validation.transformation", "validation.seed",
            "outcome.computation.time", "outcome.distance", "outcome.velocity", "\n"};

    public static void main(String[] args) throws IOException {
        String directory = Args.a(args, "dir", null);
        boolean validation = Boolean.parseBoolean(Args.a(args, "validation", null));
        String type = (validation) ? "validation" : "best";
        File bigDir = new File(directory);
        File[] files = bigDir.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isFile() && file.getPath().contains(type)) {
                validateAndwriteOnFile(file);
            }
        }
    }

    private static void validateAndwriteOnFile(File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath().replace("best", "test")));
        writer.write(String.join(";", header));
        String path = file.getPath().split("/")[file.getPath().split("/").length - 1];
        int seed = Integer.parseInt(path.split("\\.")[1]);
        Random random = new Random(seed);
        for (String terrain : terrains) {
            Outcome outcome = validateOnTerrain(file.getPath(), terrain, random);
            writer.write(String.join(";", terrain, "identity", String.valueOf(seed),
                    String.valueOf(outcome.getComputationTime()), String.valueOf(outcome.getDistance()),
                    String.valueOf(outcome.getVelocity()), "\n"));
        }
        writer.close();
    }

    private static Outcome validateOnTerrain(String bestFile, String terrain, Random random) {
        Robot<?> robot = parseIndividualFromFile(bestFile, random);
        Function<Robot<?>, Outcome> validationLocomotion = Main.buildLocomotionTask(terrain, 30.0, random);
        return validationLocomotion.apply(robot);
    }
    // TODO: duplication with ValidationBuilder
    private static Robot<?> parseIndividualFromFile(String fileName, Random random) {
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
        SerializationUtils.Mode mode = SerializationUtils.Mode.valueOf(SerializationUtils.Mode.GZIPPED_JSON.name().toUpperCase());
        return RobotUtils.buildRobotTransformation("identity", random)
                .apply(SerializationUtils.deserialize(records.get(records.size() -1).get("best→solution→serialized"), Robot.class, mode));
    }

}
