
import buildingBlocks.factories.ControllerFactory;
import buildingBlocks.MyController;
import geneticOps.*;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;
import morphologies.Morphology;
import morphologies.WormMorphology;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import org.dyn4j.dynamics.Settings;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;


public class testIntegrationAndCompute {

    private static final Random random = new Random(0);

    private static MyController getDefaultController() {
        Morphology morph = new WormMorphology(5, 1, "vel-area-touch");
        return new ControllerFactory(random::nextDouble, 1.0, morph.getBody(), morph.getNumSensors(), (x, y) -> x.getX() == y.getX() && x.getY() == y.getY()).build(random);
    }

    private static MyController getIdentityController() {
        Morphology morph = new WormMorphology(5, 1, "const");
        return new ControllerFactory(() -> 1.0, 1.0, morph.getBody(), morph.getNumSensors(), (x, y) -> x.getX() == y.getX() && x.getY() == y.getY()).build(random);
    }

    @Test(expected=Test.None.class /* no exception expected */)
    public void testSubsistence() {
        double episodeTime = 30.0;
        Settings physicsSettings = new Settings();
        Robot<?> testRobot = new Robot<>(getDefaultController(), (new WormMorphology(5, 1, "vel-area-touch")).getBody());
        Function<Robot<?>, Outcome> trainingTask = new Locomotion(episodeTime, Locomotion.createTerrain("flat"), physicsSettings);
        trainingTask.apply(testRobot);
    }

    @Test(expected=Test.None.class /* no exception expected */)
    public void testSubsistenceWithMutations() {
        double episodeTime = 30.0;
        Settings physicsSettings = new Settings();
        MyController controller = getDefaultController();
        controller = (new AddNodeMutation(() -> 1.0)).mutate(controller, random);
        controller = (new AddEdgeMutation(() -> 1.0)).mutate(controller, random);
        controller = (new MutateNode()).mutate(controller, random);
        controller = (new MutateEdge(0.1, 0.0)).mutate(controller, random);
        Robot<?> testRobot = new Robot<>(controller, (new WormMorphology(5, 5, "vel-area-touch")).getBody());
        Function<Robot<?>, Outcome> trainingTask = new Locomotion(episodeTime, Locomotion.createTerrain("flat"), physicsSettings);
        trainingTask.apply(testRobot);
    }

    @Test(expected=Test.None.class /* no exception expected */)
    public void testSubsistenceWithMutationsAndCrossover() {
        double episodeTime = 30.0;
        Settings physicsSettings = new Settings();
        MyController controller = getDefaultController();
        controller = (new AddNodeMutation(() -> 1.0)).mutate(controller, random);
        controller = (new AddEdgeMutation(() -> 1.0)).mutate(controller, random);
        controller = (new MutateNode()).mutate(controller, random);
        controller = (new MutateEdge(0.1, 0.0)).mutate(controller, random);
        controller = (new CrossoverWithDonation("traditional").recombine(controller, getIdentityController(), random));
        Robot<?> testRobot = new Robot<>(controller, (new WormMorphology(5, 5, "vel-area-touch")).getBody());
        Function<Robot<?>, Outcome> trainingTask = new Locomotion(episodeTime, Locomotion.createTerrain("flat"), physicsSettings);
        trainingTask.apply(testRobot);
    }

    @Test
    public void testCompute() {
        MyController controller = getIdentityController();
        Grid<? extends SensingVoxel> body = new WormMorphology(5, 1, "const").getBody();
        body.forEach(v -> {if (v.getValue() != null) {v.getValue().act(0.0);}});
        Collection<MyController.Neuron> nodes = controller.getNodeSet();
        nodes.forEach(n -> n.compute(body, controller));
        double value = Math.tanh(1.0);
        assertArrayEquals(new double[] {0.0, 0.0, 0.0, 0.0, 0.0}, nodes.stream().filter(MyController.Neuron::isActuator).mapToDouble(n -> n.send(0)).toArray(), 0.00001);
        nodes.forEach(MyController.Neuron::advance);
        assertArrayEquals(new double[] {value, value, value, value, value}, nodes.stream().filter(MyController.Neuron::isSensing).mapToDouble(n -> n.send(0)).toArray(), 0.00001);
        body.forEach(v -> {if (v.getValue() != null) {v.getValue().act(1.0);}});
        nodes.forEach(n -> n.compute(body, controller));
        assertArrayEquals(new double[] {value, value, value, value, value}, nodes.stream().filter(MyController.Neuron::isSensing).mapToDouble(n -> n.send(0)).toArray(), 0.00001);
        value = Math.tanh(value + 1.0);
        nodes.forEach(MyController.Neuron::advance);
        assertArrayEquals(new double[] {value, value, value, value, value}, nodes.stream().filter(MyController.Neuron::isActuator).mapToDouble(n -> n.send(0)).toArray(), 0.00001);
    }

}
