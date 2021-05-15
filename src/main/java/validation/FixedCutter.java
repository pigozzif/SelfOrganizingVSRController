package validation;

import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;


public class FixedCutter implements Cutter {

    @Override
    public Grid<Boolean> cut(Grid<? extends SensingVoxel> body) {
        if (body.getW() == 6 && body.getH() == 2) {
            return Grid.create(body.getW(), body.getH(), (x, y) -> x <= 2);
        }
        else if (body.getW() == 4 && body.getH() == 3) {
            return Grid.create(body.getW(), body.getH(), (x, y) -> (x == 0) || (x == 3));
        }
        else {
            throw new RuntimeException("Illegal shape provided: (" + body.getW() + "," + body.getH() + ")");
        }
    }

}
