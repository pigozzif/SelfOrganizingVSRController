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
public class SwitchMain extends Worker {

    private final static Settings PHYSICS_SETTINGS = new Settings();

    public static final int CACHE_SIZE = 0;
    public static final String SEQUENCE_SEPARATOR_CHAR = ">";
    public static final String SEQUENCE_ITERATION_CHAR = ":";
    private static final String dir = "./output/";
    private double episodeTime;
    private double episodeTransientTime;
    private int popSize;
    private int births;
    private Random seed;
    private Supplier<Double> parameterSupplier;
    private List<String> terrainNames;
    private Morphology morph;
    private double initPerc;
    private List<String> transformationNames;
    private String bestFileName;
    private String targetShapeName;
    private boolean isWithTopological;
    private int switchTime;

    public SwitchMain(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        new SwitchMain(args);
    }

    @Override
    public void run() {
        this.parseArguments();
        Factory<MyController> basicFactory = new ControllerFactory(this.parameterSupplier, this.initPerc, this.morph.getBody(), this.morph.getNumSensors(), (x, y) -> x.getX() == y.getX() && x.getY() == y.getY());
        //summarize params
        L.info(String.format("Starting evolution with %s", this.bestFileName));
        //start iterations
        this.performEvolution(this.prepareListenerFactory(), basicFactory);
    }

    private void parseArguments() {
        this.episodeTime = d(a("episodeTime", "30"));
        this.episodeTransientTime = d(a("episodeTransientTime", "5"));
        this.popSize = i(a("pop", "100"));
        this.births = i(a("births", "200"));
        int s = Integer.parseInt(Args.a(args, "seed", null));
        this.seed = new Random(s);
        this.parameterSupplier = () -> (this.seed.nextDouble() * 2.0) - 1.0;
        this.terrainNames = l(a("terrain", "hilly-1-10-rnd"));
        String targetSensorConfigName = Args.a(args, "sensorConfig", "spinedTouchSighted-f-f-0.01");
        this.targetShapeName = Args.a(args, "morphology", null);
        this.morph = new Morphology(5, 3, this.targetShapeName, targetSensorConfigName);
        this.initPerc = Double.parseDouble(Args.a(args, "initPerc", "1.0"));
        this.transformationNames = l(a("transformation", "identity"));
        String configuration = "minimal";
        this.isWithTopological = Boolean.parseBoolean(a("topological", null));
        this.bestFileName = a("bestFile", dir + String.join(".", String.valueOf(this.isWithTopological), String.valueOf(s), this.targetShapeName, configuration, "csv"));
        this.switchTime = i(a("time", null));
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
        return factory;
    }

    private void performEvolution(Listener.Factory<Event<?, ? extends Robot<?>, ? extends Outcome>> listenerFactory,
                                  Factory<MyController> genotypeFactory) {
        for (String terrainName : this.terrainNames) {
            for (String transformationName : this.transformationNames) {
                //build evolver
                RobotMapper mapper = new RobotMapper(this.morph.getBody());
                Evolver<MyController, Robot<?>, Outcome> evolver = new StandardEvolver<>(mapper, genotypeFactory, PartialComparator.from(Double.class).reversed().comparing(i -> i.getFitness().getVelocity()),
                        this.popSize, Map.of(new TimedOperator(this.switchTime, this.parameterSupplier, this.targetShapeName, this.isWithTopological), 1.0),
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