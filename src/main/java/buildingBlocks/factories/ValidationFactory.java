package buildingBlocks.factories;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.Factory;
import it.units.malelab.jgea.core.operator.GeneticOperator;
import it.units.malelab.jgea.core.util.Misc;

import java.util.*;


public class ValidationFactory implements Factory<MyController> {

    private final double percMut;
    private final Map<GeneticOperator<MyController>, Double> operators;
    private final MyController individualSeed;
    private final Factory<MyController> randomInitializer;

    public ValidationFactory(double mut, Map<GeneticOperator<MyController>, Double> ops, MyController s, Factory<MyController> init) {
        if (ops.values().stream().mapToDouble(d -> d).sum() != 1.0) {
            throw new IllegalArgumentException("genetic operators' probabilities do not sum to 1.0");
        }
        this.percMut = mut;
        this.operators = ops;
        this.individualSeed = s;
        this.randomInitializer = init;
    }

    @Override
    public List<MyController> build(int n, Random random) {
        List<MyController> pop = new ArrayList<>();
        pop.add(this.individualSeed);
        for (int i=0; i < Math.floor(n * this.percMut); ++i) {
            pop.add(Misc.pickRandomly(this.operators, random).apply(List.of(this.individualSeed), random).get(0));
        }
        pop.addAll(this.randomInitializer.build(n - pop.size(), random));
        return pop;
    }

}
