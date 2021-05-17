package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.util.Misc;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;


public class AddEdgeMutation implements TopologicalMutation {
    // TODO: maybe use validator object instead of string
    private final Supplier<Double> parameterSupplier;
    private final double perc;
    private final double maxDist;
    private final String morphology;
    private final String configuration;

    public AddEdgeMutation(Supplier<Double> s, double p, String dist, String morph, String conf) {
        this.parameterSupplier = s;
        this.perc = p;
        this.maxDist = TopologicalMutation.getMaxDist(dist);
        this.morphology = morph;
        this.configuration = conf;
    }

    public AddEdgeMutation(Supplier<Double> s) {
        this(s, 1.0, "minimal", "worm-5x1", "minimal");
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        if (random.nextDouble() <= this.perc) {
            this.enableMutation(newBorn, random);
            newBorn.setOrigin("add_edge");
        }
        else {
            this.disableMutation(newBorn, random);
            TopologicalMutation.pruneIsolatedNeurons(newBorn);
            newBorn.setOrigin("remove_edge");
        }
        return newBorn;
    }

    private void enableMutation(MyController controller, Random random) {
        Map<Pair<Integer, Integer>, Double> nodes = new HashMap<>();
        double[] probs = TopologicalMutation.getEdgeProbs(this.configuration);
        for (MyController.Neuron n1 : controller.getNodeSet()) {
            for (MyController.Neuron n2 : controller.getNodeSet()) {
                if (!n2.isSensing() && !n1.isActuator() && MyController.euclideanDistance(n1, n2) <= this.maxDist && !n2.hasInNeighbour(n1) && n1.getIndex() != n2.getIndex()) {
                    double weight = (TopologicalMutation.isNotCrossingEdge(this.morphology, n1, n2)) ? probs[0] : probs[1];
                    nodes.put(new Pair<>(n1.getIndex(), n2.getIndex()), weight);
                }
            }
        }
        if (nodes.isEmpty()) {
            return;
        }
        Pair<Integer, Integer> chosenOne = Misc.pickRandomly(nodes, random);//nodes.get(random.nextInt(nodes.size()));
        controller.addEdge(chosenOne.getFirst(), chosenOne.getSecond(), this.parameterSupplier.get(), this.parameterSupplier.get());
    }

    private void disableMutation(MyController controller, Random random) {
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        controller.removeEdge(candidate);
    }

}
