package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;
import it.units.malelab.jgea.representation.sequence.numeric.GaussianMutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MutateEdge implements Mutation<MyController> {

    private final GaussianMutation mutation;

    public MutateEdge(double sigma) {
        this.mutation = new GaussianMutation(sigma);
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        List<MyController.Edge> edges = newBorn.getEdgeSet();
        MyController.Edge candidate = edges.get(random.nextInt(edges.size()));
        candidate.perturb(mutation.mutate(this.extractParams(candidate), random));
        //newBorn.getNodeSet().forEach(n -> n.getIngoingEdges().forEach(e -> e.perturb(mutation.mutate(this.extractParams(e), random))));
        return newBorn;
    }

    private List<Double> extractParams(MyController.Edge edge) {
        double[] params = edge.getParams();
        return new ArrayList<>() {{ add(params[0]); add(params[1]); }};
    }

}
