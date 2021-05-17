package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface TopologicalMutation extends Mutation<MyController> {

    static double getMaxDist(String dist) {
        return switch (dist) {
            case "minimal" -> 1.0;
            case "full" -> Double.MAX_VALUE;
            default -> throw new IllegalArgumentException("Connectivity not known: " + dist);
        };
    }

    static boolean isNotCrossingEdge(String morph, MyController.Neuron neuron1, MyController.Neuron neuron2) {
        int x1 = neuron1.getX();
        int x2 = neuron2.getX();
        return switch (morph) {
            case "worm-5x1" -> false;
            case "worm-6x2" -> (x1 <= 2 && x2 <= 2) || (x1 > 2 && x2 > 2);
            case "biped-4x3" -> (x1 == 0 && x2 == 0) || (x1 == 3 && x2 == 3) || (x1 != 0 && x1 != 3 && x2 != 0 && x2 != 3);
            default -> throw new IllegalArgumentException("Morphology not known: " + morph);
        };
    }

    static double[] getEdgeProbs(String conf) {
        return switch (conf) {
            case "minimal" -> new double[] {1.0, 1.0};
            case "unmodular" -> new double[] {1.0, 10.0};
            case "modular" -> new double[] {10.0, 1.0};
            default -> throw new IllegalArgumentException("Configuration not known: " + conf);
        };
    }

    static void pruneIsolatedNeurons(MyController controller) {
        Map<Integer, List<MyController.Edge>> outgoingEdges = controller.getOutgoingEdges();
        Set<MyController.Neuron> visitedNeurons = new HashSet<>();
        for (MyController.Neuron neuron : controller.getNodeSet()) {
            if (neuron.isHidden() && neuron.getIngoingEdges().isEmpty() &&
                    !outgoingEdges.containsKey(neuron.getIndex())) {
                visitedNeurons.add(neuron);
            }
        }
        visitedNeurons.forEach(controller::removeNeuron);
    }

}
