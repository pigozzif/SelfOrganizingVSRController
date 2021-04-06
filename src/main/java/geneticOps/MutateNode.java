package geneticOps;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.malelab.jgea.core.operator.Mutation;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class MutateNode implements Mutation<MyController> {

    private final MultiLayerPerceptron.ActivationFunction[] functions;  // TODO: really need it?

    public MutateNode() {
        this.functions = MultiLayerPerceptron.ActivationFunction.values();
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        MyController.Neuron chosenOne = this.pickNode(newBorn, random);
        if (chosenOne == null) {
            return newBorn;
        }
        MultiLayerPerceptron.ActivationFunction oldA = chosenOne.getActivation();
        MultiLayerPerceptron.ActivationFunction newA;
        do {
            newA = this.functions[random.nextInt(functions.length)];
        } while (newA == oldA);
        chosenOne.setActivation(newA);
        return newBorn;
    }

    public MyController.Neuron pickNode(MyController controller, Random random) {
        List<MyController.Neuron> candidates = controller.getNodeSet().stream().filter(MyController.Neuron::isHidden)
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

}
