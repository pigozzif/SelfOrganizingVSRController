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
    private final Set<Integer> notToBeRemoved;
    private int x;
    private int y;

    public CrossoverWithDonation() {
        this.visitedNeurons = new HashMap<>();
        this.notToBeRemoved = new HashSet<>();
    }

    @Override
    public MyController recombine(MyController parent1, MyController parent2, Random random) {
        Pair<Integer, Integer> pair = parent1.getValidCoordinates()[random.nextInt(parent1.getValidCoordinates().length)];
        this.x = pair.getFirst();
        this.y = pair.getSecond();
        return this.amputateVoxel(parent1, parent2, this.x, this.y);
    }

    public MyController amputateVoxel(MyController parent1, MyController parent2, int x, int y) {
        MyController newBorn = new MyController(parent1);
        this.visit(parent1, parent1.getNodeSet().stream().filter(n -> n.isSensing() && n.getX() == x && n.getY() == y).collect(Collectors.toSet()));
        this.fillFromPatient(newBorn);
        this.visit(parent2, parent2.getNodeSet().stream().filter(n -> n.getX() == x && n.getY() == y && n.isSensing()).collect(Collectors.toSet()));
        this.fillFromDonator(newBorn, parent2);
        return newBorn;
    }

    private void fillFromDonator(MyController child, MyController parent) {
        Map<Integer, Integer> idxMap = new HashMap<>();
        for (MyController.Neuron neuron : this.visitedNeurons.values()) {
            idxMap.put(neuron.getIndex(), child.copyNeuron(neuron, false));
        }
        for (Integer idx : this.notToBeRemoved) {
            idxMap.put(idx, idx);
        }
        for (MyController.Edge edge : parent.getEdgeSet()) {
            if (this.visitedNeurons.containsKey(edge.getSource()) || this.visitedNeurons.containsKey(edge.getTarget())) {
                child.addEdge(idxMap.get(edge.getSource()), idxMap.get(edge.getTarget()), edge.getParams()[0], edge.getParams()[1]);
            }
        }
        this.notToBeRemoved.clear();
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
        this.notToBeRemoved.clear();
        this.visitedNeurons.clear();
    }

    private void visit(MyController parent, Set<MyController.Neuron> frontier) {
        Queue<MyController.Neuron> neuronQueue = new LinkedList<>(frontier);
        Map<Integer, List<MyController.Edge>> outgoingEdges = parent.getOutgoingEdges();
        MyController.Neuron current;
        while (!neuronQueue.isEmpty()) {
            current = neuronQueue.remove();
            if (this.visitedNeurons.containsKey(current.getIndex())) {
                continue;
            }
            else if (!current.isHidden() && (current.getX() != this.x || current.getY() != this.y)) {
                this.notToBeRemoved.add(current.getIndex());
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
