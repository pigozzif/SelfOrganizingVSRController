package validation;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.util.Grid;

@FunctionalInterface
public interface Assembler {

    MyController assemble(MyController controller1, MyController controller2, Grid<Boolean> grid);

    static Assembler createAssembler(String name) {
        if (name.equals("donation")) {
            return new DonationAssembler();
        }
        else {
            throw new RuntimeException(String.format("Unavailable validation strategy: %s", name));
        }
    }

}
