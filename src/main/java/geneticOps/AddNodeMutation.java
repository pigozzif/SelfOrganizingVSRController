package geneticOps;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.malelab.jgea.core.util.Misc;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class AddNodeMutation implements TopologicalMutation {

    private final Supplier<Double> parameterSupplier;
    private final double perc;
    private final double maxDist;
    private final String morphology;
    private final String configuration;

    public AddNodeMutation(Supplier<Double> sup, double p, String dist, String morph, String conf) {
        this.parameterSupplier = sup;
        this.perc = p;
        this.maxDist = TopologicalMutation.getMaxDist(dist);
        this.morphology = morph;
        this.configuration = conf;
    }

    public AddNodeMutation(Supplier<Double> sup) {
        this(sup, 1.0, "minimal", "worm-5x1", "minimal");
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        Pair<Integer, Integer> pair = parent.getValidCoordinates()[random.nextInt(parent.getValidCoordinates().length)];
        int sampleX = pair.getFirst();
        int sampleY = pair.getSecond();
        MyController newBorn = new MyController(parent);
        if (random.nextDouble() <= this.perc) {
            this.enableMutation(newBorn, sampleX, sampleY, random);
            newBorn.setOrigin("add_node");
        }
        else {
            this.disableMutation(newBorn, random);
            newBorn.setOrigin("remove_edge");
        }
        return newBorn;
    }

    private void enableMutation(MyController controller, int sampleX, int sampleY, Random random) {
        List<MyController.Neuron> candidates = controller.getNodeSet().stream().filter(n -> MyController.euclideanDistance(sampleX, sampleY, n.getX(), n.getY()) <= this.maxDist)
                .collect(Collectors.toList());
        Pair<Integer, Integer> trial = this.pickPair(candidates, random);
        controller.addHiddenNode(trial.getFirst(), trial.getSecond(), MultiLayerPerceptron.ActivationFunction.SIGMOID, sampleX, sampleY, this.parameterSupplier);
    }

    private Pair<Integer, Integer> pickPair(List<MyController.Neuron> candidates, Random random) {
        Map<Pair<Integer, Integer>, Double> nodes = new HashMap<>();
        double[] probs = TopologicalMutation.getEdgeProbs(this.configuration);
        for (MyController.Neuron neuron1 : candidates) {
            for (MyController.Neuron neuron2 : candidates) {
                if (!neuron1.isActuator() && !neuron2.isSensing()) {
                    double weight = (TopologicalMutation.isNotCrossingEdge(this.morphology, neuron1, neuron2)) ? probs[0] : probs[1];
                    nodes.put(new Pair<>(neuron1.getIndex(), neuron2.getIndex()), weight);
                }
            }
        }
        return Misc.pickRandomly(nodes, random);
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
