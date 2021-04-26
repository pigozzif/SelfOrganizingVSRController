/*
 * Copyright 2020 Eric Medvet <eric.medvet@gmail.com> (as eric)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import buildingBlocks.factories.ControllerFactory;
import buildingBlocks.MyController;
import buildingBlocks.RobotMapper;
import buildingBlocks.factories.ValidationFactory;
import com.google.common.base.Stopwatch;
import geneticOps.*;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.malelab.jgea.Worker;
import it.units.malelab.jgea.core.Factory;
import it.units.malelab.jgea.core.Individual;
import it.units.malelab.jgea.core.evolver.Event;
import it.units.malelab.jgea.core.evolver.Evolver;
import it.units.malelab.jgea.core.evolver.StandardEvolver;
import it.units.malelab.jgea.core.evolver.stopcondition.Births;
import it.units.malelab.jgea.core.listener.*;
import it.units.malelab.jgea.core.order.PartialComparator;
import it.units.malelab.jgea.core.selector.Tournament;
import it.units.malelab.jgea.core.selector.Worst;
import it.units.malelab.jgea.core.util.*;
import morphologies.Morphology;
import org.dyn4j.dynamics.Settings;
import validation.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static it.units.malelab.jgea.core.listener.NamedFunctions.*;
import static it.units.malelab.jgea.core.util.Args.*;

/**
 * @author eric
 */
public class Main extends Worker {

    private final static Settings PHYSICS_SETTINGS = new Settings();

    public static class ValidationOutcome {
        private final Event<?, ? extends Robot<?>, ? extends Outcome> event;
        private final Map<String, Object> keys;
        private final Outcome outcome;

        public ValidationOutcome(Event<?, ? extends Robot<?>, ? extends Outcome> event, Map<String, Object> keys, Outcome outcome) {
            this.event = event;
            this.keys = keys;
            this.outcome = outcome;
        }
    }

    public static final int CACHE_SIZE = 0;
    public static final String SEQUENCE_SEPARATOR_CHAR = ">";
    public static final String SEQUENCE_ITERATION_CHAR = ":";
    private static final String dir = "./output/";
    private double episodeTime;
    private double episodeTransientTime;
    private double validationEpisodeTime;
    private double validationEpisodeTransientTime;
    private int popSize;
    private int births;
    private int s;
    private Random seed;
    private Supplier<Double> parameterSupplier;
    private List<String> terrainNames;
    private Morphology morph;
    private double initPerc;
    private List<String> transformationNames;
    private String bestFileName;
    private String validationFileName;
    private String targetShapeName;
    private List<String> validationTransformationNames;
    private List<String> validationTerrainNames;
    private boolean validationFlag;
    private String connectivity;

    public Main(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        new Main(args);
    }

    @Override
    public void run() {
        this.parseArguments();
        Factory<MyController> basicFactory = new ControllerFactory(this.parameterSupplier, this.initPerc, this.morph.getBody(), this.morph.getNumSensors(), (x, y) -> x.getX() == y.getX() && x.getY() == y.getY());
        if (!this.validationFlag) {
            //summarize params
            L.info(String.format("Starting evolution with %s", this.bestFileName));
            //start iterations
            this.performEvolution(this.prepareListenerFactory(), basicFactory);
        }
        else {
            //summarize params
            L.info(String.format("Starting validation with %s", this.bestFileName));
            //start iterations
            this.performEvolution(this.prepareListenerFactory(), new ValidationFactory(1.0, Map.of(new AddNodeMutation(this.parameterSupplier, 0.5, this.connectivity), 0.15, new AddEdgeMutation(this.parameterSupplier, 0.5, this.connectivity), 0.15, new MutateEdge(0.7, 0.0), 0.7),
                    (new ValidationBuilder("fixed", "rewiring")).buildValidation(this.getDonator(), this.getReceiver(), this.morph.getBody(), this.seed), basicFactory));
        }
    }

