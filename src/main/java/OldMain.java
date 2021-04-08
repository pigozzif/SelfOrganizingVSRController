import buildingBlocks.ControllerFactory;
import buildingBlocks.MyController;
import buildingBlocks.RobotMapper;
import com.google.common.base.Stopwatch;
import geneticOps.AddEdgeMutation;
import geneticOps.AddNodeMutation;
import geneticOps.MutateEdge;
import geneticOps.MutateNode;
import it.units.erallab.hmsrobots.core.objects.Robot;
import it.units.erallab.hmsrobots.tasks.locomotion.Locomotion;
import it.units.erallab.hmsrobots.tasks.locomotion.Outcome;
import it.units.erallab.hmsrobots.util.SerializationUtils;
import it.units.malelab.jgea.Worker;
import it.units.malelab.jgea.core.Individual;
import it.units.malelab.jgea.core.evolver.Event;
import it.units.malelab.jgea.core.evolver.Evolver;
import it.units.malelab.jgea.core.evolver.StandardEvolver;
import it.units.malelab.jgea.core.evolver.stopcondition.Births;
import it.units.malelab.jgea.core.listener.CSVPrinter;
import it.units.malelab.jgea.core.listener.Listener;
import it.units.malelab.jgea.core.listener.NamedFunction;
import it.units.malelab.jgea.core.order.PartialComparator;
import it.units.malelab.jgea.core.selector.Tournament;
import it.units.malelab.jgea.core.selector.Worst;
import it.units.malelab.jgea.core.util.Args;
import it.units.malelab.jgea.core.util.Misc;
import morphologies.Morphology;
import org.dyn4j.dynamics.Settings;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static it.units.malelab.jgea.core.listener.NamedFunctions.*;

// hilly-1-10-rrnd, remap to avoid favouring easy terrains
// 5x3 worm, 4x3 biped, both with spinedTouchVisual
// enable also validation, maybe also with uphill-10 and downhill-10
public class OldMain extends Worker {

    private static Random random;
    private static Morphology morph;
    private static double initPerc;
    private static double episodeTime;
    private static int nBirths;
    private static String outputFile;

    public static void main(String[] args) {
        int seed = Integer.parseInt(Args.a(args, "seed", null));
        String sensors = Args.a(args, "sensors", "vel-area-touch");
        String morphology = Args.a(args, "morphology", null);
        morph = Morphology.createMorphology(morphology, sensors);
        initPerc = Double.parseDouble(Args.a(args, "initPerc", "1.0"));
        episodeTime = Double.parseDouble(Args.a(args, "episodeTime", "30.0"));
        nBirths = Integer.parseInt(Args.a(args, "births", "30000"));
        outputFile = Args.a(args, "outputFileName", "./output/" + seed + "." + morphology + ".csv");
        random = new Random(seed);
        new OldMain(args);
    }

    public OldMain(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        Supplier<Double> parameterSupplier = () -> (random.nextDouble() * 2.0) - 1.0;
        ControllerFactory factory = new ControllerFactory(parameterSupplier, initPerc, morph);
        RobotMapper mapper = new RobotMapper(morph);
        Settings physicsSettings = new Settings();
        Evolver<MyController, Robot<?>, Outcome> evolver = new StandardEvolver<>(mapper, factory, PartialComparator.from(Double.class).reversed().comparing(i -> i.getFitness().getDistance()),
                100, Map.of(new AddNodeMutation(morph, parameterSupplier), 0.25, new AddEdgeMutation(parameterSupplier, 1.0), 0.25, new MutateEdge(0.35, 0.0), 0.25, new MutateNode(), 0.25),
                new Tournament(5), new Worst(), 100, true, true);
        Function<Robot<?>, Outcome> trainingTask = new Locomotion(episodeTime, Locomotion.createTerrain("flat"), physicsSettings);
        L.info(String.format("Starting evolution with %s", outputFile));
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            Collection<Robot<?>> solutions = evolver.solve(trainingTask, new Births(nBirths), random, this.executorService, createListenerFactory().build());
            L.info(String.format("Done %s: %d solutions in %4ds", outputFile, solutions.size(), stopwatch.elapsed(TimeUnit.SECONDS)));
        } catch (InterruptedException | ExecutionException e) {
            L.severe(String.format("Cannot complete %s due to %s", outputFile, e));
            e.printStackTrace();
        }
    }

    private Listener.Factory<? super Event<?, ? extends Robot<?>, ? extends Outcome>> createListenerFactory() {
        List<NamedFunction<Event<?, ? extends Robot<?>, ? extends Outcome>, ?>> basicFunctions = List.of(iterations(), births(), fitnessEvaluations(), elapsedSeconds(),
                size().of(all()),
                size().of(firsts()),
                size().of(lasts()),
                uniqueness().of(each(genotype())).of(all()),
                uniqueness().of(each(solution())).of(all()),
                uniqueness().of(each(fitness())).of(all()));
        List<NamedFunction<Individual<?, ? extends Robot<?>, ? extends Outcome>, ?>> serializedFunction = List.of(
                f("serialized", r -> SerializationUtils.serialize(r, SerializationUtils.Mode.GZIPPED_JSON)).of(solution()));
        List<NamedFunction<Outcome, ?>> outcomeFunctions = List.of(f("computation.time", "%4.2f", Outcome::getComputationTime),
                f("distance", "%5.1f", Outcome::getDistance),
                f("velocity", "%5.1f", Outcome::getVelocity),
                f("corrected.efficiency", "%5.2f", Outcome::getCorrectedEfficiency),
                f("area.ratio.power", "%5.1f", Outcome::getAreaRatioPower),
                f("control.power", "%5.1f", Outcome::getControlPower));
        return new CSVPrinter<>(Misc.concat(List.of(basicFunctions, NamedFunction.then(as(Outcome.class).of(fitness()).of(best()), outcomeFunctions),
                NamedFunction.then(best(), serializedFunction))), new File(outputFile));
    }

}
