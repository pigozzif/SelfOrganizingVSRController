package buildingBlocks;

import com.fasterxml.jackson.annotation.*;
import it.units.erallab.hmsrobots.core.controllers.Controller;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


public class MyController implements Controller<SensingVoxel> {
    // TODO: implement toString()
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
        public double[] getParams() { return new double[] { weight, bias }; }

        public void perturb(List<Double> params) {
            weight = params.get(0);
            bias = params.get(1);
        }

        public int getSource() { return source; }
        // TODO: equals() and hashCode() are not well-defined!
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(value=ActuatorNeuron.class, name="actuator"),
            @JsonSubTypes.Type(value=SensingNeuron.class, name="sensing"),
            @JsonSubTypes.Type(value=HiddenNeuron.class, name="hidden")
    })
    public abstract static class Neuron implements Serializable {
        @JsonProperty
        protected List<MyController.Edge> ingoingEdges;
        @JsonProperty
        protected MultiLayerPerceptron.ActivationFunction function;
        @JsonProperty
        protected double message;
        @JsonProperty
        protected double cache;
        @JsonProperty
        protected final int x;
        @JsonProperty
        protected final int y;
        @JsonProperty
        protected final int index;

        //@JsonCreator
        public Neuron(@JsonProperty("index") int idx,
                      @JsonProperty("function") MultiLayerPerceptron.ActivationFunction a,
                      @JsonProperty("x") int coord1,
                      @JsonProperty("y") int coord2) {
            index = idx;
            function = a;
            x = coord1;
            y = coord2;
            ingoingEdges = new ArrayList<>();
            message = 0.0;
            cache = 0.0;
        }

        public Neuron(Neuron other) {
            this(other.getIndex(), other.getActivation(), other.getX(), other.getY());
            ingoingEdges = other.getIngoingEdges().stream().map(MyController.Edge::new).collect(Collectors.toList());
            message = 0.0;
            cache = 0.0;
        }
        // TODO: call it forward?
        public abstract void compute(Grid<? extends SensingVoxel> voxels, MyController controller);

        public abstract boolean isActuator();

        public abstract boolean isSensing();

        public boolean isHidden() { return !(this.isSensing() || this.isActuator());}

        protected double propagate(MyController.Edge e, MyController controller) {
            double[] params = e.getParams();
            return controller.getNodeSet().get(e.getSource()).send() * params[0] + params[1];
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
                    String.valueOf(x),  String.valueOf(y), String.valueOf(function),
                    String.join("-", ingoingEdges.stream().map(e -> String.valueOf(e.getSource())).toArray(String[]::new)));
        }

    }

    public static class ActuatorNeuron extends Neuron {
        @JsonCreator
        public ActuatorNeuron(@JsonProperty("index") int idx,
                              @JsonProperty("x") int coord1,
                              @JsonProperty("y") int coord2) {
            super(idx, MultiLayerPerceptron.ActivationFunction.TANH, coord1, coord2);
        }

        public ActuatorNeuron(ActuatorNeuron entry) {
            super(entry);
        }

        @Override
        public void compute(Grid<? extends SensingVoxel> voxels, MyController controller) {
            SensingVoxel voxel = voxels.get(x, y);
            message = function.apply(ingoingEdges.stream().mapToDouble(e -> this.propagate(e, controller)).sum());
            voxel.applyForce(message);
        }

        @Override
        public boolean isActuator() { return true; }

        @Override
        public boolean isSensing() { return false; }

        @Override
        public String toString() {
            return String.join(",", super.toString(), "ACTUATOR");
        }

    }

    public static class SensingNeuron extends Neuron {
        @JsonProperty
        private final int numSensor;

        @JsonCreator
        public SensingNeuron(@JsonProperty("index") int idx,
                             @JsonProperty("x") int coord1,
                             @JsonProperty("y") int coord2,
                             @JsonProperty("numSensor") int s) {
            super(idx, MultiLayerPerceptron.ActivationFunction.TANH, coord1, coord2);
            numSensor = s;
        }

        public SensingNeuron(SensingNeuron other) {
            super(other);
            numSensor = other.numSensor;
        }

        @Override
        public void compute(Grid<? extends SensingVoxel> voxels, MyController controller) {
            SensingVoxel voxel = voxels.get(x, y);
            message = function.apply(voxel.getLastReadings().stream().flatMapToDouble(x -> Arrays.stream(x.getValue()))
                    .toArray()[numSensor]);
        }

        @Override
        public boolean isSensing() { return true; }

        @Override
        public boolean isActuator() { return false; }

        @Override
        public String toString() {
            return String.join(",", super.toString(), "SENSING" + numSensor);
        }

    }

    public static class HiddenNeuron extends Neuron {
        @JsonCreator
        public HiddenNeuron(@JsonProperty("index") int idx,
                            @JsonProperty("function") MultiLayerPerceptron.ActivationFunction a,
                            @JsonProperty("x") int coord1,
                            @JsonProperty("y") int coord2) {
            super(idx, a, coord1, coord2);
        }

        public HiddenNeuron(HiddenNeuron entry) {
            super(entry);
        }

        @Override
        public void compute(Grid<? extends SensingVoxel> voxels, MyController controller) {
            message = function.apply(ingoingEdges.stream().mapToDouble(e -> this.propagate(e, controller)).sum());
        }

        @Override
        public boolean isActuator() { return false; }

        @Override
        public boolean isSensing() { return false; }

        @Override
        public String toString() {
            return String.join(",", super.toString(), "HIDDEN");
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
            if (entry instanceof SensingNeuron) {
                nodes.add(new SensingNeuron((SensingNeuron) entry));
            }
            else if (entry instanceof ActuatorNeuron) {
                nodes.add(new ActuatorNeuron((ActuatorNeuron) entry));
            }
            else if (entry instanceof HiddenNeuron)  {
                nodes.add(new HiddenNeuron((HiddenNeuron) entry));
            }
            else {
                throw new RuntimeException("Provided Neuron type not supported: " + n.getClass());
            }
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

    public Neuron addHiddenNode(MultiLayerPerceptron.ActivationFunction a, int x, int y) {
        Neuron newNode = new HiddenNeuron(this.nodes.size(), a, x, y);
        nodes.add(newNode);
        return newNode;
    }

    public Neuron addActuatorNode(int x, int y) {
        Neuron newNode = new ActuatorNeuron(this.nodes.size(), x, y);
        nodes.add(newNode);
        return newNode;
    }

    public Neuron addSensingNode(int x, int y, int s) {
        Neuron newNode = new SensingNeuron(this.nodes.size(), x, y, s);
        nodes.add(newNode);
        return newNode;
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

}
