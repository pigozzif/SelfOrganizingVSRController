package geneticOps;

import buildingBlocks.MyController;
import morphologies.Morphology;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.malelab.jgea.core.operator.Mutation;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class AddNodeMutation implements Mutation<MyController> {

    private final Morphology morphology;
    private final MultiLayerPerceptron.ActivationFunction[] functions;
    private final Supplier<Double> parameterSupplier;

    public AddNodeMutation(Morphology morph, Supplier<Double> sup) {
        morphology = morph;
        functions = MultiLayerPerceptron.ActivationFunction.values();
        parameterSupplier = sup;
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        Morphology.Pair sample = morphology.getAllowedMorph().get(random.nextInt(morphology.getAllowedMorph().size()));
        MultiLayerPerceptron.ActivationFunction a = functions[random.nextInt(functions.length)];
        MyController newBorn = new MyController(parent);
        MyController.Neuron newNode = newBorn.addHiddenNode(a, sample.first, sample.second);
        List<MyController.Neuron> candidates = newBorn.getNodeSet().stream().filter(n -> newNode.getIndex() != n.getIndex() && MyController.euclideanDistance(newNode, n) <= 1.0)
                .collect(Collectors.toList());
        List<Integer> indexes = IntStream.range(0, candidates.size()).boxed().collect(Collectors.toList());
        int source, dest;
        //do {
            Collections.shuffle(indexes, random);
            source = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isActuator()).findFirst().get()).getIndex();
            dest = candidates.get(indexes.stream().filter(i -> !candidates.get(i).isSensing()).findFirst().get()).getIndex();
        //} while (source == dest);
        // TODO: is it really necessary to avoid actuators as sources and sensors as targets?
        newBorn.addEdge(source, newNode.getIndex(), parameterSupplier.get(), parameterSupplier.get());
        newBorn.addEdge(newNode.getIndex(), dest, parameterSupplier.get(), parameterSupplier.get());
        return newBorn;
    }

}
