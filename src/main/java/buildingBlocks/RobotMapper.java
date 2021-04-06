package buildingBlocks;

import it.units.erallab.hmsrobots.util.SerializationUtils;
import morphologies.Morphology;
import it.units.erallab.hmsrobots.core.objects.Robot;
import java.util.function.Function;


public class RobotMapper implements Function<MyController, Robot<?>> {

    private final Morphology morphology;

    public RobotMapper(Morphology morph) {
        this.morphology = morph;
    }

    @Override
    public Robot<?> apply(MyController genotype) {
        return new Robot<>(genotype, SerializationUtils.clone(this.morphology.getBody()));
    }

}
