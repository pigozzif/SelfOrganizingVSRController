package geneticOps;

import buildingBlocks.MyController;
import morphologies.Morphology;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class AddNodeMutation extends StructuralMutation {

    private final Morphology morphology;
    private final Supplier<Double> parameterSupplier;

    public AddNodeMutation(Morphology morph, Supplier<Double> sup) {
        morphology = morph;
        parameterSupplier = sup;
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        ++INNOVATION_COUNTER;
        Morphology.Pair sample = morphology.getAllowedMorph().get(random.nextInt(morphology.getAllowedMorph().size()));
        //MultiLayerPerceptron.ActivationFunction a = MultiLayerPerceptron.ActivationFunction.values()[random.nextInt(MultiLayerPerceptron.ActivationFunction.values().length)];
        MyController newBorn = new MyController(parent);
        MyController.Neuron newNode = newBorn.addHiddenNode(sample.first, sample.second, INNOVATION_COUNTER);
        List<MyController.Neuron> candidates = newBorn.getNodeSet().stream().filter(n -> newNode.getIndex() != n.getIndex() && MyController.euclideanDistance(newNode, n) <= 1.0)
                .collect(Collectors.toList());
        List<Integer> indexes = IntStream.range(0, candidates.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(indexes, random);
        int source = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isActuator()).findFirst().get()).getIndex();
        int dest = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isSensing()).findFirst().get()).getIndex();
        // TODO: is it really necessary to avoid actuators as sources and sensors as targets?
        newBorn.addEdge(source, newNode.getIndex(), parameterSupplier.get(), parameterSupplier.get(), INNOVATION_COUNTER);
        newBorn.addEdge(newNode.getIndex(), dest, parameterSupplier.get(), parameterSupplier.get(), INNOVATION_COUNTER);
        return newBorn;
    }

}
