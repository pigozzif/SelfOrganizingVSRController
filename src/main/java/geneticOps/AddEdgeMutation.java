package geneticOps;

import buildingBlocks.MyController;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;


public class AddEdgeMutation implements TopologicalMutation {

    private final Supplier<Double> parameterSupplier;
    private final double perc;
    private final double maxDist;

    public AddEdgeMutation(Supplier<Double> s, double p, String dist) {
        this.parameterSupplier = s;
        this.perc = p;
        this.maxDist = TopologicalMutation.getMaxDist(dist);
    }

    public AddEdgeMutation(Supplier<Double> s) {
        this(s, 1.0, "minimal");
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
        List<Pair<Integer, Integer>> nodes = new LinkedList<>();
        for (MyController.Neuron n1 : controller.getNodeSet()) {
            for (MyController.Neuron n2 : controller.getNodeSet()) {
                if (!n2.isSensing() && !n1.isActuator() && MyController.euclideanDistance(n1, n2) <= this.maxDist && !n2.hasInNeighbour(n1) && n1.getIndex() != n2.getIndex()) {
                    nodes.add(new Pair<>(n1.getIndex(), n2.getIndex()));
                }
            }
        }
        if (nodes.isEmpty()) {
            return;
        }
        Pair<Integer, Integer> chosenOne = nodes.get(random.nextInt(nodes.size()));
        controller.addEdge(chosenOne.getFirst(), chosenOne.getSecond(), this.parameterSupplier.get(), this.parameterSupplier.get());
    }

    private void disableMutation(MyController controller, Random random) {
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        controller.removeEdge(candidate);
    }

}
