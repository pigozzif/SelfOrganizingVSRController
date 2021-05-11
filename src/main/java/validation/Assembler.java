package validation;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.Random;

@FunctionalInterface
public interface Assembler {

    MyController assemble(MyController controller1, MyController controller2, Grid<Boolean> grid, Random random);

    static Assembler createAssembler(String name) {
        return switch (name) {
            case "growing" -> new DonationAssembler();
            case "rewiring" -> new RewiringAssembler();
            case "identity" -> (c1, c2, g, r) -> c1;
            default -> throw new RuntimeException(String.format("Unknown assembling strategy: %s", name));
        };
    }

}
