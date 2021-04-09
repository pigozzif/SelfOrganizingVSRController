package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class AddEdgeMutation implements Mutation<MyController> {

    private final Supplier<Double> parameterSupplier;
    private final double perc;

    public AddEdgeMutation(Supplier<Double> s, double p) {
        this.parameterSupplier = s;
        this.perc = p;
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        if (random.nextDouble() <= this.perc) {
            this.addMutation(newBorn, random);
        }
        else {
            this.enableAndDisableMutation(newBorn, random);
        }
        return newBorn;
    }

    private void addMutation(MyController controller, Random random) {
        Map<Integer, MyController.Neuron> nodes = controller.getNodeMap();
        List<Integer> indexes = IntStream.range(0, nodes.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(indexes, random);
        // TODO: for the moment, we don't allow outgoing edges from actuators
        MyController.Neuron source = nodes.get(indexes.stream().filter(i -> !nodes.get(i).isActuator()).findFirst().get());
        int target = nodes.get(indexes.stream().filter(i -> nodes.get(i).getIndex() != source.getIndex() &&
                MyController.euclideanDistance(source, nodes.get(i)) <= 1.0 && !nodes.get(i).isSensing()).findFirst().get()).getIndex();
        controller.addEdge(source.getIndex(), target, this.parameterSupplier.get(), this.parameterSupplier.get());
    }
    // TODO: might be the source of incorrect results for tests, still to verify
    private void enableAndDisableMutation(MyController controller, Random random) {
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        candidate.perturbAbility();
    }

}
