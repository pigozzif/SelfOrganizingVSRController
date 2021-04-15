package validation;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.Random;

@FunctionalInterface
public interface Assembler {

    MyController assemble(MyController controller1, MyController controller2, Grid<Boolean> grid, Random random);

    static Assembler createAssembler(String name) {
        if (name.equals("growing")) {
            return new DonationAssembler();
        }
        else if (name.equals("rewiring")) {
            return new RewiringAssembler();
        }
        else {
            throw new RuntimeException(String.format("Unknown assembling strategy: %s", name));
        }
    }

}
