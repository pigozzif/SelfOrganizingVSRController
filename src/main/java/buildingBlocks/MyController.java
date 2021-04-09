package buildingBlocks;

import com.fasterxml.jackson.annotation.*;
import geneticOps.Innovated;
import it.units.erallab.hmsrobots.core.controllers.Controller;
import it.units.erallab.hmsrobots.core.controllers.MultiLayerPerceptron;
import it.units.erallab.hmsrobots.core.objects.SensingVoxel;
import it.units.erallab.hmsrobots.util.Grid;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class MyController implements Controller<SensingVoxel> {
    // TODO: implement toString()
    // TODO: maybe better representation would be to also have a list of edges, and nodes have a list of edge indexes, but too much for the moment
    public static class Edge extends Innovated implements Serializable {
        @JsonProperty
        private double weight;
        @JsonProperty
        private double bias;
        @JsonProperty
        private final int source;
        private final int target;
        @JsonProperty
        private int delay;
        @JsonProperty
        public static int MAX_DELAY = 59;
        @JsonProperty
        private boolean enabled;

        @JsonCreator
        public Edge(@JsonProperty("weight") double w,
                    @JsonProperty("bias") double b,
                    @JsonProperty("source") int s,
                    int t,
                    @JsonProperty("delay") int d,
                    int innX, int innY) {
            this(w, b, s, t, d);
            this.setInnovation(innX, innY);
        }

        public Edge(@JsonProperty("weight") double w,
                    @JsonProperty("bias") double b,
                    @JsonProperty("source") int s,
                    int t,
                    @JsonProperty("delay") int d) {
            super();
            weight = w;
            bias = b;
            source = s;
            target = t;
            delay = d;
            enabled = true;
        }

        public Edge(Edge other) {
            this(other.weight, other.bias, other.source, other.target, other.delay);
            enabled = true;  // not necessary
            this.setInnovation(other.event);
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

        public int getTarget() { return target; }
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
    public abstract static class Neuron extends Innovated implements Serializable {
        @JsonProperty
        protected List<MyController.Edge> ingoingEdges;
        @JsonProperty
        protected MultiLayerPerceptron.ActivationFunction function;
        protected transient double message;
        protected transient double[] cache;
        @JsonProperty
        protected final int x;
        @JsonProperty
        protected final int y;
        @JsonProperty
        protected final int index;

        public Neuron(@JsonProperty("index") int idx,
                      @JsonProperty("function") MultiLayerPerceptron.ActivationFunction a,
                      @JsonProperty("x") int coord1,
                      @JsonProperty("y") int coord2,
                      int innX, int innY) {
            this(idx, a, coord1, coord2);
            this.setInnovation(innX, innY);
        }

        public Neuron(@JsonProperty("index") int idx,
                      @JsonProperty("function") MultiLayerPerceptron.ActivationFunction a,
                      @JsonProperty("x") int coord1,
                      @JsonProperty("y") int coord2) {
            super();
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
            this.setInnovation(other.event);
        }
        // TODO: call it forward?
        public abstract void compute(Grid<? extends SensingVoxel> voxels, MyController controller);

        public abstract boolean isActuator();

        public abstract boolean isSensing();

        public boolean isHidden() { return !(this.isSensing() || this.isActuator());}

        protected double propagate(MyController.Edge e, MyController controller) {
            double[] params = e.getParams();
            return controller.getNodeMap().get(e.getSource()).send(e.getDelay()) * params[0] + params[1];
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

        public void resetState() {
            message = 0.0;
            cache = new double[Edge.MAX_DELAY + 1];
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
        public int getInnovation() { return index; }

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

        public int getNumSensor() { return numSensor; }

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
        public int getInnovation() { return index; }

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
                            @JsonProperty("y") int coord2,
                            int innX, int innY) {
            super(idx, a, coord1, coord2, innX, innY);
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
    private final Map<Integer, Neuron> nodes;

    @JsonCreator
    public MyController(@JsonProperty("nodes") Map<Integer, Neuron> n) {
        this.nodes = new HashMap<>();
        for (Neuron entry : n.values()) {
            this.copyNeuron(entry, true);
        }
    }

    public MyController(MyController other) {
        this(other.getNodeMap());
    }

    public Map<Integer, Neuron> getNodeMap() { return this.nodes; }

    public Collection<Neuron> getNodeSet() { return this.nodes.values(); }

    public List<Edge> getEdgeSet() { return this.nodes.values().stream().flatMap(n -> n.getIngoingEdges().stream()).collect(Collectors.toList()); }

    public void addEdge(int source, int dest, double weight, double bias) {
        Edge edge = new Edge(weight, bias, source, dest, 0, this.nodes.get(source).getInnovation(), this.nodes.get(dest).getInnovation());
        this.nodes.get(dest).addIngoingEdge(edge);
    }

    public void copyNeuron(Neuron neuron, boolean copyEdges) {
        int idx = this.nodes.size();
        Neuron newComer;
        if (neuron instanceof SensingNeuron) {
            newComer = new SensingNeuron(idx, neuron.getX(), neuron.getY(), ((SensingNeuron) neuron).getNumSensor());
        }
        else if (neuron instanceof ActuatorNeuron) {
            newComer = new ActuatorNeuron(idx, neuron.getX(), neuron.getY());
        }
        else if (neuron instanceof HiddenNeuron)  {
            newComer = new HiddenNeuron(idx, neuron.getActivation(), neuron.getX(), neuron.getY(), neuron.getFirstInnovation(), neuron.getSecondInnovation());
        }
        else {
            throw new RuntimeException("Neuron type not supported: " + neuron.getClass());
        }
        this.nodes.put(idx, newComer);
        if (copyEdges) {
            neuron.getIngoingEdges().forEach(e -> newComer.addIngoingEdge(new Edge(e)));
        }
    }

    public Neuron addHiddenNode(int source, int dest, MultiLayerPerceptron.ActivationFunction a, int x, int y, Supplier<Double> parameterSupplier) {
        int idx = this.nodes.size();
        Neuron newNode = new HiddenNeuron(idx, a, x, y, this.nodes.get(source).getInnovation(), this.nodes.get(dest).getInnovation());
        this.nodes.put(idx, newNode);
        this.addEdge(source, idx, parameterSupplier.get(), parameterSupplier.get());
        this.addEdge(idx, dest, parameterSupplier.get(), parameterSupplier.get());
        return newNode;
    }

    public Neuron addActuatorNode(int x, int y) {
        int idx = this.nodes.size();
        Neuron newNode = new ActuatorNeuron(idx, x, y);
        this.nodes.put(idx, newNode);
        return newNode;
    }

    public Neuron addSensingNode(int x, int y, int s) {
        int idx = this.nodes.size();
        Neuron newNode = new SensingNeuron(this.nodes.size(), x, y, s);
        this.nodes.put(idx, newNode);
        return newNode;
    }
    // TODO: strong suspect this could be sped up
    public boolean hasCycles() {
        for (Neuron s : this.getNodeSet().stream().filter(Neuron::isSensing).collect(Collectors.toList())) {
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
        this.getNodeSet().stream().flatMap(n -> n.getIngoingEdges().stream().filter(e -> e.getSource() == currentNode.getIndex()).map(e -> this.nodes.get(e.getSource())))
                .forEach(s -> {Set<Neuron> updated = new HashSet<>(visited); updated.add(s); this.recursivelyVisit(s, updated);});
        return false;
    }

    public static double euclideanDistance(Neuron n1, Neuron n2) {
        return euclideanDistance(n1.getX(), n1.getY(), n2.getX(), n2.getY());
    }

    public static double euclideanDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static int flattenCoord(int x, int y, int width) {
        return y * width + x;
    }

    @Override
    public void control(double t, Grid<? extends SensingVoxel> voxels) {
        this.getNodeSet().forEach(n -> n.compute(voxels, this));
        this.getNodeSet().forEach(Neuron::advance);
    }

    @Override
    public void reset() {
        this.getNodeSet().forEach(Neuron::resetState);
    }

}
