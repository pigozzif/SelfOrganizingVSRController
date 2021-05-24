import buildingBlocks.factories.ControllerFactory;
import buildingBlocks.MyController;
import geneticOps.*;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import morphologies.Morphology;
import morphologies.WormMorphology;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


public class testGeneticOperationsAndInit {

    private static final Random random = new Random(0);

    private static MyController getDefaultController() {
        Morphology morph = new WormMorphology(5, 1, "vel-area-touch");
        return new ControllerFactory(random::nextDouble, 1.0, morph.getBody(), morph.getNumSensors(), (x, y) -> x.getX() == y.getX() && x.getY() == y.getY()).build(random);
    }

    private static MyController getIdentityController(double fixedParam) {
        Morphology morph = new WormMorphology(5, 1, "vel-area-touch");
        return new ControllerFactory(() -> fixedParam, 1.0, morph.getBody(), morph.getNumSensors(), (x, y) -> x.getX() == y.getX() && x.getY() == y.getY()).build(random);
    }

    @Test
    public void testInitialization() {
        MyController controller = getDefaultController();
        List<MyController.Neuron> nodes = new ArrayList<>(controller.getNodeSet());
        assertEquals(25, nodes.size());
        assertEquals(5, nodes.stream().filter(MyController.Neuron::isActuator).count());
        assertEquals(20, nodes.stream().filter(MyController.Neuron::isSensing).count());
        for (MyController.Neuron n : nodes) {
            if (n.isActuator()) {
                assertEquals(4, n.getIngoingEdges().size());
            }
            else {
                assertEquals(0, n.getIngoingEdges().size());
            }
        }
    }

    @Test
    public void testAddNodeMutation() {
        MyController controller = getDefaultController();
        AddNodeMutation mutation = new AddNodeMutation(() -> 1.0);
        MyController mutant = mutation.mutate(controller, random);
        Map<Integer, MyController.Neuron> nodes = mutant.getNodeMap();
        assertEquals(26, nodes.size());
        MyController.Neuron[] candidates = nodes.values().stream().filter(MyController.Neuron::isHidden).toArray(MyController.Neuron[]::new);
        assertEquals(1, candidates.length);
        assertTrue(nodes.get(candidates[0].getIngoingEdges().get(0).getSource()).isSensing());
        candidates = nodes.values().stream().filter(n -> n.isActuator() & n.getIngoingEdges().size() == 5).toArray(MyController.Neuron[]::new);
        assertEquals(1, candidates.length);
        assertTrue(nodes.get(candidates[0].getIngoingEdges().get(4).getSource()).isHidden());
    }

    @Test
    public void testEdgePerturbation() {
        MyController controller = getDefaultController();
        MutateEdge mutation = new MutateEdge(0.1, 0.0);
        MyController mutant = mutation.mutate(controller, random);
        Map<MyController.Edge, double[]> edges = controller.getEdgeSet().stream().collect(Collectors.toMap(Function.identity(), MyController.Edge::getParams));
        Map<MyController.Edge, double[]> newEdges = mutant.getEdgeSet().stream().collect(Collectors.toMap(Function.identity(), MyController.Edge::getParams));
        for (Map.Entry<MyController.Edge, double[]> entry : edges.entrySet()) {
            assertFalse(Arrays.equals(entry.getValue(), newEdges.get(entry.getKey())));
        }
    }

    @Test
    public void testAddEdgeMutation() {
        MyController controller = getDefaultController();
        AddEdgeMutation mutation = new AddEdgeMutation(() -> 1.0);
        MyController mutant = mutation.mutate(controller, random);
        assertEquals(21, mutant.getEdgeSet().size());
        for (MyController.Edge e: mutant.getEdgeSet()) {
            assertTrue(mutant.getNodeMap().get(e.getSource()).isSensing());
        }
    }

}
