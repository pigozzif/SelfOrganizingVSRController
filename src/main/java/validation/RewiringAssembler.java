package validation;

import buildingBlocks.MyController;
import geneticOps.TopologicalMutation;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class RewiringAssembler implements Assembler {

    private static class CrossingEdge {

        public final MyController.Edge edge;
        public final MyController.Neuron source;
        public final MyController.Neuron target;
        public final boolean isFromDonator;

        public CrossingEdge(MyController.Edge e, MyController.Neuron n1, MyController.Neuron n2, boolean donator) {
            this.edge = e;
            this.source = n1;
            this.target = n2;
            this.isFromDonator = donator;
        }

    }

    private final Map<Integer, MyController.Neuron> visitedNeurons;
    private Grid<Boolean> cuttingGrid;
    private final List<CrossingEdge> crossingEdges;
    private final Supplier<Double> parameterSupplier;
    private final String shape;

    public RewiringAssembler(String shape, Supplier<Double> supplier) {
        this.visitedNeurons = new HashMap<>();
        this.crossingEdges = new ArrayList<>();
        this.parameterSupplier = supplier;
        this.shape = shape;
    }

    public RewiringAssembler(String shape) {
        this(shape, null);
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
        this.pruneIsolatedNeurons(hybrid);
        //this.reWire(hybrid, random);
        return hybrid;
    }

    private void cutFromReceiver(MyController hybrid, MyController receiver) {
        for (MyController.Edge edge : hybrid.getEdgeSet()) {
            MyController.Neuron source = receiver.getNodeMap().get(edge.getSource());
            MyController.Neuron target = receiver.getNodeMap().get(edge.getTarget());
            boolean whatAboutSource = this.visitedNeurons.containsKey(edge.getSource());
            boolean whatAboutTarget = this.visitedNeurons.containsKey(edge.getTarget());
            if (whatAboutSource && whatAboutTarget && TopologicalMutation.isNotCrossingEdge(shape, source, target)) {
                hybrid.removeEdge(edge);
            }
            else if (whatAboutSource || whatAboutTarget) {
                hybrid.removeEdge(edge);
                this.crossingEdges.add(new CrossingEdge(edge, source, target, false));
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
            MyController.Neuron source = donator.getNodeMap().get(edge.getSource());
            MyController.Neuron target = donator.getNodeMap().get(edge.getTarget());
            boolean whatAboutSource = this.visitedNeurons.containsKey(edge.getSource());
            boolean whatAboutTarget = this.visitedNeurons.containsKey(edge.getTarget());
            if (whatAboutSource && whatAboutTarget && TopologicalMutation.isNotCrossingEdge(shape, source, target)) {
                edge.setSource(fromOldToNew.get(edge.getSource()));
                edge.setTarget(fromOldToNew.get(edge.getTarget()));
                hybrid.copyEdge(edge);
            }
            else if (whatAboutSource || whatAboutTarget) {
                this.crossingEdges.add(new CrossingEdge(edge, source, target, true));
            }
        }
        this.visitedNeurons.clear();
    }

    private void reWire(MyController hybrid, Random random) {
        for (CrossingEdge crossingEdge : this.crossingEdges) {
            List<MyController.Neuron> sourceCandidates;
            List<MyController.Neuron> targetCandidates;
            if (crossingEdge.isFromDonator) {
                sourceCandidates = List.of(crossingEdge.source);
                targetCandidates = hybrid.getNodeSet().stream().filter(n -> n.isSensing() == crossingEdge.target.isSensing() &&
                        n.isActuator() == crossingEdge.target.isActuator() && n.getX() == crossingEdge.target.getX() && n.getY() == crossingEdge.target.getY()).collect(Collectors.toList());
            }
            else {
                sourceCandidates = hybrid.getNodeSet().stream().filter(n -> n.isSensing() == crossingEdge.source.isSensing() &&
                        n.isActuator() == crossingEdge.source.isActuator() && n.getX() == crossingEdge.source.getX() && n.getY() == crossingEdge.source.getY()).collect(Collectors.toList());
                targetCandidates = List.of(crossingEdge.target);
            }
            if (sourceCandidates.isEmpty() || targetCandidates.isEmpty()) {
                continue;
            }
            MyController.Neuron source = sourceCandidates.get(random.nextInt(sourceCandidates.size()));
            MyController.Neuron target = targetCandidates.get(random.nextInt(targetCandidates.size()));
            if (this.parameterSupplier == null) {
                hybrid.addEdge(source.getIndex(), target.getIndex(), crossingEdge.edge.getParams()[0], crossingEdge.edge.getParams()[1]);
            }
            else {
                hybrid.addEdge(source.getIndex(), target.getIndex(), this.parameterSupplier.get(), this.parameterSupplier.get());
            }
        }
        this.crossingEdges.clear();
    }

    private void visitFast(MyController controller, Predicate<MyController.Neuron> filterCondition) {
        this.visitedNeurons.putAll(controller.getNodeMap().entrySet().stream().filter(e -> filterCondition.test(e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private boolean isToCut(MyController.Neuron neuron) {
        return this.cuttingGrid.get(neuron.getX(), neuron.getY());
    }

    private void pruneIsolatedNeurons(MyController controller) {
        Map<Integer, List<MyController.Edge>> outgoingEdges = controller.getOutgoingEdges();
        this.visitedNeurons.clear();
        for (MyController.Neuron neuron : controller.getNodeSet()) {
            if (neuron.isHidden() && neuron.getIngoingEdges().isEmpty() &&
                    !outgoingEdges.containsKey(neuron.getIndex())) {
                this.visitedNeurons.put(neuron.getIndex(), neuron);
            }
        }
        this.visitedNeurons.values().forEach(controller::removeNeuron);
        this.visitedNeurons.clear();
    }

}
