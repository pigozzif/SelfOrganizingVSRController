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

import buildingBlocks.ControllerFactory;
import buildingBlocks.MyController;
import buildingBlocks.RobotMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import geneticOps.AddEdgeMutation;
import geneticOps.AddNodeMutation;
import geneticOps.MutateEdge;
import geneticOps.MutateNode;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.RobotUtils;
import it.units.malelab.jgea.Worker;
import it.units.malelab.jgea.core.Individual;
import it.units.malelab.jgea.core.evolver.Event;
import it.units.malelab.jgea.core.evolver.Evolver;
import it.units.malelab.jgea.core.evolver.StandardEvolver;
import it.units.malelab.jgea.core.evolver.stopcondition.Births;
import it.units.malelab.jgea.core.evolver.stopcondition.FitnessEvaluations;
import it.units.malelab.jgea.core.listener.*;
import it.units.malelab.jgea.core.listener.telegram.TelegramUpdater;
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

    public Main(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        new Main(args);
    }

    @Override
    public void run() {
        //int spectrumSize = 10;
        //double spectrumMinFreq = 0d;
        //double spectrumMaxFreq = 5d;
        double episodeTime = d(a("episodeTime", "30"));
        double episodeTransientTime = d(a("episodeTransientTime", "5"));
        double validationEpisodeTime = d(a("validationEpisodeTime", Double.toString(episodeTime)));
        double validationEpisodeTransientTime = d(a("validationEpisodeTransientTime", Double.toString(episodeTransientTime)));
        //double videoEpisodeTime = d(a("videoEpisodeTime", "10"));
        //double videoEpisodeTransientTime = d(a("videoEpisodeTransientTime", "0"));
        int popSize = i(a("pop", "100"));
        int births = i(a("births", "200"));
        int seed = Integer.parseInt(Args.a(args, "seed", null));
        //String experimentName = a("expName", "short");
        List<String> terrainNames = l(a("terrain", "hilly-1-10-rnd"));
        String targetSensorConfigName = Args.a(args, "sensorConfig", "spinedTouchSighted-f-f-0.01");
        String targetShapeName = Args.a(args, "morphology", null);
        Morphology morph = new Morphology(5, 3, targetShapeName, targetSensorConfigName);
        double initPerc = Double.parseDouble(Args.a(args, "initPerc", "1.0"));
        List<String> transformationNames = l(a("transformation", "identity"));
        //List<String> evolverNames = l(a("evolver", "ES-10-0.35"));
        //List<String> mapperNames = l(a("mapper", "fixedCentralized<pMLP-2-2-tanh-4.5-0.95-abs_signal_mean"));
        //String lastFileName = a("lastFile", null);
        String bestFileName = a("bestFile", "./output/" + String.join(".", "best", String.valueOf(seed), targetShapeName, "csv"));
        //String allFileName = a("allFile", null);
        String validationFileName = a("validationFile", "./output/" + String.join(".", "validation", String.valueOf(seed), targetShapeName, "csv"));
        //String telegramBotId = a("telegramBotId", null);
        //long telegramChatId = Long.parseLong(a("telegramChatId", "0"));
        //List<String> serializationFlags = l(a("serialization", "")); //last,best,all
        //boolean output = a("output", "false").startsWith("t");
        List<String> validationTransformationNames = l(a("validationTransformation", "identity")).stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
        List<String> validationTerrainNames = l(a("validationTerrain", "flat,hilly-1-10-0,hilly-1-10-1,hilly-1-10-2,steppy-1-10-0,steppy-1-10-1,steppy-1-10-2")).stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
        Function<Outcome, Double> fitnessFunction = Outcome::getVelocity;
        //consumers
        //List<NamedFunction<Event<?, ? extends Robot<?>, ? extends Outcome>, ?>> keysFunctions = Utils.keysFunctions();
        List<NamedFunction<Event<?, ? extends Robot<?>, ? extends Outcome>, ?>> basicFunctions = Utils.basicFunctions();
        //List<NamedFunction<Individual<?, ? extends Robot<?>, ? extends Outcome>, ?>> basicIndividualFunctions = Utils.individualFunctions(fitnessFunction);
        List<NamedFunction<Event<?, ? extends Robot<?>, ? extends Outcome>, ?>> populationFunctions = Utils.populationFunctions(fitnessFunction);
        //List<NamedFunction<Event<?, ? extends Robot<?>, ? extends Outcome>, ?>> visualFunctions = Utils.visualFunctions(fitnessFunction);
        List<NamedFunction<Outcome, ?>> basicOutcomeFunctions = Utils.basicOutcomeFunctions();
        //List<NamedFunction<Outcome, ?>> detailedOutcomeFunctions = Utils.detailedOutcomeFunctions(spectrumMinFreq, spectrumMaxFreq, spectrumSize);
        //List<NamedFunction<Outcome, ?>> visualOutcomeFunctions = Utils.visualOutcomeFunctions(spectrumMinFreq, spectrumMaxFreq);
        Listener.Factory<Event<?, ? extends Robot<?>, ? extends Outcome>> factory = Listener.Factory.deaf();
        //screen listener
        /*if (bestFileName == null || output) {
            factory = factory.and(new TabularPrinter<>(Misc.concat(List.of(
                    basicFunctions,
                    populationFunctions,
                    visualFunctions,
                    NamedFunction.then(best(), basicIndividualFunctions),
                    NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), basicOutcomeFunctions),
                    NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), visualOutcomeFunctions)
            ))));
        }*/
        //file listeners
        /*if (lastFileName != null) {
            factory = factory.and(new CSVPrinter<>(Misc.concat(List.of(
                    //keysFunctions,
                    basicFunctions,
                    populationFunctions,
                    //NamedFunction.then(best(), basicIndividualFunctions),
                    NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), basicOutcomeFunctions)
                    //NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), detailedOutcomeFunctions),
                    //NamedFunction.then(best(), Utils.serializationFunction(serializationFlags.contains("last")))
            )), new File(lastFileName)
            ).onLast());
        }*/
        if (bestFileName != null) {
            factory = factory.and(new CSVPrinter<>(Misc.concat(List.of(
                    //keysFunctions,
                    basicFunctions,
                    populationFunctions,
                    //NamedFunction.then(best(), basicIndividualFunctions),
                    NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), basicOutcomeFunctions),
                    //NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), detailedOutcomeFunctions),
                    NamedFunction.then(best(), Utils.serializationFunction(true))
            )), new File(bestFileName)
            ));
        }
        /*if (allFileName != null) {
            factory = factory.and(Listener.Factory.forEach(
                    event -> event.getOrderedPopulation().all().stream()
                            .map(i -> Pair.of(event, i))
                            .collect(Collectors.toList()),
                    new CSVPrinter<>(
                            Misc.concat(List.of(
                                    NamedFunction.then(f("event", Pair::first), keysFunctions),
                                    NamedFunction.then(f("event", Pair::first), basicFunctions),
                                    NamedFunction.then(f("individual", Pair::second), basicIndividualFunctions),
                                    NamedFunction.then(f("individual", Pair::second), Utils.serializationFunction(serializationFlags.contains("all")))
                            )),
                            new File(allFileName)
                    )
            ));
        }*/
        //validation listener
        if (validationFileName != null) {
            Listener.Factory<Event<?, ? extends Robot<?>, ? extends Outcome>> validationFactory = Listener.Factory.forEach(
                    Utils.validation(validationTerrainNames, validationTransformationNames, List.of(0), validationEpisodeTime),
                    new CSVPrinter<>(
                            Misc.concat(List.of(
                                    NamedFunction.then(f("event", (ValidationOutcome vo) -> vo.event), basicFunctions),
                                    //NamedFunction.then(f("event", (ValidationOutcome vo) -> vo.event), keysFunctions),
                                    NamedFunction.then(f("keys", (ValidationOutcome vo) -> vo.keys), List.of(
                                            f("validation.terrain", (Map<String, Object> map) -> map.get("validation.terrain")),
                                            f("validation.transformation", (Map<String, Object> map) -> map.get("validation.transformation")),
                                            f("validation.seed", "%2d", (Map<String, Object> map) -> map.get("validation.seed"))
                                    )),
                                    NamedFunction.then(
                                            f("outcome", (ValidationOutcome vo) -> vo.outcome.subOutcome(validationEpisodeTransientTime, validationEpisodeTime)),
                                            basicOutcomeFunctions
                                    )
                                    //NamedFunction.then(
                                    //        f("outcome", (ValidationOutcome vo) -> vo.outcome.subOutcome(validationEpisodeTransientTime, validationEpisodeTime))
                                            //detailedOutcomeFunctions
                                    //)
                            )),
                            new File(validationFileName)
                    )
            ).onLast();
            factory = factory.and(validationFactory);
        }
        /*if (telegramBotId != null && telegramChatId != 0) {
            factory = factory.and(new TelegramUpdater<>(List.of(
                    Utils.lastEventToString(fitnessFunction),
                    Utils.fitnessPlot(fitnessFunction),
                    Utils.centerPositionPlot(),
                    Utils.bestVideo(videoEpisodeTransientTime, videoEpisodeTime, PHYSICS_SETTINGS)
            ), telegramBotId, telegramChatId));
        }*/
        //summarize params
        L.info(String.format("Starting evolution with %s", bestFileName));
        //start iterations
        int counter = 0;
        for (String terrainName : terrainNames) {
            for (String transformationName : transformationNames) {
                counter = counter + 1;
                final Random random = new Random(seed);
                //build evolver
                Supplier<Double> parameterSupplier = () -> (random.nextDouble() * 2.0) - 1.0;
                ControllerFactory genotypeFactory = new ControllerFactory(parameterSupplier, initPerc, morph);
                RobotMapper mapper = new RobotMapper(morph);
                Evolver<MyController, Robot<?>, Outcome> evolver = new StandardEvolver<>(mapper, genotypeFactory, PartialComparator.from(Double.class).reversed().comparing(i -> i.getFitness().getVelocity()),
                        popSize, Map.of(new AddNodeMutation(morph, parameterSupplier), 0.1, new AddEdgeMutation(parameterSupplier, 1.0), 0.1, new MutateEdge(0.35, 0.25), 0.8),// new MutateNode(), 0.25),
                        new Tournament(5), new Worst(), popSize, true, true);
                Listener<Event<?, ? extends Robot<?>, ? extends Outcome>> listener = Listener.all(List.of(factory.build()));
                //optimize
                Stopwatch stopwatch = Stopwatch.createStarted();
                try {
                    Collection<Robot<?>> solutions = evolver.solve(
                            buildTaskFromName(transformationName, terrainName, episodeTime, random).andThen(o -> o.subOutcome(episodeTransientTime, episodeTime)),
                            new Births(births),
                            random,
                            this.executorService,
                            listener
                    );
                    L.info(String.format("Done %s: %d solutions in %4ds", bestFileName, solutions.size(), stopwatch.elapsed(TimeUnit.SECONDS)));
                } catch (Exception e) {
                    L.severe(String.format("Cannot complete due to %s", e));
                }
            }
        }
        factory.shutdown();
    }

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