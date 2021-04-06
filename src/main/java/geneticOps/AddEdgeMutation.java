package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class AddEdgeMutation implements Mutation<MyController> {

    private final Supplier<Double> parameterSupplier;

    public AddEdgeMutation(Supplier<Double> s) { this.parameterSupplier = s; }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        List<MyController.Neuron> nodes = newBorn.getNodeSet();
        List<Integer> indexes = IntStream.range(0, nodes.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(indexes, random);
        // TODO: for the moment, we don't allow outgoing edges from actuators
        MyController.Neuron source = nodes.get(indexes.stream().filter(i -> nodes.get(i).getType() != MyController.NodeType.ACTUATOR).findFirst().get());
        int target = nodes.get(indexes.stream().filter(i -> nodes.get(i).getIndex() != source.getIndex() &&
                MyController.euclideanDistance(source, nodes.get(i)) <= 1.0 && nodes.get(i).getType() != MyController.NodeType.SENSING).findFirst().get()).getIndex();
        newBorn.addEdge(source.getIndex(), target, this.parameterSupplier.get(), this.parameterSupplier.get());
        return newBorn;
    }

}
