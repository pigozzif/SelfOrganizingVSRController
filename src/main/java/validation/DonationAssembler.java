package validation;

import buildingBlocks.MyController;
import it.units.erallab.hmsrobots.util.Grid;

import java.util.*;
import java.util.stream.Collectors;


public class DonationAssembler implements Assembler {

    private final Map<Integer, MyController.Neuron> visitedNeurons;
    private Grid<Boolean> cuttingGrid;

    public DonationAssembler() {
        this.visitedNeurons = new HashMap<>();
    }

    @Override
    public MyController assemble(MyController controller1, MyController controller2, Grid<Boolean> grid) {
        this.cuttingGrid = grid;
        MyController hybrid = new MyController(controller1);
        this.visit(controller1, controller1.getNodeSet().stream().filter(n -> n.isSensing() && this.isToCut(n)).collect(Collectors.toSet()));
        this.fillFromReceiver(hybrid);
        this.visit(controller2, controller2.getNodeSet().stream().filter(n -> n.isSensing() && this.isToCut(n)).collect(Collectors.toSet()));
        this.fillFromDonator(hybrid, controller2);
        return hybrid;
    }

    private void fillFromDonator(MyController child, MyController parent) {
        Map<Integer, Integer> idxMap = new HashMap<>();
        for (MyController.Neuron neuron : this.visitedNeurons.values()) {
            idxMap.put(neuron.getIndex(), child.copyNeuron(neuron, false));
        }
        for (MyController.Edge edge : parent.getEdgeSet()) {
            if (this.visitedNeurons.containsKey(edge.getSource()) || this.visitedNeurons.containsKey(edge.getTarget())) {
                child.addEdge(idxMap.getOrDefault(edge.getSource(), edge.getSource()), idxMap.getOrDefault(edge.getTarget(), edge.getTarget()), edge.getParams()[0], edge.getParams()[1]);
            }
        }
        this.visitedNeurons.clear();
    }

    private void fillFromReceiver(MyController child) {
        for (MyController.Neuron neuron : this.visitedNeurons.values()) {
            child.removeNeuron(neuron);
        }
        for (MyController.Edge edge : child.getEdgeSet()) {
            if (this.visitedNeurons.containsKey(edge.getSource()) || this.visitedNeurons.containsKey(edge.getTarget())) {
                child.removeEdge(edge);
            }
        }
        child.resetIndexes();
        this.visitedNeurons.clear();
    }

    private void visit(MyController parent, Set<MyController.Neuron> frontier) {
        Queue<MyController.Neuron> neuronQueue = new LinkedList<>(frontier);
        Map<Integer, List<MyController.Edge>> outgoingEdges = parent.getOutgoingEdges();
        MyController.Neuron current;
        while (!neuronQueue.isEmpty()) {
            current = neuronQueue.remove();
            if (this.visitedNeurons.containsKey(current.getIndex()) || (!current.isHidden() && !this.isToCut(current))) {
                continue;
            }
            this.visitedNeurons.put(current.getIndex(), current);
            for (MyController.Edge edge : current.getIngoingEdges()) {
                neuronQueue.add(parent.getNodeMap().get(edge.getSource()));
            }
            for (MyController.Edge edge : outgoingEdges.getOrDefault(current.getIndex(), Collections.emptyList())) {
                neuronQueue.add(parent.getNodeMap().get(edge.getTarget()));
            }
        }
    }

    private boolean isToCut(MyController.Neuron neuron) {
        return this.cuttingGrid.get(neuron.getX(), neuron.getY());
    }

}
