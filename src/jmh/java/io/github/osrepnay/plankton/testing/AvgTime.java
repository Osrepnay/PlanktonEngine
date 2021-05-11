package io.github.osrepnay.plankton.testing;

import io.github.osrepnay.plankton.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Fork(0)
public class AvgTime {

    @Benchmark
    public double[] bestMove() {
        Game game = new Game();
        Plankton engine = new Plankton();
        return engine.bestMove(game, 0, 5);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(AvgTime.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
