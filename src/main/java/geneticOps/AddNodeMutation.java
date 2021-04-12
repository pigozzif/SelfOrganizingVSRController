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

    public AddNodeMutation(Supplier<Double> sup) {
        parameterSupplier = sup;
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        Pair<Integer, Integer> pair = parent.getValidCoordinates()[random.nextInt(parent.getValidCoordinates().length)];
        int sampleX = pair.getFirst();//parent.getValidXCoordinates()[random.nextInt(parent.getValidXCoordinates().length)];
        int sampleY = pair.getSecond();//parent.getValidYCoordinates()[random.nextInt(parent.getValidYCoordinates().length)];
        //MultiLayerPerceptron.ActivationFunction a = MultiLayerPerceptron.ActivationFunction.values()[random.nextInt(MultiLayerPerceptron.ActivationFunction.values().length)];
        MyController newBorn = new MyController(parent);
        //List<MyController.Edge> edges = newBorn.getEdgeSet();
        //MyController.Edge edge = edges.get(random.nextInt(edges.size()));
        //newBorn.splitEdge(edge, this.parameterSupplier, random);
        Map<Integer, MyController.Neuron> candidates = newBorn.getNodeMap().entrySet().stream().filter(n -> MyController.euclideanDistance(sampleX, sampleY, n.getValue().getX(), n.getValue().getY()) <= 1.0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Pair<MyController.Neuron, MyController.Neuron> trial = this.pickPair(candidates, random);
        // TODO: is it really necessary to avoid actuators as sources and sensors as targets?
        newBorn.addHiddenNode(trial.getFirst().getIndex(), trial.getSecond().getIndex(), MultiLayerPerceptron.ActivationFunction.SIGMOID, sampleX, sampleY, this.parameterSupplier);
        return newBorn;
    }

    private Pair<MyController.Neuron, MyController.Neuron> pickPair(Map<Integer, MyController.Neuron> candidates, Random random) {
        List<Integer> indexes = new ArrayList<>(candidates.keySet());
        Collections.shuffle(indexes, random);
        MyController.Neuron source = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isActuator()).findFirst().get());
        MyController.Neuron dest = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isSensing()).findFirst().get());
        return new Pair<>(source, dest);
    }

}
