package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Mutation;


public interface TopologicalMutation extends Mutation<MyController> {

    static double getMaxDist(String dist) {
        return switch (dist) {
            case "minimal" -> 1.0;
            case "full" -> Double.MAX_VALUE;
            default -> throw new IllegalArgumentException("Connectivity not known: " + dist);
        };
    }

}
