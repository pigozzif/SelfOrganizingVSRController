import buildingBlocks.ControllerFactory;
import buildingBlocks.MyController;
import geneticOps.AddEdgeMutation;
import geneticOps.AddNodeMutation;
import geneticOps.MutateEdge;
import geneticOps.MutateNode;
import morphologies.WormMorphology;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


public class testGeneticOperationsAndInit {

    private static final Random random = new Random(0);

    private static MyController getDefaultController() {
        return new ControllerFactory(random::nextDouble, 1.0, new WormMorphology(5, 1, "vel-area-touch")).build(random);
    }

    @Test
    public void testInitialization() {
        MyController controller = getDefaultController();
        List<MyController.Neuron> nodes = new ArrayList<>(controller.getNodeSet());
        assertEquals(25, nodes.size());
        assertEquals(5, nodes.stream().filter(n -> n.getType() == MyController.NodeType.ACTUATOR).count());
        assertEquals(20, nodes.stream().filter(n -> n.getType() == MyController.NodeType.SENSING).count());
        for (MyController.Neuron n : nodes) {
            if (n.getType() == MyController.NodeType.ACTUATOR) {
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
        AddNodeMutation mutation = new AddNodeMutation(new WormMorphology(5, 1, "vel-area-touch"), () -> 1.0);
        MyController mutant = mutation.mutate(controller, random);
        List<MyController.Neuron> nodes = new ArrayList<>(mutant.getNodeSet());
        assertEquals(26, nodes.size());
        MyController.Neuron[] candidates = nodes.stream().filter(n -> n.getType() == MyController.NodeType.HIDDEN).toArray(MyController.Neuron[]::new);
        assertEquals(1, candidates.length);
        assertSame(MyController.NodeType.SENSING, nodes.get(candidates[0].getIngoingEdges().get(0).getSource()).getType());
        candidates = nodes.stream().filter(n -> n.getType() == MyController.NodeType.ACTUATOR & n.getIngoingEdges().size() == 5).toArray(MyController.Neuron[]::new);
        assertEquals(1, candidates.length);
        assertSame(MyController.NodeType.HIDDEN, nodes.get(candidates[0].getIngoingEdges().get(4).getSource()).getType());
    }

    @Test
    public void testEdgePerturbation() {
        MyController controller = getDefaultController();
        MutateEdge mutation = new MutateEdge(0.1);
        MyController mutant = mutation.mutate(controller, random);
        Map<MyController.Edge, double[]> edges = controller.getNodeSet().stream().flatMap(n -> n.getIngoingEdges().stream()).collect(Collectors.toMap(Function.identity(), e -> new double[] {e.getParams().get(0), e.getParams().get(1)}));
        Map<MyController.Edge, double[]> newEdges = mutant.getNodeSet().stream().flatMap(n -> n.getIngoingEdges().stream()).collect(Collectors.toMap(Function.identity(), e -> new double[] {e.getParams().get(0), e.getParams().get(1)}));
        for (Map.Entry<MyController.Edge, double[]> entry : edges.entrySet()) {
            assertFalse(Arrays.equals(entry.getValue(), newEdges.get(entry.getKey())));
        }
    }

    @Test
    public void testNodeMutation() {
        MyController controller = getDefaultController();
        MutateNode mutation = new MutateNode();
        assertNull(mutation.pickNode(controller, random));
        AddNodeMutation mutationAdd = new AddNodeMutation(new WormMorphology(5, 1, "vel-area-touch"), () -> 1.0);
        MyController mutant = mutationAdd.mutate(controller, random);
        assertNotSame(mutation.pickNode(mutant, random).getActivation(), mutation.pickNode(mutation.mutate(mutant, random), random).getActivation());
    }

    @Test
    public void testAddEdgeMutation() {
        MyController controller = getDefaultController();
        AddEdgeMutation mutation = new AddEdgeMutation(() -> 1.0);
        MyController mutant = mutation.mutate(controller, random);
        assertEquals(21, mutant.getNodeSet().stream().flatMap(n -> n.getIngoingEdges().stream()).toArray().length);
        for (MyController.Edge e: mutant.getNodeSet().stream().flatMap(n -> n.getIngoingEdges().stream()).toArray(MyController.Edge[]::new)) {
            assertSame(mutant.getNodeSet().get(e.getSource()).getType(), MyController.NodeType.SENSING);
        }
    }

}
