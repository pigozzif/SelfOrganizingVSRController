package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.GeneticOperator;
import it.units.malelab.jgea.core.operator.Mutation;
import it.units.malelab.jgea.core.util.Misc;

import java.util.*;
import java.util.function.Supplier;


public class TimedOperator implements GeneticOperator<MyController> {

    private final int time;
    private final Map<Mutation<MyController>, Double> priorMap;
    private final Map<Mutation<MyController>, Double> postMap;

    public TimedOperator(int t, Supplier<Double> parameterSupplier, String shape, boolean postTopological) {
        this.time = t;
        this.priorMap = Map.of(new AddNodeMutation(parameterSupplier, 0.5, "full", shape, "minimal"), 0.3,
                new AddEdgeMutation(parameterSupplier, 0.5, "full", shape, "minimal"), 0.2,
                new MutateEdge(0.7, 0.0), 0.5);
        if (postTopological) {
            this.postMap = Map.of(new AddNodeMutation(parameterSupplier, 0.5, "full", shape, "minimal"), 0.6,
                    new AddEdgeMutation(parameterSupplier, 0.5, "full", shape, "minimal"), 0.4);
        }
        else {
            this.postMap = Map.of(new MutateEdge(0.7, 0.0), 1.0);
        }
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public List<? extends MyController> apply(List<? extends MyController> parents, Random random) {
        int t = parents.get(0).getIteration();
        if (t < this.time) {
            return Misc.pickRandomly(this.priorMap, random).apply(parents, random);
        }
        else {
            return Misc.pickRandomly(this.postMap, random).apply(parents, random);
        }
    }

}