    private void parseArguments() {
        this.episodeTime = d(a("episodeTime", "30"));
        this.episodeTransientTime = d(a("episodeTransientTime", "5"));
        this.validationEpisodeTime = d(a("validationEpisodeTime", Double.toString(episodeTime)));
        this.validationEpisodeTransientTime = d(a("validationEpisodeTransientTime", Double.toString(episodeTransientTime)));
        this.popSize = i(a("pop", "100"));
        this.births = i(a("births", "200"));
        this.s = Integer.parseInt(Args.a(args, "seed", null));
        this.seed = new Random(s);
        this.parameterSupplier = () -> (this.seed.nextDouble() * 2.0) - 1.0;
        this.terrainNames = l(a("terrain", "hilly-1-10-rnd"));
        String targetSensorConfigName = Args.a(args, "sensorConfig", "spinedTouchSighted-f-f-0.01");
        this.targetShapeName = Args.a(args, "morphology", null);
        this.morph = new Morphology(5, 3, this.targetShapeName, targetSensorConfigName);
        this.initPerc = Double.parseDouble(Args.a(args, "initPerc", "1.0"));
        this.transformationNames = l(a("transformation", "identity"));
        this.validationFlag = Boolean.parseBoolean(a("validation", "false"));
        this.bestFileName = a("bestFile", dir + String.join(".", (validationFlag) ? "validation" : "best", String.valueOf(s), this.targetShapeName, "csv"));
        this.validationFileName = a("validationFile", dir + String.join(".", (validationFlag) ? "validation" : "", "test", String.valueOf(s), this.targetShapeName, "csv"));
        this.validationTransformationNames = l(a("validationTransformation", "identity")).stream().filter(sen -> !sen.isEmpty()).collect(Collectors.toList());
        this.validationTerrainNames = l(a("validationTerrain", "flat,hilly-1-10-0,hilly-1-10-1,hilly-1-10-2,steppy-1-10-0,steppy-1-10-1,steppy-1-10-2")).stream().filter(sen -> !sen.isEmpty()).collect(Collectors.toList());
        this.connectivity = a("connectivity", null);
    }

    private String getDonator() {
        return dir + String.join(".", "best", String.valueOf(this.s), this.targetShapeName, "csv");
    }

