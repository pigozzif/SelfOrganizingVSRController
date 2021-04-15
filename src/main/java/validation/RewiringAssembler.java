package validation;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

// TODO: decide whether reinitialize rewired weights or keep them
public class RewiringAssembler extends DonationAssembler {
    // TODO: a little bit big this class
    private static class CrossingEdge {

        public final boolean isSourceSensing;
        public final boolean isSourceActuator;
        public final boolean isTargetSensing;
        public final boolean isTargetActuator;
        public final Pair<Integer, Integer> sourceLoc;
        public final Pair<Integer, Integer> targetLoc;
        private final double weight;
        private final double bias;

        public CrossingEdge(boolean ssen, boolean sact, boolean tsen, boolean tact, int sx, int sy, int tx, int ty, double w, double b) {
            this.isSourceSensing = ssen;
            this.isSourceActuator = sact;
            this.isTargetSensing = tsen;
            this.isTargetActuator = tact;
            this.sourceLoc = new Pair<>(sx, sy);
            this.targetLoc = new Pair<>(tx, ty);
            this.weight = w;
            this.bias = b;
        }

    }

    private final List<CrossingEdge> crossingEdges;

    public RewiringAssembler() {
        super();
        this.crossingEdges = new ArrayList<>();
    }

    @Override
    public MyController assemble(MyController controller1, MyController controller2, Grid<Boolean> grid, Random random) {
        this.cuttingGrid = grid;
        MyController hybrid = new MyController(controller1);
        this.visit(hybrid, this.getSensingNeuronsFromVoxel(hybrid), x -> !this.isToCut(x));
        this.cutFromReceiver(hybrid);
        this.visit(controller2, this.getSensingNeuronsFromVoxel(controller2), x -> !this.isToCut(x));
        this.moveFromDonator(hybrid, controller2);
        this.reWire(hybrid, random);
        return hybrid;
    }

    private void cutFromReceiver(MyController hybrid) {
        for (MyController.Neuron neuron : this.visitedNeurons.values()) {
            hybrid.removeNeuron(neuron);
        }
        for (MyController.Edge edge : hybrid.getEdgeSet()) {
            boolean whatAboutSource = this.visitedNeurons.containsKey(edge.getSource());
            boolean whatAboutTarget = this.visitedNeurons.containsKey(edge.getTarget());
            if (whatAboutSource && whatAboutTarget) {
                hybrid.removeEdge(edge);
            }
            else if (whatAboutSource || whatAboutTarget) {
                MyController.Neuron source = this.visitedNeurons.get(edge.getSource());
                MyController.Neuron target = this.visitedNeurons.get(edge.getTarget());
                this.crossingEdges.add(new CrossingEdge(source.isSensing(), source.isActuator(), target.isSensing(), target.isActuator(),
                        source.getX(), source.getY(), target.getX(), target.getY(), edge.getParams()[0], edge.getParams()[1]));
            }
        }
        this.visitedNeurons.clear();
    }

    private void moveFromDonator(MyController hybrid, MyController donator) {
        for (Map.Entry<Integer, MyController.Neuron> entry : this.visitedNeurons.entrySet()) {
            if (!hybrid.getNodeMap().containsKey(entry.getKey())) {
                hybrid.copyNeuron(entry.getValue(), false);
            }
        }
        for (MyController.Edge edge : donator.getEdgeSet()) {
            boolean whatAboutSource = this.visitedNeurons.containsKey(edge.getSource());
            boolean whatAboutTarget = this.visitedNeurons.containsKey(edge.getTarget());
            if (whatAboutSource && whatAboutTarget) {
                hybrid.copyEdge(edge);
            }
            else if (whatAboutSource || whatAboutTarget) {
                MyController.Neuron source = this.visitedNeurons.get(edge.getSource());
                MyController.Neuron target = this.visitedNeurons.get(edge.getTarget());
                this.crossingEdges.add(new CrossingEdge(source.isSensing(), source.isActuator(), target.isSensing(), target.isActuator(),
                        source.getX(), source.getY(), target.getX(), target.getY(), edge.getParams()[0], edge.getParams()[1]));
            }
        }
        this.visitedNeurons.clear();
    }

    private void reWire(MyController hybrid, Random random) {
        for (CrossingEdge edge : this.crossingEdges) {
            List<MyController.Neuron> sourceCandidates = hybrid.getNodeSet().stream().filter(n -> n.isSensing() == edge.isSourceSensing &&
                    n.isActuator() == edge.isSourceActuator && n.getX() == edge.sourceLoc.getFirst() && n.getY() == edge.sourceLoc.getSecond()).collect(Collectors.toList());
            List<MyController.Neuron> targetCandidates = hybrid.getNodeSet().stream().filter(n -> n.isSensing() == edge.isTargetSensing &&
                    n.isActuator() == edge.isTargetActuator && n.getX() == edge.targetLoc.getFirst() && n.getY() == edge.targetLoc.getSecond()).collect(Collectors.toList());
            MyController.Neuron source = sourceCandidates.get(random.nextInt(sourceCandidates.size()));
            MyController.Neuron target = targetCandidates.get(random.nextInt(targetCandidates.size()));
            if (source != null && target != null) {
                hybrid.addEdge(source.getIndex(), target.getIndex(), edge.weight, edge.bias);
            }
        }
    }

}
