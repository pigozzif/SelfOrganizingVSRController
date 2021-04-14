package geneticOps;

import buildingBlocks.MyController;
import it.units.malelab.jgea.core.operator.Crossover;
import org.apache.commons.math3.util.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// TODO: for the moment not so nice
public class CrossoverWithDonation implements Crossover<MyController> {

    private final Map<Integer, MyController.Neuron> visitedNeurons;
    private int x;
    private int y;
    private final String strategy;

    public CrossoverWithDonation(String strategy) {
        this.visitedNeurons = new HashMap<>();
        this.strategy = strategy;
    }

    @Override
    public MyController recombine(MyController parent1, MyController parent2, Random random) {
        Pair<Integer, Integer> pair = parent1.getValidCoordinates()[random.nextInt(parent1.getValidCoordinates().length)];
        this.x = pair.getFirst();
        this.y = pair.getSecond();
        return this.amputateVoxel(parent1, parent2, this.x, this.y);
    }

    public MyController amputateVoxel(MyController parent1, MyController parent2, int x, int y) {
        MyController newBorn;
        newBorn = new MyController(parent1);
        if (this.strategy.equals("traditional")) {
            this.visit(parent1, parent1.getNodeSet().stream().filter(n -> n.isSensing() && n.getX() == x && n.getY() == y).collect(Collectors.toSet()));
            this.fillFromPatient(newBorn);
            this.visit(parent2, parent2.getNodeSet().stream().filter(n -> n.getX() == x && n.getY() == y && n.isSensing()).collect(Collectors.toSet()));
            this.fillFromDonator(newBorn, parent2);
        }
        else if (this.strategy.equals("growing")) {
            this.visit(parent2, parent2.getNodeSet().stream().filter(n -> n.getX() == x && n.getY() == y && n.isSensing()).collect(Collectors.toSet()));
            this.fillGrowing(newBorn, parent2);
        }
        return newBorn;
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

    private void fillFromPatient(MyController child) {
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

    private void fillGrowing(MyController child, MyController parent) {
        Map<Integer, Integer> idxMap = new HashMap<>();
        for (MyController.Neuron neuron : this.visitedNeurons.values()) {
            idxMap.put(neuron.getIndex(), child.copyNeuron(neuron, false));
        }
        for (MyController.Edge edge : parent.getEdgeSet()) {
            if ((this.visitedNeurons.containsKey(edge.getSource()) || this.visitedNeurons.containsKey(edge.getTarget()))) {
                int source = idxMap.getOrDefault(edge.getSource(), edge.getSource());
                int target = idxMap.getOrDefault(edge.getTarget(), edge.getTarget());
                if (!child.getNodeMap().get(source).isHidden() && !child.getNodeMap().get(target).isHidden()) {
                    child.addEdge(source, target, edge.getParams()[0], edge.getParams()[1]);
                }
            }
        }
        this.visitedNeurons.clear();
    }

    private void visit(MyController parent, Set<MyController.Neuron> frontier) {
        Queue<MyController.Neuron> neuronQueue = new LinkedList<>(frontier);
        Map<Integer, List<MyController.Edge>> outgoingEdges = parent.getOutgoingEdges();
        MyController.Neuron current;
        while (!neuronQueue.isEmpty()) {
            current = neuronQueue.remove();
            if (this.visitedNeurons.containsKey(current.getIndex()) || (!current.isHidden() && (current.getX() != this.x || current.getY() != this.y))) {
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

    private static void plotBrain(MyController controller, String name) throws IOException, InterruptedException {
        String intermediateFileName = name + ".txt";
        String outputFileName = name + ".png";
        try {
            writeBrainToFile(controller, intermediateFileName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        Process p = Runtime.getRuntime().exec("python python/visualize_brain.py " + intermediateFileName + " " + outputFileName);
        p.waitFor();
    }

    private static void writeBrainToFile(MyController controller, String outputName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputName));
        writer.write("index,x,y,function,edges,type\n");
        controller.getNodeSet().forEach(n -> {
            try {
                writer.write(n.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
    }

}
