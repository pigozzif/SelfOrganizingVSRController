package geneticOps;

import buildingBlocks.MyController;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;


public class AddBoundedEdgeMutation extends AddEdgeMutation {

    private final List<Pair<Integer, Integer>> module;

    public AddBoundedEdgeMutation(Supplier<Double> s, double p, String dist, String morph, String conf,
                                  List<Pair<Integer, Integer>> m) {
        super(s, p, dist, morph, conf);
        this.module = m;
    }

    @Override
    public void enableMutation(MyController controller, Random random) {
        throw new UnsupportedOperationException("bounded edge addition not supported");
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
