package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;
import it.units.malelab.jgea.representation.sequence.numeric.GaussianMutation;

import java.util.Random;


public class MutateEdge implements Mutation<MyController> {

    private final GaussianMutation mutation;

    public MutateEdge(double sigma) {
        this.mutation = new GaussianMutation(sigma);
    }

    @Override
    public MyController mutate(MyController parent, Random random) {
        MyController newBorn = new MyController(parent);
        newBorn.getNodeSet().forEach(n -> n.getIngoingEdges().forEach(e -> e.perturb(mutation.mutate(e.getParams(), random))));
        return newBorn;
    }

}
