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
        @JsonProperty
        private int delay;
        @JsonProperty
        public static int maxDelay = 0;
        @JsonProperty
        private boolean enabled;

        @JsonCreator
        public Edge(@JsonProperty("weight") double w,
                    @JsonProperty("bias") double b,
                    @JsonProperty("source") int s,
                    @JsonProperty("delay") int d) {
            weight = w;
            bias = b;
            source = s;
            delay = d;
            enabled = true;
        }

        public Edge(Edge other) {
            weight = other.weight;
            bias = other.bias;
            source = other.source;
            delay = other.delay;
            enabled = other.enabled;
        }
        // TODO: decide whether to keep array alltogether
        public double[] getParams() { return new double[] { weight, bias }; }

        public void perturbParams(List<Double> params) {
            weight = params.get(0);
            bias = params.get(1);
        }

        public int getDelay() { return delay; }

        public void perturbDelay(int d) { delay = d; }

        public int getSource() { return source; }
        // TODO: equals() and hashCode() are not well-defined!
        public boolean isEnabled() { return enabled; }

        public void perturbAbility() { enabled = !enabled; }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME,
            include=JsonTypeInfo.As.PROPERTY)
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
        protected double message;
        protected double[] cache;
        @JsonProperty
        protected final int x;
        @JsonProperty
        protected final int y;
        @JsonProperty
        protected final int index;

        public Neuron(@JsonProperty("index") int idx,
                      @JsonProperty("function") MultiLayerPerceptron.ActivationFunction a,
                      @JsonProperty("x") int coord1,
                      @JsonProperty("y") int coord2) {
            index = idx;
            function = a;
            x = coord1;
            y = coord2;
            ingoingEdges = new ArrayList<>();
            this.resetState();
        }

        public Neuron(Neuron other) {
            this(other.index, other.function, other.x, other.y);
            ingoingEdges = other.getIngoingEdges().stream().map(MyController.Edge::new).collect(Collectors.toList());
            this.resetState();
        }
        // TODO: call it forward?
        public abstract void compute(Grid<? extends SensingVoxel> voxels, MyController controller);

        public abstract boolean isActuator();

        public abstract boolean isSensing();

        public boolean isHidden() { return !(this.isSensing() || this.isActuator());}

        protected double propagate(MyController.Edge e, MyController controller) {
            double[] params = e.getParams();
            return controller.getNodeSet().get(e.getSource()).send(e.getDelay()) * params[0] + params[1];
        }

        public void advance() {
            if (cache.length - 1 >= 0) System.arraycopy(cache, 0, cache, 1, cache.length - 1);
            cache[0] = message;
        }

        public double send(int k) { return cache[k]; }

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

        protected void resetState() {
            message = 0.0;
            cache = new double[Edge.maxDelay + 1];
            Arrays.fill(cache, 0.0);
        }
        // TODO: maybe dict-like representation of key-value pairs
        @Override
        public String toString() {
           return String.join(",", String.valueOf(index),
                    String.valueOf(x),  String.valueOf(y), String.valueOf(function),
                    String.join("&", ingoingEdges.stream().map(e -> String.join("/",
                            String.valueOf(e.getSource()), String.valueOf(e.getParams()[0]))).toArray(String[]::new)));
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
            message = function.apply(ingoingEdges.stream().filter(Edge::isEnabled).mapToDouble(e -> this.propagate(e, controller)).sum());
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
            message = function.apply(ingoingEdges.stream().filter(Edge::isEnabled).mapToDouble(e -> this.propagate(e, controller)).sum());
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

    @JsonCreator
    public MyController(@JsonProperty("nodes") List<Neuron> n) {
        this.nodes = new ArrayList<>();
        for (Neuron entry : n) {
            if (entry instanceof SensingNeuron) {
                this.nodes.add(new SensingNeuron((SensingNeuron) entry));
            }
            else if (entry instanceof ActuatorNeuron) {
                this.nodes.add(new ActuatorNeuron((ActuatorNeuron) entry));
            }
            else if (entry instanceof HiddenNeuron)  {
                this.nodes.add(new HiddenNeuron((HiddenNeuron) entry));
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
        return this.nodes;
    }

    public List<Edge> getEdgeSet() { return this.nodes.stream().flatMap(n -> n.getIngoingEdges().stream()).collect(Collectors.toList()); }

    public void addEdge(int source, int dest, double weight, double bias) {
        Edge edge = new Edge(weight, bias, source, 0);
        this.nodes.get(dest).addIngoingEdge(edge);
    }

    public Neuron addHiddenNode(MultiLayerPerceptron.ActivationFunction a, int x, int y) {
        Neuron newNode = new HiddenNeuron(this.nodes.size(), MultiLayerPerceptron.ActivationFunction.SIGMOID, x, y);
        this.nodes.add(newNode);
        return newNode;
    }

    public Neuron addActuatorNode(int x, int y) {
        Neuron newNode = new ActuatorNeuron(this.nodes.size(), x, y);
        this.nodes.add(newNode);
        return newNode;
    }

    public Neuron addSensingNode(int x, int y, int s) {
        Neuron newNode = new SensingNeuron(this.nodes.size(), x, y, s);
        this.nodes.add(newNode);
        return newNode;
    }

    public boolean hasCycles() {
        for (Neuron s : this.nodes.stream().filter(Neuron::isSensing).collect(Collectors.toList())) {
            if (this.recursivelyVisit(s, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean recursivelyVisit(Neuron currentNode, Set<Neuron> visited) {
        if (visited.contains(currentNode)) {
            return true;
        }
        visited.add(currentNode);
        this.nodes.stream().flatMap(n -> n.getIngoingEdges().stream().filter(e -> e.getSource() == currentNode.getIndex()).map(e -> this.nodes.get(e.getSource())))
                .forEach(s -> {Set<Neuron> updated = new HashSet<>(visited); updated.add(s); this.recursivelyVisit(s, updated);});
        return false;
    }

    public static double euclideanDistance(Neuron n1, Neuron n2) {
        return Math.sqrt(Math.pow(n1.getX() - n2.getX(), 2) + Math.pow(n1.getY() - n2.getY(), 2));
    }

    public static int flattenCoord(int x, int y, int width) {
        return y * width + x;
    }

    @Override
    public void control(double t, Grid<? extends SensingVoxel> voxels) {
        this.nodes.forEach(n -> n.compute(voxels, this));
        this.nodes.forEach(Neuron::advance);
    }

    @Override
    public void reset() {
        this.getNodeSet().forEach(Neuron::resetState);
    }

}
