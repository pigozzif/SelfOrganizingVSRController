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

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;


public class ValidationBuilder {

    private final String shape;
    private final String serializationColumn;
    private final String transformation;
    private final Cutter cutter;
    private final Assembler assembler;

    public ValidationBuilder(String shape, String serialized, String transf, String cuttingStrategy, String assemblyStrategy) {
        this.shape = shape;
        this.serializationColumn = serialized;
        this.transformation = transf;
        this.cutter = Cutter.createCutter(cuttingStrategy);
        this.assembler = Assembler.createAssembler(assemblyStrategy, this.shape);
    }

    public ValidationBuilder(String shape, String cuttingStrategy, String assemblyStrategy) {
        this(shape, "best→solution→serialized", "identity", cuttingStrategy, assemblyStrategy);
    }

    public MyController buildValidation(String file1, String file2, Grid<? extends SensingVoxel> body, Random random) {
        MyController controller1 = (MyController) parseIndividualFromFile(file1, random);
        //writeBrainToFile(controller1, "receiver.txt", "./src/main/java/validation/test/receiver.png");
        MyController controller2 = (MyController) parseIndividualFromFile(file2, random);
        //writeBrainToFile(controller2, "donator.txt", "./src/main/java/validation/test/donator.png");
        MyController output = this.assembler.assemble(controller1, controller2, this.cutter.cut(body), random);
        //writeBrainToFile(output, "child.txt", "./src/main/java/validation/test/child.png");
        return output;
    }

    public Controller<?> parseIndividualFromFile(String fileName, Random random) {
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
        if (!headers.contains(this.serializationColumn)) {
            throw new RuntimeException(String.format("input file %s does not contain serialization column", fileName));
        }
        SerializationUtils.Mode mode = SerializationUtils.Mode.valueOf(SerializationUtils.Mode.GZIPPED_JSON.name().toUpperCase());
        return RobotUtils.buildRobotTransformation(this.transformation, random)
                .apply(SerializationUtils.deserialize(records.get(records.size() - 1).get(this.serializationColumn), Robot.class, mode)).getController();
    }

    public Grid<Boolean> toBeCut(Grid<? extends SensingVoxel> body) {
        return this.cutter.cut(body);
    }

    private static void writeBrainToFile(MyController controller, String inputName, String outputName) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(inputName));
            writer.write("index,x,y,function,edges,type\n");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        BufferedWriter finalWriter = writer;
        controller.getNodeSet().forEach(n -> {
            try {
                finalWriter.write(n.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        try {
            writer.close();
            Process p = Runtime.getRuntime().exec("python python/visualize_brain.py " + inputName + " " + outputName);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
