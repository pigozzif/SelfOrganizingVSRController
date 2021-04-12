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

    private final Set<MyController.Neuron> visitedNeurons;
    private final Set<MyController.Edge> visitedEdges;

    public CrossoverWithDonation() {
        this.visitedNeurons = new HashSet<>();
        this.visitedEdges = new HashSet<>();
    }

    @Override
    public MyController recombine(MyController parent1, MyController parent2, Random random) {
        Pair<Integer, Integer> pair = parent1.getValidCoordinates()[random.nextInt(parent1.getValidCoordinates().length)];
        int sampleX = pair.getFirst();
        int sampleY = pair.getSecond();
        //try {
        //    plotBrain(parent1, "parent1");
        //    plotBrain(parent2, "parent2");
        //}
        //catch (Exception e) {
        //    System.exit(1);
        //}
        //this.visitAndCopyFromPatient(newBorn, newBorn.getNodeSet().stream().filter(n -> n.getX() == sampleX && n.getY() == sampleY && n.isSensing()).collect(Collectors.toSet()));
        //int numFromDonator = newBorn.getNodeSet().size();
        //try {
        //    plotBrain(newBorn, "child_intermediate");
        //}
        //catch (Exception e) {
        //    System.exit(1);
        //}
        //this.visitAndCopyFromDonator(newBorn, parent2, parent2.getNodeSet().stream().filter(n -> n.isSensing() && n.getX() == sampleX && n.getY() == sampleY).collect(Collectors.toSet()));//, sharedTerminals);
        //System.out.println("Operated on (" + sampleX + "," + sampleY + "), with " + numFromDonator + " from donator and " + (newBorn.getNodeSet().size() - numFromDonator) + " from the other one");
        /*try {
            plotBrain(newBorn, "child_final");
        }
        catch (Exception e) {
            System.exit(1);
        }*/

        return this.amputateVoxel(parent1, parent2, sampleX, sampleY);
    }

    public MyController amputateVoxel(MyController parent1, MyController parent2, int x, int y) {
        MyController newBorn = new MyController(parent1);
        this.visit(parent1, parent1.getNodeSet().stream().filter(n -> n.isSensing() && n.getX() == x && n.getY() == y).collect(Collectors.toSet()));
        this.fillNewBorn(newBorn, false);
        this.visit(parent2, parent2.getNodeSet().stream().filter(n -> n.getX() == x && n.getY() == y && n.isSensing()).collect(Collectors.toSet()));
        this.fillNewBorn(newBorn, true);
        return newBorn;
    }

    private void fillNewBorn(MyController child, boolean isDonating) {
        if (isDonating) {
            this.visitedNeurons.forEach(n -> child.copyNeuron(n, false));
            this.visitedEdges.forEach(child::copyEdge);
        }
        else {
            this.visitedEdges.forEach(child::removeEdge);
            this.visitedNeurons.forEach(child::removeNeuron);
        }
        this.visitedNeurons.clear();
        this.visitedEdges.clear();
    }

    private void visit(MyController parent, Set<MyController.Neuron> frontier) {
        Map<Integer, List<MyController.Edge>> outgoingEdges = parent.getOutgoingEdges();
        Queue<MyController.Edge> edgeQueue = frontier.stream().flatMap(n -> outgoingEdges.getOrDefault(n.getIndex(), new ArrayList<>()).stream()).collect(Collectors.toCollection(LinkedList::new));
        MyController.Neuron source, target;
        MyController.Edge edge;
        while (!edgeQueue.isEmpty()) {
            edge = edgeQueue.remove();
            source = parent.getNodeMap().get(edge.getSource());
            target = parent.getNodeMap().get(edge.getTarget());
            if (!this.visitedNeurons.contains(source)) {
                this.visitedNeurons.add(source);
                edgeQueue.addAll(source.getIngoingEdges());
            }
            if (!this.visitedNeurons.contains(target)) {
                this.visitedNeurons.add(target);
                edgeQueue.addAll(outgoingEdges.getOrDefault(target.getIndex(), Collections.emptyList()));
            }
            this.visitedEdges.add(edge);
        }
    }

    private void visitAndCopyFromDonator(MyController child, MyController parent, Set<MyController.Neuron> frontier) {
        Map<MyController.Neuron, Integer> visited = new HashMap<>();
        Map<Integer, List<MyController.Edge>> outgoingEdges = parent.getOutgoingEdges();
        Queue<MyController.Edge> edgeQueue = frontier.stream().flatMap(n -> outgoingEdges.getOrDefault(n.getIndex(), new ArrayList<>()).stream()).collect(Collectors.toCollection(LinkedList::new));
        int sourceIdx;
        int targetIdx;
        MyController.Neuron source, target;
        MyController.Edge edge;
        while (!edgeQueue.isEmpty()) {
            edge = edgeQueue.remove();
            source = parent.getNodeMap().get(edge.getSource());
            target = parent.getNodeMap().get(edge.getTarget());
            if (!visited.containsKey(source)) {
                sourceIdx = child.copyNeuron(source, false);
                visited.put(source, sourceIdx);
                edgeQueue.addAll(source.getIngoingEdges());
            }
            else {
                sourceIdx = visited.get(source);
            }
            if (!visited.containsKey(target)) {
                targetIdx = child.copyNeuron(target, false);
                visited.put(target, targetIdx);
                if (outgoingEdges.containsKey(target.getIndex())) {
                    edgeQueue.addAll(outgoingEdges.get(target.getIndex()));
                }
            }
            else {
                targetIdx = visited.get(target);
            }
            child.addEdge(sourceIdx, targetIdx, edge.getParams()[0], edge.getParams()[1]);
        }
    }

    private void visitAndCopyFromPatient(MyController child, Set<MyController.Neuron> frontier) {
        /*Map<MyController.Neuron, Integer> visited = new HashMap<>();
        Map<Integer, List<MyController.Edge>> outgoingEdges = parent.getOutgoingEdges();
        Queue<MyController.Edge> edgeQueue = frontier.stream().flatMap(n -> outgoingEdges.getOrDefault(n.getIndex(), new ArrayList<>()).stream()).collect(Collectors.toCollection(LinkedList::new));
        int sourceIdx;
        int targetIdx;
        MyController.Neuron source, target;
        MyController.Edge edge;
        while (!edgeQueue.isEmpty()) {
            edge = edgeQueue.remove();
            source = parent.getNodeMap().get(edge.getSource());
            target = parent.getNodeMap().get(edge.getTarget());
            if (!visited.containsKey(source)) {
                //if (!terminals.containsKey(source.getIndex())) {
                    sourceIdx = child.copyNeuron(source, false);
                //}
                //else {
                //    sourceIdx = terminals.get(source.getIndex());
                //}
                visited.put(source, sourceIdx);
                edgeQueue.addAll(source.getIngoingEdges());
            }
            else {
                sourceIdx = visited.get(source);
            }
            if (!visited.containsKey(target)) {
                //if (!terminals.containsKey(target.getIndex())) {
                    targetIdx = child.copyNeuron(target, false);
                //}
                //else {
                //    targetIdx = terminals.get(target.getIndex());
                //}
                visited.put(target, targetIdx);
                if (outgoingEdges.containsKey(target.getIndex())) {
                    edgeQueue.addAll(outgoingEdges.get(target.getIndex()));
                }
            }
            else {
                targetIdx = visited.get(target);
            }
            child.addEdge(sourceIdx, targetIdx, edge.getParams()[0], edge.getParams()[1]);
        }*/
        Set<MyController.Neuron> destroyed = new HashSet<>();
        Map<Integer, List<MyController.Edge>> outgoingEdges = child.getOutgoingEdges();
        Queue<MyController.Edge> edgeQueue = frontier.stream().flatMap(n -> outgoingEdges.getOrDefault(n.getIndex(), new ArrayList<>()).stream()).collect(Collectors.toCollection(LinkedList::new));
        MyController.Neuron source, target;
        MyController.Edge edge;
        while (!edgeQueue.isEmpty()) {
            edge = edgeQueue.remove();
            source = child.getNodeMap().get(edge.getSource());
            target = child.getNodeMap().get(edge.getTarget());
            if (source != null) {
                edgeQueue.addAll(source.getIngoingEdges());
            }
            if (target != null) {
                edgeQueue.addAll(outgoingEdges.getOrDefault(target.getIndex(), new ArrayList<>()));
                child.removeEdge(edge);
            }
            if (!destroyed.contains(source) && source != null) {
                destroyed.add(source);
                child.removeNeuron(source);
            }
            if (!destroyed.contains(target) && target != null) {
                destroyed.add(target);
                child.removeNeuron(target);
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
