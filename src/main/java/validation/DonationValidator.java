package validation;

import buildingBlocks.MyController;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;


public class DonationValidator implements Validator {

    private final Map<Integer, MyController.Neuron> visitedNeurons;
    private final Set<Pair<Integer, Integer>> voxelsToCut;

    public DonationValidator() {
        this.visitedNeurons = new HashMap<>();
        this.voxelsToCut = new HashSet<>();
    }

    @Override
    public MyController apply(MyController individual1, MyController individual2, Set<Pair<Integer, Integer>> voxelsFrom2) {
        this.voxelsToCut.addAll(voxelsFrom2);
        MyController hybrid = new MyController(individual1);
        this.visit(individual1, individual1.getNodeSet().stream().filter(n -> n.isSensing() && this.isToCut(n)).collect(Collectors.toSet()));
        this.fillFromReceiver(hybrid);
        this.visit(individual2, individual2.getNodeSet().stream().filter(n -> n.isSensing() && this.isToCut(n)).collect(Collectors.toSet()));
        this.fillFromDonator(hybrid, individual2);
        this.voxelsToCut.clear();
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
        return this.voxelsToCut.contains(new Pair<>(neuron.getX(), neuron.getY()));
    }

}
