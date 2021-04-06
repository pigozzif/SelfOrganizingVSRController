package buildingBlocks;

import it.units.malelab.jgea.core.IndependentFactory;
import morphologies.Morphology;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.*;
import java.util.function.Supplier;


public class ControllerFactory implements IndependentFactory<MyController> {

    private final Supplier<Double> parameterSupplier;
    private final double fillPerc;
    private final Morphology morphology;

    public ControllerFactory(Supplier<Double> s, double p, Morphology morph) {
        this.parameterSupplier = s;
        this.fillPerc = p;
        this.morphology = morph;
    }

    @Override
    public MyController build(Random random) {
        MyController controller = new MyController(Collections.emptyList());
        for (Grid.Entry<? extends SensingVoxel> voxel : this.morphology.getBody()) {
            if (voxel.getValue() == null) {
                continue;
            }
            int x = voxel.getX();
            int y = voxel.getY();
            for (int i = 0; i < this.morphology.getNumSensors()[MyController.flattenCoord(x, y, this.morphology.getBody().getW())]; ++i) {
                controller.addNode(MultiLayerPerceptron.ActivationFunction.TANH, MyController.NodeType.SENSING, x, y, i);
            }
            controller.addNode(MultiLayerPerceptron.ActivationFunction.TANH, MyController.NodeType.ACTUATOR, x, y);
        }
        for (MyController.Neuron n1 : controller.getNodeSet()) {
            for (MyController.Neuron n2 : controller.getNodeSet()) {
                if (n1.getType() == MyController.NodeType.SENSING && n2.getType() == MyController.NodeType.ACTUATOR &&
                                n1.getX() == n2.getX() && n1.getY() == n2.getY() && random.nextDouble() < this.fillPerc) {
                    controller.addEdge(n1.getIndex(), n2.getIndex(), this.parameterSupplier.get(), this.parameterSupplier.get());
                }
            }
        }
        return controller;
    }

}
