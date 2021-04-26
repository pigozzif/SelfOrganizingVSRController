package buildingBlocks.factories;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.IndependentFactory;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;


public class ControllerFactory implements IndependentFactory<MyController> {

    private final Supplier<Double> parameterSupplier;
    private final double fillPerc;
    private final Grid<? extends SensingVoxel> body;
    private final int[] sensors;
    private final BiPredicate<MyController.Neuron, MyController.Neuron> posFilter;

    public ControllerFactory(Supplier<Double> s, double perc, Grid<? extends SensingVoxel> b, int[] sen, BiPredicate<MyController.Neuron, MyController.Neuron> filter) {
        this.parameterSupplier = s;
        this.fillPerc = perc;
        this.body = b;
        this.sensors = sen;
        this.posFilter = filter;
    }

    @Override
    public MyController build(Random random) {
        MyController controller = new MyController(Collections.emptyMap());
        for (Grid.Entry<? extends SensingVoxel> voxel : this.body) {
            if (voxel.getValue() == null) {
                continue;
            }
            int x = voxel.getX();
            int y = voxel.getY();
            for (int i = 0; i < this.sensors[MyController.flattenCoord(x, y, this.body.getW())]; ++i) {
                controller.addSensingNode(x, y, i);
            }
            controller.addActuatorNode(x, y);
        }
        this.initInVoxelEdges(controller, random);
        return controller;
    }

    private void initInVoxelEdges(MyController controller, Random random) {
        for (MyController.Neuron n1 : controller.getNodeSet()) {
            for (MyController.Neuron n2 : controller.getNodeSet()) {
                if (n1.isSensing() && n2.isActuator() &&
                        this.posFilter.test(n1, n2) && random.nextDouble() < this.fillPerc) {
                    controller.addEdge(n1.getIndex(), n2.getIndex(), this.parameterSupplier.get(), this.parameterSupplier.get());
                }
            }
        }
    }

}
