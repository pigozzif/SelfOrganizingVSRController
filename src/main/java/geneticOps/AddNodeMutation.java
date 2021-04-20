package geneticOps;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.malelab.jgea.core.operator.Mutation;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class AddNodeMutation implements Mutation<MyController> {

    private final Supplier<Double> parameterSupplier;
    private final double perc;
    private static final double MAX_DIST = 1.0;

    public AddNodeMutation(Supplier<Double> sup, double p) {
        this.parameterSupplier = sup;
        this.perc = p;
    }

    public AddNodeMutation(Supplier<Double> sup) {
        this(sup, 1.0);
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        Pair<Integer, Integer> pair = parent.getValidCoordinates()[random.nextInt(parent.getValidCoordinates().length)];
        int sampleX = pair.getFirst();
        int sampleY = pair.getSecond();
        MyController newBorn = new MyController(parent);
        //List<MyController.Edge> edges = newBorn.getEdgeSet();
        //MyController.Edge edge = edges.get(random.nextInt(edges.size()));
        //newBorn.splitEdge(edge, this.parameterSupplier, random);
        if (random.nextDouble() <= this.perc) {
            this.enableMutation(newBorn, sampleX, sampleY, random);
        }
        else {
            this.disableMutation(newBorn, random);
        }
        return newBorn;
    }

    private void enableMutation(MyController controller, int sampleX, int sampleY, Random random) {
        List<MyController.Neuron> candidates = controller.getNodeSet().stream().filter(n -> MyController.euclideanDistance(sampleX, sampleY, n.getX(), n.getY()) <= MAX_DIST)
                .collect(Collectors.toList());
        Pair<MyController.Neuron, MyController.Neuron> trial = this.pickPair(candidates, random);
        controller.addHiddenNode(trial.getFirst().getIndex(), trial.getSecond().getIndex(), MultiLayerPerceptron.ActivationFunction.SIGMOID, sampleX, sampleY, this.parameterSupplier);
    }

    private Pair<MyController.Neuron, MyController.Neuron> pickPair(List<MyController.Neuron> candidates, Random random) {
        List<MyController.Neuron> sourceCandidates = candidates.stream().filter(n -> !n.isActuator()).collect(Collectors.toList());
        List<MyController.Neuron> targetCandidates = candidates.stream().filter(n -> !n.isSensing()).collect(Collectors.toList());
        MyController.Neuron source = sourceCandidates.get(random.nextInt(sourceCandidates.size()));
        MyController.Neuron target = targetCandidates.get(random.nextInt(targetCandidates.size()));
        return new Pair<>(source, target);
    }

    private void disableMutation(MyController controller, Random random) {
        List<MyController.Neuron> candidates = controller.getNodeSet().stream().filter(MyController.Neuron::isHidden).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return;
        }
        MyController.Neuron candidate = candidates.get(random.nextInt(candidates.size()));
        controller.removeNeuron(candidate);
    }

}
