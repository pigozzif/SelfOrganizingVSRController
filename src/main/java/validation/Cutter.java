package validation;

import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;

@FunctionalInterface
public interface Cutter {

    Grid<Boolean> cut(Grid<? extends SensingVoxel> body);

    static Cutter createCutter(String name) {
        if (name.equals("fixed")) {
            return new FixedCutter();
        }
        else {
            throw new IllegalArgumentException(String.format("Unknown cutting strategy: %s", name));
        }
    }

}
