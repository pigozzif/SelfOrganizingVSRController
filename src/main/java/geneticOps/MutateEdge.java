package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;
import it.units.malelab.jgea.representation.sequence.numeric.GaussianMutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;


public class MutateEdge implements Mutation<MyController> {

    private final GaussianMutation mutation;
    private final double perc;
    private final int[] delayPicks = IntStream.rangeClosed(0, MyController.Edge.maxDelay).toArray();

    public MutateEdge(double sigma, double p) {
        this.mutation = new GaussianMutation(sigma);
        this.perc = p;
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        if (random.nextDouble() >= this.perc) {
            return this.perturbParameters(newBorn, random);
        }
        return this.perturbDelay(newBorn, random);
    }

    private MyController perturbParameters(MyController controller, Random random) {
        //controller.getNodeSet().forEach(n -> n.getIngoingEdges().forEach(e -> e.perturbParams(this.mutation.mutate(this.extractParams(e), random))));
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        candidate.perturbParams(mutation.mutate(this.extractParams(candidate), random));
        return controller;
    }

    private MyController perturbDelay(MyController controller, Random random) {
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        int d;
        do {
            d = this.delayPicks[random.nextInt(this.delayPicks.length)];
        } while (d == candidate.getDelay());
        candidate.perturbDelay(d);
        return controller;
    }

    private List<Double> extractParams(MyController.Edge edge) {
        double[] params = edge.getParams();
        return new ArrayList<>() {{ add(params[0]); add(params[1]); }};
    }

}