    private String getReceiver() {
        return dir + String.join(".", "best", String.valueOf((this.s == 4) ? 0 : this.s + 1), this.targetShapeName, "csv");
    }
    // TODO: could be called directly inside the evolution
    private Listener.Factory<Event<?, ? extends Robot<?>, ? extends Outcome>> prepareListenerFactory() {
        Function<Outcome, Double> fitnessFunction = Outcome::getVelocity;
        //consumers
        List<NamedFunction<Event<?, ? extends Robot<?>, ? extends Outcome>, ?>> basicFunctions = Utils.basicFunctions();
        List<NamedFunction<Event<?, ? extends Robot<?>, ? extends Outcome>, ?>> populationFunctions = Utils.populationFunctions(fitnessFunction);
        List<NamedFunction<Individual<?, ? extends Robot<?>, ? extends Outcome>, ?>> individualFunctions = Utils.individualFunctions(fitnessFunction);
        List<NamedFunction<Outcome, ?>> basicOutcomeFunctions = Utils.basicOutcomeFunctions();
        Listener.Factory<Event<?, ? extends Robot<?>, ? extends Outcome>> factory = Listener.Factory.deaf();
        //screen listener
        //file listeners
        if (this.bestFileName != null) {
            factory = factory.and(new CSVPrinter<>(Misc.concat(List.of(
                    basicFunctions,
                    populationFunctions,
                    NamedFunction.then(best(), individualFunctions),
                    NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), basicOutcomeFunctions),
                    NamedFunction.then(best(), Utils.serializationFunction(true))
            )), new File(this.bestFileName)
            ));
        }
        //validation listener
        if (this.validationFileName != null) {
            Listener.Factory<Event<?, ? extends Robot<?>, ? extends Outcome>> validationFactory = Listener.Factory.forEach(
                    Utils.validation(this.validationTerrainNames, this.validationTransformationNames, List.of(0), this.validationEpisodeTime),
                    new CSVPrinter<>(
                            Misc.concat(List.of(
                                    NamedFunction.then(f("event", (ValidationOutcome vo) -> vo.event), basicFunctions),
                                    NamedFunction.then(f("keys", (ValidationOutcome vo) -> vo.keys), List.of(
                                            f("validation.terrain", (Map<String, Object> map) -> map.get("validation.terrain")),
                                            f("validation.transformation", (Map<String, Object> map) -> map.get("validation.transformation")),
                                            f("validation.seed", "%2d", (Map<String, Object> map) -> map.get("validation.seed"))
                                    )),
                                    NamedFunction.then(
                                            f("outcome", (ValidationOutcome vo) -> vo.outcome.subOutcome(this.validationEpisodeTransientTime, this.validationEpisodeTime)),
                                            basicOutcomeFunctions
                                    )
                            )),
                            new File(this.validationFileName)
                    )
            ).onLast();
            factory = factory.and(validationFactory);
        }
        return factory;
    }

    private void performEvolution(Listener.Factory<Event<?, ? extends Robot<?>, ? extends Outcome>> listenerFactory,
                                  Factory<MyController> genotypeFactory) {
        for (String terrainName : this.terrainNames) {
            for (String transformationName : this.transformationNames) {
                //build evolver
                RobotMapper mapper = new RobotMapper(this.morph.getBody());
                Evolver<MyController, Robot<?>, Outcome> evolver = new StandardEvolver<>(mapper, genotypeFactory, PartialComparator.from(Double.class).reversed().comparing(i -> i.getFitness().getVelocity()),
                        this.popSize, Map.of(new AddNodeMutation(this.parameterSupplier, 0.5, this.connectivity), 0.15, new AddEdgeMutation(this.parameterSupplier, 0.5, this.connectivity), 0.15, new MutateEdge(0.7, 0.0), 0.7),// new CrossoverWithDonation("growing"), 0.1),// new MutateNode(), 0.25),
                        new Tournament(5), new Worst(), this.popSize, true, true);
                Listener<Event<?, ? extends Robot<?>, ? extends Outcome>> listener = Listener.all(List.of(listenerFactory.build()));
                //optimize
                Stopwatch stopwatch = Stopwatch.createStarted();
                try {
                    Collection<Robot<?>> solutions = evolver.solve(
                            buildTaskFromName(transformationName, terrainName, this.episodeTime, this.seed).andThen(o -> o.subOutcome(this.episodeTransientTime, this.episodeTime)),
                            new Births(this.births),
                            this.seed,
                            this.executorService,
                            listener
                    );
                    L.info(String.format("Done %s: %d solutions in %4ds", this.bestFileName, solutions.size(), stopwatch.elapsed(TimeUnit.SECONDS)));
                } catch (Exception e) {
                    L.severe(String.format("Cannot complete due to %s", e));
                    e.printStackTrace();
                }
            }
        }
        listenerFactory.shutdown();
    }
    // TODO: all these static methods could be moved to Utils
    public static Function<Robot<?>, Outcome> buildTaskFromName(String transformationSequenceName, String
            terrainSequenceName, double episodeT, Random random) {
        //for sequence, assume format '99:name>99:name'
        //transformations
        Function<Robot<?>, Robot<?>> transformation;
        if (transformationSequenceName.contains(SEQUENCE_SEPARATOR_CHAR)) {
            transformation = new SequentialFunction<>(getSequence(transformationSequenceName).entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> RobotUtils.buildRobotTransformation(e.getValue(), random)
                            )
                    ));
        } else {
            transformation = RobotUtils.buildRobotTransformation(transformationSequenceName, random);
        }
        //terrains
        Function<Robot<?>, Outcome> task;
        if (terrainSequenceName.contains(SEQUENCE_SEPARATOR_CHAR)) {
            task = new SequentialFunction<>(getSequence(terrainSequenceName).entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> buildLocomotionTask(e.getValue(), episodeT, random)
                            )
                    ));
        } else {
            task = buildLocomotionTask(terrainSequenceName, episodeT, random);
        }
        return task.compose(transformation);
    }

    public static Function<Robot<?>, Outcome> buildLocomotionTask(String terrainName, double episodeT, Random random) {
        if (!terrainName.contains("-rnd")) {
            return Misc.cached(new Locomotion(
                    episodeT,
                    Locomotion.createTerrain(terrainName),
                    PHYSICS_SETTINGS
            ), CACHE_SIZE);
        }
        return r -> new Locomotion(
                episodeT,
                Locomotion.createTerrain(terrainName.replace("-rnd", "-" + random.nextInt(10000))),
                PHYSICS_SETTINGS
        ).apply(r);
    }

    public static SortedMap<Long, String> getSequence(String sequenceName) {
        return new TreeMap<>(Arrays.stream(sequenceName.split(SEQUENCE_SEPARATOR_CHAR)).collect(Collectors.toMap(
                s -> s.contains(SEQUENCE_ITERATION_CHAR) ? Long.parseLong(s.split(SEQUENCE_ITERATION_CHAR)[0]) : 0,
                s -> s.contains(SEQUENCE_ITERATION_CHAR) ? s.split(SEQUENCE_ITERATION_CHAR)[1] : s
        )));
    }

}