package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;
import it.units.malelab.jgea.representation.sequence.numeric.GaussianMutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

// when evolving also activation, better to perturb one edge at a time. If not, better to
// perturb all edges together
public class MutateEdge implements Mutation<MyController> {

    private final GaussianMutation mutation;
    private final double perc;

    public MutateEdge(double sigma, double p) {
        this.mutation = new GaussianMutation(sigma);
        this.perc = p;
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        if (random.nextDouble() >= this.perc) {
            this.perturbParameters(newBorn, random);
        }
        else {
            this.perturbDelay(newBorn, random);
        }
        return newBorn;
    }

    private void perturbParameters(MyController controller, Random random) {
        controller.getNodeSet().forEach(n -> n.getIngoingEdges().forEach(e -> e.perturbParams(this.mutation.mutate(this.extractParams(e), random))));
        //List<MyController.Edge> edges = controller.getEdgeSet();
        //MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        //candidate.perturbParams(mutation.mutate(this.extractParams(candidate), random));
    }

    private void perturbDelay(MyController controller, Random random) {
        List<MyController.Edge> edges = controller.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        int d;
        int oldD = candidate.getDelay();
        if (oldD == 0) {
            d = 1;
        }
        else if (oldD == 9) {
            d = 8;
        }
        else {
            d = (random.nextBoolean()) ? oldD + 1 : oldD - 1;
        }
        candidate.perturbDelay(d);
    }

    private List<Double> extractParams(MyController.Edge edge) {
        double[] params = edge.getParams();
        return new ArrayList<>() {{ add(params[0]); add(params[1]); }};
    }

}
