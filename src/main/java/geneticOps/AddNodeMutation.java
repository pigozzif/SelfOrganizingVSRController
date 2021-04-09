package geneticOps;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.malelab.jgea.core.operator.Mutation;
import morphologies.Morphology;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class AddNodeMutation implements Mutation<MyController> {

    private final Morphology morphology;
    private final Supplier<Double> parameterSupplier;

    public AddNodeMutation(Morphology morph, Supplier<Double> sup) {
        morphology = morph;
        parameterSupplier = sup;
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        Morphology.Pair sample = morphology.getAllowedMorph().get(random.nextInt(morphology.getAllowedMorph().size()));
        //MultiLayerPerceptron.ActivationFunction a = MultiLayerPerceptron.ActivationFunction.values()[random.nextInt(MultiLayerPerceptron.ActivationFunction.values().length)];
        MyController newBorn = new MyController(parent);
        List<MyController.Neuron> candidates = newBorn.getNodeSet().stream().filter(n -> MyController.euclideanDistance(sample.first, sample.second, n.getX(), n.getY()) <= 1.0)
                .collect(Collectors.toList());
        List<Integer> indexes = IntStream.range(0, candidates.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(indexes, random);
        int source = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isActuator()).findFirst().get()).getIndex();
        int dest = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isSensing()).findFirst().get()).getIndex();
        // TODO: is it really necessary to avoid actuators as sources and sensors as targets?
        newBorn.addHiddenNode(source, dest, MultiLayerPerceptron.ActivationFunction.SIGMOID, sample.first, sample.second, this.parameterSupplier);
        return newBorn;
    }

}
