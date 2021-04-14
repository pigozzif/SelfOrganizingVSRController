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
    // TODO: we now outlaw multiedges, but this couls make sense with delay
    private void addMutation(MyController controller, Random random) {
        Map<Integer, MyController.Neuron> nodes = controller.getNodeMap();
        List<Integer> indexes = new ArrayList<>(nodes.keySet());
        Collections.shuffle(indexes, random);
        Pair<Integer, Integer> candidates = this.pickPair(nodes, random);
        // TODO: for the moment, we don't allow outgoing edges from actuators
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
    // TODO: might be the source of incorrect results for tests, still to verify
    private void enableAndDisableMutation(MyController controller, Random random) {
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        candidate.perturbAbility();
    }

}
