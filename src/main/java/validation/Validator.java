package validation;

import buildingBlocks.MyController;
import org.apache.commons.math3.util.Pair;

import java.util.Set;

@FunctionalInterface
public interface Validator {

    MyController apply(MyController individual1, MyController individual2, Set<Pair<Integer, Integer>> voxelsFrom2);

    static Validator createValidator(String strategy) {
        if (strategy.equals("donation")) {
            return new DonationValidator();
        }
        throw new RuntimeException("Unavailable validation strategy: " + strategy);
    }

}
