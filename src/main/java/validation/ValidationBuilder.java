package validation;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.controllers.Controller;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;

// TODO: look up builder pattern
public class ValidationBuilder {

    // TODO: quite a lot of variables to extract; consider switching to an abstract class
    public static Controller<?>  parseIndividualFromFile(String fileName, Random random) {
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
        if (!headers.contains("serialized")) {
            throw new RuntimeException(String.format("input file %s does not contain serialization column", fileName));
        }
        SerializationUtils.Mode mode = SerializationUtils.Mode.valueOf(SerializationUtils.Mode.GZIPPED_JSON.name().toUpperCase());
        return RobotUtils.buildRobotTransformation("identity", random)
                .apply(SerializationUtils.deserialize(records.get(records.size() -1).get("serialized"), Robot.class, mode)).getController();
    }

    public static MyController buildValidation(String cuttingStrategy, String assemblingStrategy, String file1, String file2,
                                               Grid<? extends SensingVoxel> body, Random random) {
        MyController controller1 = (MyController) parseIndividualFromFile(file1, random);
        MyController controller2 = (MyController) parseIndividualFromFile(file2, random);
        return Assembler.createAssembler(assemblingStrategy).assemble(controller1, controller2, Cutter.createCutter(cuttingStrategy).cut(body));
    }

}
