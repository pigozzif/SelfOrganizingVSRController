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
        return new ControllerFactory(random::nextDouble, 1.0, morph.getBody(), morph.getNumSensors()).build(random);
    }

    private static MyController getIdentityController(double fixedParam) {
        Morphology morph = new WormMorphology(5, 1, "vel-area-touch");
        return new ControllerFactory(() -> fixedParam, 1.0, morph.getBody(), morph.getNumSensors()).build(random);
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
    public void testNodeMutation() {
        MyController controller = getDefaultController();
        MutateNode mutation = new MutateNode();
        assertNull(mutation.pickNode(controller, random));
        AddNodeMutation mutationAdd = new AddNodeMutation(() -> 1.0);
        MyController mutant = mutationAdd.mutate(controller, random);
        assertNotSame(mutation.pickNode(mutant, random).getActivation(), mutation.pickNode(mutation.mutate(mutant, random), random).getActivation());
    }

    @Test
    public void testAddEdgeMutation() {
        MyController controller = getDefaultController();
        AddEdgeMutation mutation = new AddEdgeMutation(() -> 1.0, 1.0);
        MyController mutant = mutation.mutate(controller, random);
        assertEquals(21, mutant.getEdgeSet().size());
        for (MyController.Edge e: mutant.getEdgeSet()) {
            assertTrue(mutant.getNodeMap().get(e.getSource()).isSensing());
        }
    }

    @Test
    public void testCrossoverWithInnovationSimple() {
        MyController mother = getIdentityController(1.0);
        MyController father = getIdentityController(2.0);
        CrossoverWithInnovation crossoverWithInnovation = new CrossoverWithInnovation();
        MyController newBorn = crossoverWithInnovation.recombine(mother, father, random);
        assertEquals(25, newBorn.getNodeSet().size());
        assertEquals(20, newBorn.getEdgeSet().size());
        List<Integer> nodeInnovations = new ArrayList<>();
        nodeInnovations.addAll(mother.getNodeSet().stream().map(MyController.Neuron::getIndex).collect(Collectors.toList()));
        nodeInnovations.addAll(father.getNodeSet().stream().map(MyController.Neuron::getIndex).collect(Collectors.toList()));
        for (MyController.Neuron n : newBorn.getNodeSet()) {
            assertTrue(nodeInnovations.contains(n.getIndex()));
        }
        List<Integer> edgeInnovations = new ArrayList<>();
        edgeInnovations.addAll(mother.getEdgeSet().stream().map(MyController.Edge::getIndex).collect(Collectors.toList()));
        edgeInnovations.addAll(father.getEdgeSet().stream().map(MyController.Edge::getIndex).collect(Collectors.toList()));
        for (MyController.Edge e : newBorn.getEdgeSet()) {
            assertTrue(edgeInnovations.contains(e.getIndex()));
        }
        for (int i=0; i < 25; ++i) {
            assertTrue(newBorn.getNodeMap().containsKey(i));
        }
    }

    @Test
    public void testCrossoverWithInnovationComplex() {
        MyController mother = getIdentityController(1.0);
        MyController father = getIdentityController(2.0);
        CrossoverWithInnovation crossoverWithInnovation = new CrossoverWithInnovation();
        AddEdgeMutation edgeMutation = new AddEdgeMutation(() -> 1.0, 1.0);
        AddNodeMutation nodeMutation = new AddNodeMutation(() -> 1.0);
        for (int i=0; i < 50; ++i) {
            if (i % 2 != 0) {
                father = edgeMutation.mutate(father, random);
                mother = edgeMutation.mutate(mother, random);
            }
            else {
                father = nodeMutation.mutate(father, random);
                mother = nodeMutation.mutate(mother, random);
            }
        }
        MyController newBorn = crossoverWithInnovation.recombine(mother, father, random);
        List<Integer> nodeInnovations = new ArrayList<>();
        nodeInnovations.addAll(mother.getNodeSet().stream().map(MyController.Neuron::getIndex).collect(Collectors.toList()));
        nodeInnovations.addAll(father.getNodeSet().stream().map(MyController.Neuron::getIndex).collect(Collectors.toList()));
        for (MyController.Neuron n : newBorn.getNodeSet()) {
            assertTrue(nodeInnovations.contains(n.getIndex()));
        }
        List<Integer> edgeInnovations = new ArrayList<>();
        edgeInnovations.addAll(mother.getEdgeSet().stream().map(MyController.Edge::getIndex).collect(Collectors.toList()));
        edgeInnovations.addAll(father.getEdgeSet().stream().map(MyController.Edge::getIndex).collect(Collectors.toList()));
        for (MyController.Edge e : newBorn.getEdgeSet()) {
            assertTrue(edgeInnovations.contains(e.getIndex()));
        }
        for (int i=0; i < 25; ++i) {
            assertTrue(newBorn.getNodeMap().containsKey(i));
        }
    }

    @Test
    public void testCrossoverWithDonationSimple() {
        MyController mother = getIdentityController(1.0);
        MyController father = getIdentityController(2.0);
        CrossoverWithDonation crossover = new CrossoverWithDonation("growing");
        MyController newBorn = crossover.recombine(mother, father, random);
        assertEquals(25, newBorn.getNodeSet().size());
        assertEquals(20, newBorn.getEdgeSet().size());
        assertEquals(20, newBorn.getNodeSet().stream().filter(MyController.Neuron::isSensing).count());
        assertEquals(5, newBorn.getNodeSet().stream().filter(MyController.Neuron::isActuator).count());
    }

    @Test(expected=Test.None.class /* no exception expected */)
    public void testCrossoverWithDonationLong() {
        MyController mother = getIdentityController(1.0);
        MyController father = getIdentityController(2.0);
        CrossoverWithDonation crossover = new CrossoverWithDonation("growing");
        AddEdgeMutation edgeMutation = new AddEdgeMutation(() -> 1.0, 1.0);
        AddNodeMutation nodeMutation = new AddNodeMutation(() -> 1.0);
        for (int i=0; i < 50; ++i) {
            if (i % 2 != 0) {
                father = edgeMutation.mutate(father, random);
                mother = edgeMutation.mutate(mother, random);
            }
            else {
                father = nodeMutation.mutate(father, random);
                mother = nodeMutation.mutate(mother, random);
            }
        }
        crossover.recombine(mother, father, random);
    }

    @Test
    public void testCrossoverWithDonationComplex() {
        MyController mother = getIdentityController(1.0);
        MyController father = getIdentityController(2.0);
        CrossoverWithDonation crossover = new CrossoverWithDonation("growing");
        mother.addHiddenNode(0, 4, MultiLayerPerceptron.ActivationFunction.SIGMOID, 0, 0, () -> 1.0);
        mother.addEdge(5, 4, 1.0, 1.0);
        mother.addEdge(8, 14, 1.0, 1.0);
        father.addHiddenNode(5, 9, MultiLayerPerceptron.ActivationFunction.SIGMOID, 1, 0, () -> 1.0);
        father.addHiddenNode(3, MyController.computeIndex(5, 9), MultiLayerPerceptron.ActivationFunction.SIGMOID, 0, 0, () -> 1.0);
        father.addHiddenNode(5, 4, MultiLayerPerceptron.ActivationFunction.SIGMOID, 0, 1, () -> 1.0);
        MyController newBorn = crossover.amputateVoxel(father, mother, 0, 0, random);
        assertEquals(26, newBorn.getNodeSet().size());
        assertEquals(23, newBorn.getEdgeSet().size());
        assertEquals(1, newBorn.getNodeSet().stream().filter(MyController.Neuron::isHidden).count());
        assertEquals(6, newBorn.getNodeSet().stream().filter(n -> n.getY() == 0 && n.getX() == 0).count());
    }

}
