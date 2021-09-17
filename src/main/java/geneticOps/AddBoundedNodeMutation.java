package geneticOps;

import buildingBlocks.MyController;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;


public class AddBoundedNodeMutation extends AddNodeMutation {

    private final List<Pair<Integer, Integer>> module;

    public AddBoundedNodeMutation(Supplier<Double> sup, double p, String dist, String morph, String conf,
                                  List<Pair<Integer, Integer>> m) {
        super(sup, p, dist, morph, conf);
        this.module = m;
    }

    @Override
    public void enableMutation(MyController controller, int sampleX, int sampleY, Random random) {
        throw new UnsupportedOperationException("bounded node addition not supported");
    }

    @Override
    public void disableMutation(MyController controller, Random random) {
        List<MyController.Neuron> candidates = new ArrayList<>();
        for (MyController.Neuron neuron : controller.getNodeSet()) {
            if (!neuron.isHidden()) {
                continue;
            }
            for (MyController.Edge edge : neuron.getIngoingEdges()) {
                MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
                MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
                if (TopologicalMutation.areCrossingModule(this.module, source, target)) {
                    candidates.add(source);
                    candidates.add(target);
                    break;
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        MyController.Neuron candidate = candidates.get(random.nextInt(candidates.size()));
        controller.removeNeuron(candidate);
    }

}
