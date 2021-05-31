package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;
import org.apache.commons.math3.util.Pair;

import java.util.*;


public interface TopologicalMutation extends Mutation<MyController> {

    static double getMaxDist(String dist) {
        return switch (dist) {
            case "minimal" -> 0.0;
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

    static List<Pair<Integer, Integer>> selectBestModule(MyController controller, int minSize, int maxSize) {
        Pair<Integer, Integer>[] voxels = controller.getValidCoordinates();
        List<List<Pair<Integer, Integer>>> subsets = new ArrayList<>();
        for (int i=minSize; i <= maxSize; ++i) {
            subsets.addAll(subSets(voxels, i));
        }
        return subsets.stream().min(Comparator.comparingDouble(x -> countCrossingEdgesWeight(x, controller) / countNotCrossingEdgesWeight(x, controller))).get();
    }

    static <T> List<List<T>> subSets(T[] list, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i=0; i < list.length - size + 1; ++i) {
            List<T> subset = new ArrayList<>(Arrays.asList(list).subList(i, i + size - 1));
            if (!(size == 1 && i > 0)) {
                for (int j=i + size - 1;j < list.length; ++j) {
                    List<T> newSubset = new ArrayList<>(subset);
                    newSubset.add(list[j]);
                    if (isContigousSubset(newSubset.toArray(Pair[]::new))) {
                        out.add(newSubset);
                    }
                }
            }
        }
        return out;
    }

    static boolean isContigousSubset(Pair<Integer, Integer>[] subset) {
        Set<Pair<Integer, Integer>> visited = new HashSet<>();
        Queue<Pair<Integer, Integer>> queue = new LinkedList<>();
        Set<Pair<Integer, Integer>> toVisit = new HashSet<>(Arrays.asList(subset));
        queue.add(subset[0]);
        while (!queue.isEmpty()) {
            Pair<Integer, Integer> current = queue.remove();
            visited.add(current);
            int x1 = current.getFirst();
            int y1 = current.getSecond();
            for (int i : new int[]{1, -1}) {
                Pair<Integer, Integer> candidate = new Pair<>(x1, y1 + i);
                if (!visited.contains(candidate) && toVisit.contains(candidate)) {
                    queue.add(candidate);
                }
                candidate = new Pair<>(x1 + i, y1);
                if (!visited.contains(candidate) && toVisit.contains(candidate)) {
                    queue.add(candidate);
                }
            }
        }
        return visited.size() == subset.length;
    }

    static double countCrossingEdgesWeight(List<Pair<Integer, Integer>> subset, MyController controller) {
        double sum = 0.0;
        for (MyController.Edge edge : controller.getEdgeSet()) {
            MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
            MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
            Pair<Integer, Integer> sourcePair = new Pair<>(source.getX(), source.getY());
            Pair<Integer, Integer> targetPair = new Pair<>(target.getX(), target.getY());
            if ((subset.contains(sourcePair) && !subset.contains(targetPair)) ||
                    (subset.contains(targetPair) && !(subset.contains(sourcePair)))) {
                sum += Math.abs(edge.getParams()[0]) + Math.abs(edge.getParams()[1]);
            }
        }
        return sum;
    }

    static double countNotCrossingEdgesWeight(List<Pair<Integer, Integer>> subset, MyController controller) {
        double sum = 0.0;
        for (MyController.Edge edge : controller.getEdgeSet()) {
            MyController.Neuron source = controller.getNodeMap().get(edge.getSource());
            MyController.Neuron target = controller.getNodeMap().get(edge.getTarget());
            Pair<Integer, Integer> sourcePair = new Pair<>(source.getX(), source.getY());
            Pair<Integer, Integer> targetPair = new Pair<>(target.getX(), target.getY());
            if ((subset.contains(sourcePair) && subset.contains(targetPair))) {
                sum += Math.abs(edge.getParams()[0]) + Math.abs(edge.getParams()[1]);
            }
        }
        return sum;
    }

    static boolean areCrossingModule(List<Pair<Integer, Integer>> module, MyController.Neuron source, MyController.Neuron target) {
        Pair<Integer, Integer> sourcePair = new Pair<>(source.getX(), source.getY());
        Pair<Integer, Integer> targetPair = new Pair<>(target.getX(), target.getY());
        return (module.contains(sourcePair) && !module.contains(targetPair)) ||
                (module.contains(targetPair) && !module.contains(sourcePair));
    }

}
