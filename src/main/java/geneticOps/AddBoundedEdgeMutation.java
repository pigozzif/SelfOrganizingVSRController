package geneticOps;

import buildingBlocks.MyController;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;


public class AddBoundedEdgeMutation extends AddEdgeMutation {
    // TODO: maybe use validator object instead of string
    private final List<Pair<Integer, Integer>> module;

    public AddBoundedEdgeMutation(Supplier<Double> s, double p, String dist, String morph, String conf,
                                  List<Pair<Integer, Integer>> m) {
        super(s, p, dist, morph, conf);
        this.module = m;
    }

    @Override
    public void enableMutation(MyController controller, Random random) {
        List<Pair<Integer, Integer>> nodes = new ArrayList<>();
        for (MyController.Neuron n1 : controller.getNodeSet()) {
            for (MyController.Neuron n2 : controller.getNodeSet()) {
                if (!n2.isSensing() && !n1.isActuator() && MyController.euclideanDistance(n1, n2) <= this.maxDist && !n2.hasInNeighbour(n1) &&
                        n1.getIndex() != n2.getIndex() && TopologicalMutation.areCrossingModule(this.module, n1, n2)) {
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

    @Override
    public void disableMutation(MyController controller, Random random) {
        List<MyController.Edge> candidates = new ArrayList<>();
        for (MyController.Edge edge : controller.getEdgeSet()) {
            MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
            MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
            if (TopologicalMutation.areCrossingModule(this.module, source, target)) {
                candidates.add(edge);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        MyController.Edge candidate = candidates.get(random.nextInt(candidates.size()));
        controller.removeEdge(candidate);
    }

}
