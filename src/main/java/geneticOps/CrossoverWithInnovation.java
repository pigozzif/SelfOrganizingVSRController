package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Crossover;

import java.util.*;

// TODO: for the moment, to avoid hash collisions, it might happen that an edge with the same
// index but different semantics than an existing one is created, same for neurons (but, apparently, never happened).
// As a result, offspring can be disrupted. BE CAREFUL!
public class CrossoverWithInnovation implements Crossover<MyController> {
    // TODO: horrible try catch, but let's see
    @Override
    public MyController recombine(MyController parent1, MyController parent2, Random random) {
        MyController newBorn = new MyController(Collections.emptyMap());
        Set<MyController.Neuron> allNodes = new HashSet<>(parent1.getNodeSet());
        allNodes.addAll(parent2.getNodeSet());
        allNodes.forEach(n -> newBorn.copyNeuron(n, false));
        try {
            Map<Integer, MyController.Edge> edges1 = parent1.getEdgeMap();
            Map<Integer, MyController.Edge> edges2 = parent2.getEdgeMap();
            Set<MyController.Edge> allEdges = new HashSet<>(edges1.values());
            allEdges.addAll(edges2.values());
            for (MyController.Edge edge : allEdges) {
                MyController.Edge fromFirst = edges1.get(edge.getIndex());
                MyController.Edge fromSecond = edges2.get(edge.getIndex());
                if (fromFirst != null && fromSecond != null) {
                    newBorn.copyEdge((random.nextBoolean()) ? fromFirst : fromSecond);
                } else if (fromFirst != null) {
                    newBorn.copyEdge(fromFirst);
                } else {
                    newBorn.copyEdge(fromSecond);
                }
            }
        }
        catch (Exception e) {
            System.out.println("DUPLICATION ERROR!");
            return parent1;
        }
        /*List<MyController.Edge> genes1 = new ArrayList<>(parent1.getEdgeSet());
        List<MyController.Edge> genes2 = new ArrayList<>(parent2.getEdgeSet());
        genes1.sort(Comparator.comparingInt(MyController.Edge::getIndex));
        genes2.sort(Comparator.comparingInt(MyController.Edge::getIndex));
        Set<MyController.Neuron> visited = new HashSet<>();
        int i = 0;
        int j = 0;
        MyController.Edge currFrom1;
        MyController.Edge currFrom2;
        while (i < genes1.size() && j < genes2.size()) {
            currFrom1 = genes1.get(i);
            currFrom2 = genes2.get(j);
            if (currFrom1.getIndex() == currFrom2.getIndex()) {
                boolean isFromFirst = random.nextBoolean();
                this.updateNewBorn(visited, newBorn, (isFromFirst) ? parent1 : parent2, (isFromFirst) ? currFrom1 : currFrom2);
                ++i;
                ++j;
            }
            else if (currFrom1.getIndex() < currFrom2.getIndex()) {
                this.updateNewBorn(visited, newBorn, parent1, currFrom1);
                ++i;
            }
            else {
                this.updateNewBorn(visited, newBorn, parent2, currFrom2);
                ++j;
            }
        }
        int k = Math.min(i, j);
        boolean isFirstRemaining = i < j;
        List<MyController.Edge> rest = (isFirstRemaining) ? genes1 : genes2;
        MyController.Edge curr;
        while (k < rest.size()) {
            curr = rest.get(k);
            this.updateNewBorn(visited, newBorn, (isFirstRemaining) ? parent1 : parent2, curr);
            ++k;
        }*/
        return newBorn;
    }

    private void updateNewBorn(Set<MyController.Neuron> nodes, MyController newBorn, MyController parent, MyController.Edge edge) {
        MyController.Neuron source = parent.getNodeMap().get(edge.getSource());
        MyController.Neuron dest = parent.getNodeMap().get(edge.getTarget());
        if (!nodes.contains(source)) {
            newBorn.copyNeuron(source, false);
            nodes.add(source);
        }
        if (!nodes.contains(dest)) {
            newBorn.copyNeuron(dest, false);
            nodes.add(dest);
        }
        newBorn.addEdge(source.getIndex(), dest.getIndex(), edge.getParams()[0], edge.getParams()[1]);
    }

}
