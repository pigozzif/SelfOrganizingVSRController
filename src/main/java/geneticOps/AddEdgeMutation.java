package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;


public class AddEdgeMutation implements Mutation<MyController> {

    private final Supplier<Double> parameterSupplier;
    private final double perc;

    public AddEdgeMutation(Supplier<Double> s, double p) {
        this.parameterSupplier = s;
        this.perc = p;
    }

    public AddEdgeMutation(Supplier<Double> s) {
        this(s, 1.0);
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        if (random.nextDouble() <= this.perc) {
            this.enableMutation(newBorn, random);
        }
        else {
            this.disableMutation(newBorn, random);
        }
        return newBorn;
    }

    private void enableMutation(MyController controller, Random random) {
        Map<Integer, MyController.Neuron> nodes = controller.getNodeMap();
        List<Integer> indexes = new ArrayList<>(nodes.keySet());
        Collections.shuffle(indexes, random);
        Pair<Integer, Integer> candidates = this.pickPair(nodes, random);
        controller.addEdge(candidates.getFirst(), candidates.getSecond(), this.parameterSupplier.get(), this.parameterSupplier.get());
    }

    private Pair<Integer, Integer> pickPair(Map<Integer, MyController.Neuron> nodes, Random random) {
        Optional<Integer> candidate;
        MyController.Neuron source;
        do {
            List<Integer> indexes = new ArrayList<>(nodes.keySet());
            Collections.shuffle(indexes, random);
            source = nodes.get(indexes.stream().filter(i -> !nodes.get(i).isActuator()).findFirst().get());
            MyController.Neuron finalSource = source;
            candidate = indexes.stream().filter(i -> nodes.get(i).getIndex() != finalSource.getIndex() &&
                    MyController.euclideanDistance(finalSource, nodes.get(i)) <= 1.0 && !nodes.get(i).isSensing() && !nodes.get(i).hasInNeighbour(finalSource)).findFirst();
        } while (candidate.isEmpty());
        return new Pair<>(source.getIndex(), candidate.get());
    }

    private void disableMutation(MyController controller, Random random) {
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        controller.removeEdge(candidate);
    }

}
