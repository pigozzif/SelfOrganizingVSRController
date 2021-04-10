package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Crossover;

import java.util.*;
import java.util.stream.Collectors;


public class CrossoverWithDonation implements Crossover<MyController> {

    @Override
    public MyController recombine(MyController parent1, MyController parent2, Random random) {
        MyController newBorn = new MyController(Collections.emptyMap());
        int sampleX = parent1.getValidXCoordinates()[random.nextInt(parent1.getValidXCoordinates().length)];
        int sampleY = parent1.getValidYCoordinates()[random.nextInt(parent1.getValidYCoordinates().length)];
        Map<Integer, Integer> sharedTerminals = this.visitAndCopyFromDonator(newBorn, parent2, parent2.getNodeSet().stream().filter(n -> n.isSensing() && n.getX() == sampleX && n.getY() == sampleY).collect(Collectors.toSet()));
        this.visitAndCopyFromPatient(newBorn, parent1, parent1.getNodeSet().stream().filter(n -> (n.getX() != sampleX || n.getY() != sampleY) && n.isSensing()).collect(Collectors.toSet()), sharedTerminals);
        return newBorn;
    }

    private Map<Integer, Integer> visitAndCopyFromDonator(MyController child, MyController parent, Set<MyController.Neuron> frontier) {
        Map<MyController.Neuron, Integer> visited = new HashMap<>();
        Map<Integer, Integer> terminals = new HashMap<>();
        for (MyController.Neuron n : frontier) {
            this.recursivelyVisitAndCopyFromDonator(child, parent, n, null, visited, terminals);
        }
        return terminals;
    }

    private void recursivelyVisitAndCopyFromDonator(MyController child, MyController parent, MyController.Neuron current, MyController.Neuron previous,
                                         Map<MyController.Neuron, Integer> visited, Map<Integer, Integer> terminals) {
        if (visited.containsKey(current)) {
            current.getIngoingEdges().stream().filter(e -> e.getSource() == previous.getIndex()).forEach(e -> child.addEdge(visited.get(previous), visited.get(current), e.getParams()[0], e.getParams()[1]));
            return;
        }
        visited.put(current, child.getNodeSet().size());
        child.copyNeuron(current, false);
        if (current.isActuator()) {
            terminals.put(current.getIndex(), visited.get(current));
        }
        if (previous != null) {
            current.getIngoingEdges().stream().filter(e -> e.getSource() == previous.getIndex()).forEach(e -> child.addEdge(visited.get(previous), visited.get(current), e.getParams()[0], e.getParams()[1]));
        }
        parent.getOutgoingEdges(current).forEach(e -> this.recursivelyVisitAndCopyFromDonator(child, parent, parent.getNodeMap().get(e.getTarget()), current, visited, terminals));
    }

    private void visitAndCopyFromPatient(MyController child, MyController parent, Set<MyController.Neuron> frontier,
                                                 Map<Integer, Integer> terminals) {
        Map<MyController.Neuron, Integer> visited = new HashMap<>();
        for (MyController.Neuron n : frontier) {
            this.recursivelyVisitAndCopyFromPatient(child, parent, n, null, visited, terminals);
        }
    }

    private void recursivelyVisitAndCopyFromPatient(MyController child, MyController parent, MyController.Neuron current, MyController.Neuron previous,
                                         Map<MyController.Neuron, Integer> visited, Map<Integer, Integer> terminals) {
        if (visited.containsKey(current)) {
            current.getIngoingEdges().stream().filter(e -> e.getSource() == previous.getIndex()).forEach(e -> child.addEdge(visited.get(previous), visited.get(current), e.getParams()[0], e.getParams()[1]));
            return;
        }
        if (!terminals.containsKey(current.getIndex())) {
            visited.put(current, child.getNodeSet().size());
            child.copyNeuron(current, false);
        }
        else {
            visited.put(current, terminals.get(current.getIndex()));
        }
        if (previous != null) {
            current.getIngoingEdges().stream().filter(e -> e.getSource() == previous.getIndex()).forEach(e -> child.addEdge(visited.get(previous), visited.get(current), e.getParams()[0], e.getParams()[1]));
        }
        parent.getOutgoingEdges(current).forEach(e -> this.recursivelyVisitAndCopyFromPatient(child, parent, parent.getNodeMap().get(e.getTarget()), current, visited, terminals));
    }

}
