package geneticOps;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.malelab.jgea.core.util.Misc;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class AddBoundedNodeMutation extends AddNodeMutation {

    private final List<Pair<Integer, Integer>> module;

    public AddBoundedNodeMutation(Supplier<Double> sup, double p, String dist, String morph, String conf,
                                  List<Pair<Integer, Integer>> m) {
        super(sup, p, dist, morph, conf);
        this.module = m;
    }

    @Override
    public void enableMutation(MyController controller, int sampleX, int sampleY, Random random) {
        List<MyController.Neuron> candidates = controller.getNodeSet().stream().filter(n -> MyController.euclideanDistance(sampleX, sampleY, n.getX(), n.getY()) <= this.maxDist)
                .collect(Collectors.toList());
        Pair<Integer, Integer> trial = this.pickPair(candidates, random);
        controller.addHiddenNode(trial.getFirst(), trial.getSecond(), MultiLayerPerceptron.ActivationFunction.SIGMOID, sampleX, sampleY, this.parameterSupplier);
    }

    private Pair<Integer, Integer> pickPair(List<MyController.Neuron> candidates, Random random) {
        List<Pair<Integer, Integer>> nodes = new ArrayList<>();
        for (MyController.Neuron neuron1 : candidates) {
            for (MyController.Neuron neuron2 : candidates) {
                if (!neuron1.isActuator() && !neuron2.isSensing() && TopologicalMutation.areCrossingModule(this.module, neuron1, neuron2)) {
                    nodes.add(new Pair<>(neuron1.getIndex(), neuron2.getIndex()));
                }
            }
        }
        return Misc.pickRandomly(nodes, random);
    }

    @Override
    public void disableMutation(MyController controller, Random random) {
        List<MyController.Neuron> candidates = new ArrayList<>();
        for (MyController.Neuron neuron : controller.getNodeSet()) {
            if (!neuron.isHidden()) {
                continue;
            }
            for (MyController.Edge edge : controller.getAllEdges(neuron)) {
                MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
                MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
                if (TopologicalMutation.areCrossingModule(this.module, source, target)) {
                    candidates.add(neuron);
                    break;
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        MyController.Neuron candidate = candidates.get(random.nextInt(candidates.size()));
        controller.removeNeuron(candidate);
    }

}
