package validation;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.util.Grid;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// TODO: decide whether reinitialize rewired weights or keep them
public class RewiringAssembler implements Assembler {
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

    private final Map<Integer, MyController.Neuron> visitedNeurons;
    private Grid<Boolean> cuttingGrid;
    private final List<CrossingEdge> crossingEdges;

    public RewiringAssembler() {
        this.visitedNeurons = new HashMap<>();
        this.crossingEdges = new ArrayList<>();
    }

    @Override
    public MyController assemble(MyController controller1, MyController controller2, Grid<Boolean> grid, Random random) {
        this.cuttingGrid = grid;
        controller1.resetIndexes();
        controller2.resetIndexes();
        MyController hybrid = new MyController(controller1);
        this.visitFast(hybrid, this::isToCut);
        this.cutFromReceiver(hybrid, controller1);
        this.visitFast(controller2, this::isToCut);
        this.moveFromDonator(hybrid, controller2);
        this.reWire(hybrid, random);
        return hybrid;
    }

    private void cutFromReceiver(MyController hybrid, MyController receiver) {
        for (MyController.Edge edge : hybrid.getEdgeSet()) {
            boolean whatAboutSource = this.visitedNeurons.containsKey(edge.getSource());
            boolean whatAboutTarget = this.visitedNeurons.containsKey(edge.getTarget());
            if (whatAboutSource && whatAboutTarget) {
                hybrid.removeEdge(edge);
            }
            else if (whatAboutSource || whatAboutTarget) {
                MyController.Neuron source = receiver.getNodeMap().get(edge.getSource());
                MyController.Neuron target = receiver.getNodeMap().get(edge.getTarget());
                this.crossingEdges.add(new CrossingEdge(source.isSensing(), source.isActuator(), target.isSensing(), target.isActuator(),
                        source.getX(), source.getY(), target.getX(), target.getY(), edge.getParams()[0], edge.getParams()[1]));
            }
        }
        for (MyController.Neuron neuron : this.visitedNeurons.values()) {
            if (neuron.isHidden()) {
                hybrid.removeNeuron(neuron);
            }
        }
        this.visitedNeurons.clear();
    }

    private void moveFromDonator(MyController hybrid, MyController donator) {
        Map<Integer, Integer> fromOldToNew = new HashMap<>();
        for (Map.Entry<Integer, MyController.Neuron> entry : this.visitedNeurons.entrySet()) {
            if (entry.getValue().isHidden()) {
                fromOldToNew.put(entry.getKey(), hybrid.copyNeuron(entry.getValue(), false));
            }
            else {
                fromOldToNew.put(entry.getKey(), entry.getKey());
            }
        }
        for (MyController.Edge edge : donator.getEdgeSet()) {
            boolean whatAboutSource = this.visitedNeurons.containsKey(edge.getSource());
            boolean whatAboutTarget = this.visitedNeurons.containsKey(edge.getTarget());
            if (whatAboutSource && whatAboutTarget) {
                edge.setSource(fromOldToNew.get(edge.getSource()));
                edge.setTarget(fromOldToNew.get(edge.getTarget()));
                hybrid.copyEdge(edge);
            }
            else if (whatAboutSource || whatAboutTarget) {
                MyController.Neuron source = donator.getNodeMap().get(edge.getSource());
                MyController.Neuron target = donator.getNodeMap().get(edge.getTarget());
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
            if (sourceCandidates.isEmpty() || targetCandidates.isEmpty()) {
                continue;
            }
            MyController.Neuron source = sourceCandidates.get(random.nextInt(sourceCandidates.size()));
            MyController.Neuron target = targetCandidates.get(random.nextInt(targetCandidates.size()));
            hybrid.addEdge(source.getIndex(), target.getIndex(), edge.weight, edge.bias);
        }
    }

    private void visitFast(MyController controller, Predicate<MyController.Neuron> filterCondition) {
        this.visitedNeurons.putAll(controller.getNodeMap().entrySet().stream().filter(e -> filterCondition.test(e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private boolean isToCut(MyController.Neuron neuron) {
        return this.cuttingGrid.get(neuron.getX(), neuron.getY());
    }

}
