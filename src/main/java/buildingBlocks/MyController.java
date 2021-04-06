package buildingBlocks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.units.erallab.hmsrobots.core.controllers.Controller;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


public class MyController implements Controller<SensingVoxel> {
    // TODO: implement toString()
    public enum NodeType {

        ACTUATOR,
        SENSING,
        HIDDEN

    }
    // TODO: maybe better representation would be to also have a list of edges, and nodes have a list of edge indexes, but too much for the moment
    public static class Edge implements Serializable {
        @JsonProperty
        private double weight;
        @JsonProperty
        private double bias;
        @JsonProperty
        private final int source;

        @JsonCreator
        public Edge(@JsonProperty("weight") double w,
                    @JsonProperty("bias") double b,
                    @JsonProperty("source") int s) {
            weight = w;
            bias = b;
            source = s;
        }

        public Edge(Edge other) {
            weight = other.weight;
            bias = other.bias;
            source = other.source;
        }
        // TODO: decide whether to keep list alltogether
        // TODO: double[]?
        public List<Double> getParams() { return new ArrayList<>() {{ add(weight); add(bias); }}; }

        public void perturb(List<Double> params) {
            weight = params.get(0);
            bias = params.get(1);
        }

        public int getSource() { return source; }
        // TODO: equals() and hashCode() are not well-defined!
    }
    // TODO: decide what to do with exiled Neuron class
    public static class Neuron implements Serializable {
        @JsonProperty
        private final List<MyController.Edge> ingoingEdges;
        @JsonProperty
        private MultiLayerPerceptron.ActivationFunction function;
        @JsonProperty
        private final MyController.NodeType type;
        @JsonProperty
        private double message;
        @JsonProperty
        private double cache;
        @JsonProperty
        private final int x;
        @JsonProperty
        private final int y;
        @JsonProperty
        private final int index;
        // TODO: very coarse, but few easy solutions (without specialization)
        // TODO: char?
        @JsonProperty
        private final int numSensor;

        @JsonCreator
        public Neuron(@JsonProperty("index") int idx,
                      @JsonProperty("function") MultiLayerPerceptron.ActivationFunction a,
                      @JsonProperty("type") MyController.NodeType t,
                      @JsonProperty("x") int coord1,
                      @JsonProperty("y") int coord2,
                      @JsonProperty("numSensor") int s) {
            index = idx;
            function = a;
            type = t;
            x = coord1;
            y = coord2;
            ingoingEdges = new ArrayList<>();
            message = 0.0;
            cache = 0.0;
            numSensor = s;
        }

        public Neuron(Neuron other) {
            index = other.getIndex();
            function = other.getActivation();
            type = other.getType();
            x = other.getX();
            y = other.getY();
            ingoingEdges = other.getIngoingEdges().stream().map(MyController.Edge::new).collect(Collectors.toList());
            message= 0.0;
            cache = 0.0;
            numSensor = other.numSensor;
        }
        // TODO: call it forward?
        public void compute(Grid<? extends SensingVoxel> voxels, MyController controller) {
            SensingVoxel voxel = voxels.get(x, y);
            switch (type) {
                case SENSING -> message = function.apply(voxel.getLastReadings().stream().flatMapToDouble(x -> Arrays.stream(x.getValue()))
                        .toArray()[numSensor]);
                case ACTUATOR -> {message = function.apply(ingoingEdges.stream().mapToDouble(e -> this.propagate(e, controller)).sum());
                    voxel.applyForce(message);}
                default -> message = function.apply(ingoingEdges.stream().mapToDouble(e -> this.propagate(e, controller)).sum());
            }
        }

        private double propagate(MyController.Edge e, MyController controller) {
            List<Double> params = e.getParams();
            return controller.getNodeSet().get(e.getSource()).send() * params.get(0) + params.get(1);
        }

        public void advance() {
            cache = message;
        }

        public double send() { return cache; }

        public MultiLayerPerceptron.ActivationFunction getActivation() { return function; }

        public void setActivation(MultiLayerPerceptron.ActivationFunction function) { this.function = function; }

        public void addIngoingEdge(MyController.Edge e) {
            ingoingEdges.add(e);
        }

        public int getIndex() { return index; }

        public MyController.NodeType getType() {
            return type;
        }

        public List<MyController.Edge> getIngoingEdges() {
           return ingoingEdges;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void reset() {
            message = 0.0;
            cache = 0.0;
        }
        // TODO: maybe dict-like representation of key-value pairs
        @Override
        public String toString() {
           return String.join(",", String.valueOf(index),
                    String.valueOf(x),  String.valueOf(y), String.valueOf(type), String.valueOf(function),
                    String.join("-", ingoingEdges.stream().map(e -> String.valueOf(e.getSource())).toArray(String[]::new)),
                    String.valueOf(numSensor));
        }

    }

    @JsonProperty
    private final List<Neuron> nodes;

    public static int flattenCoord(int x, int y, int width) {
        return y * width + x;
    }

    @JsonCreator
    public MyController(@JsonProperty("nodes") List<Neuron> n) {
        nodes = new ArrayList<>();
        for (Neuron entry : n) {
            nodes.add(new Neuron(entry));
        }
    }

    public MyController(MyController other) {
        this(other.getNodeSet());
    }

    public List<Neuron> getNodeSet() {
        return nodes;
    }

    public void addEdge(int source, int dest, double weight, double bias) {
        Edge edge = new Edge(weight, bias, source);
        nodes.get(dest).addIngoingEdge(edge);
    }

    public Neuron addNode(MultiLayerPerceptron.ActivationFunction a, NodeType t, int x, int y, int s) {
        Neuron newNode = new Neuron(this.nodes.size(), a, t, x, y, s);
        nodes.add(newNode);
        return newNode;
    }

    public Neuron addNode(MultiLayerPerceptron.ActivationFunction a, NodeType t, int x, int y) {
        return this.addNode(a, t, x, y, -1);
    }

    public static double euclideanDistance(Neuron n1, Neuron n2) {
        return Math.sqrt(Math.pow(n1.getX() - n2.getX(), 2) + Math.pow(n1.getY() - n2.getY(), 2));
    }

    @Override
    public void control(double t, Grid<? extends SensingVoxel> voxels) {
        nodes.forEach(n -> n.compute(voxels, this));
        nodes.forEach(Neuron::advance);
    }

    @Override
    public void reset() {
        this.getNodeSet().forEach(Neuron::reset);
    }
    // TODO: really need the two following?
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyController that = (MyController) o;
        return nodes.equals(that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }

}
