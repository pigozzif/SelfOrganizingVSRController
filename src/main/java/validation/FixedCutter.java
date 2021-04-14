package validation;

import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;


public class FixedCutter implements Cutter {

    @Override
    public Grid<Boolean> cut(Grid<? extends SensingVoxel> body) {
        return Grid.create(body.getW(), body.getH(), (x, y) -> (x == 0 && y == 2) || (x == 0 && y == 1));
    }

}
