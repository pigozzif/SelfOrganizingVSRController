package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Crossover;

import java.util.*;


public class CrossoverWithInnovation implements Crossover<MyController> {
    // TODO: rewrite as he did
    @Override
    public MyController recombine(MyController parent1, MyController parent2, Random random) {
        MyController newBorn = new MyController(Collections.emptyMap());
        List<MyController.Edge> edges1 = parent1.getEdgeSet();
        List<MyController.Edge> edges2 = parent2.getEdgeSet();
        Collections.sort(edges1);
        Collections.sort(edges2);
        Set<MyController.Neuron> visited = new HashSet<>();
        int i = 0;
        int j = 0;
        MyController.Edge currFrom1;
        MyController.Edge currFrom2;
        while (i < edges1.size() && j < edges2.size()) {
            currFrom1 = edges1.get(i);
            currFrom2 = edges2.get(j);
            if (currFrom1.getInnovation() == currFrom2.getInnovation()) {
                boolean isFromFirst = random.nextBoolean();
                this.updateNewBorn(visited, newBorn, (isFromFirst) ? parent1 : parent2, (isFromFirst) ? currFrom1 : currFrom2);
                ++i;
                ++j;
            }
            else if (currFrom1.getInnovation() < currFrom2.getInnovation()) {
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
        List<MyController.Edge> rest = (isFirstRemaining) ? edges1 : edges2;
        MyController.Edge curr;
        while (k < rest.size()) {
            curr = rest.get(k);
            this.updateNewBorn(visited, newBorn, (isFirstRemaining) ? parent1 : parent2, curr);
            ++k;
        }
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
        newBorn.addEdge(newBorn.getNodeSet().size() - 2, newBorn.getNodeSet().size() - 1, edge.getParams()[0], edge.getParams()[1]);
    }

}
