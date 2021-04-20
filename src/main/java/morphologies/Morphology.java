package morphologies;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.core.sensors.*;
import it.units.erallab.hmsrobots.util.Grid;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.erallab.hmsrobots.util.SerializationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


public class Morphology {

    public static class Pair {

        public final int first;
        public final int second;

        public Pair(int x, int y) {
            this.first = x;
            this.second = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return first == pair.first && second == pair.second;
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

    }

    protected final Grid<? extends SensingVoxel> body;
    protected final int[] sensors;
    protected final List<Pair> allowedMorph;

    public Morphology(int w, int h, String shape, String sensorConfig) {
        Grid<Boolean> booleanGrid = RobotUtils.buildShape(shape);
        this.body = sensorsFactory(sensorConfig).apply(booleanGrid);
        sensors = new int[this.body.getH() * this.body.getW()];
        for (Grid.Entry<? extends SensingVoxel> voxel : this.body) {
            int coord = MyController.flattenCoord(voxel.getX(), voxel.getY(), this.body.getW());
            if (voxel.getValue() == null) {
                sensors[coord] = -1;
            }
            else {
                sensors[coord] = voxel.getValue().getSensors().stream().mapToInt(s -> s.domains().length).sum();
            }
        }
        this.allowedMorph = this.body.stream().filter(v -> v.getValue() != null).map(v -> new Pair(v.getX(), v.getY())).collect(Collectors.toList());
    }

    public void fillBody() {}

    public Grid<? extends SensingVoxel> getBody() { return this.body; }

    public List<Pair> getAllowedMorph() { return this.allowedMorph; }

    public int[] getNumSensors() {
        return this.sensors;
    }
    // TODO: this function has lot of room for improvement
    public static Function<Grid<Boolean>, Grid<? extends SensingVoxel>> sensorsFactory(String config) {
        if (config.equals("vel-area-touch")) {
            return b -> {List<Sensor> sensors = new ArrayList<>() {{
                add(new Normalization(new Velocity(true, 5d, Velocity.Axis.X, Velocity.Axis.Y)));
                add(new Normalization(new AreaRatio()));
                add(new Normalization(new Average(new Touch(), 0.5D)));
            }};
            SensingVoxel voxel = new SensingVoxel(sensors);
            Grid<SensingVoxel> grid = Grid.create(b.getW(), b.getH());
            b.stream().filter(Grid.Entry::getValue).forEach(v -> grid.set(v.getX(), v.getY(), SerializationUtils.clone(voxel)));
            return grid; };
        }
        else if (config.equals("const")) {
            return b -> {List<Sensor> sensors = new ArrayList<>() {{
                add(new TimeFunction(x -> 1.0, 0.0, 1.0));
            }};
                SensingVoxel voxel = new SensingVoxel(sensors);
                Grid<SensingVoxel> grid = Grid.create(b.getW(), b.getH());
                b.stream().filter(Grid.Entry::getValue).forEach(v -> grid.set(v.getX(), v.getY(), SerializationUtils.clone(voxel)));
                return grid; };
        }
        else {
            return RobotUtils.buildSensorizingFunction(config);
        }
    }
    // TODO: decide whether to move to factory class
    public static Morphology createMorphology(String name, String sensorConfig) {
        switch (name) {
            case "worm-small":
                return new WormMorphology(4, 1, sensorConfig);
            case "worm-large":
                return new WormMorphology(11, 4, sensorConfig);
            case "biped-small":
                return new BipedMorphology(4, 2, sensorConfig, 1);
            case "biped-large":
                return new BipedMorphology(11, 4, sensorConfig, 2);
            case "triped-small":
                return new TripedMorphology(5, 2, sensorConfig, 1);
            case "triped-large":
                return new TripedMorphology(11, 4, sensorConfig, 2);
        }
        throw new RuntimeException("Morphology " + name + " not available!");
    }

}
