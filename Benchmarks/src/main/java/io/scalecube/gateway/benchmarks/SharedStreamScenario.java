package io.scalecube.gateway.benchmarks;

import io.scalecube.benchmarks.BenchmarkSettings;
import io.scalecube.gateway.clientsdk.ClientMessage;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;

public final class SharedStreamScenario {

  private static final Logger LOGGER = LoggerFactory.getLogger(SharedStreamScenario.class);

  public static final String QUALIFIER = "/benchmarks/sharedStream";

  private static final String RATE_LIMIT = "rateLimit";

  private SharedStreamScenario() {
    // Do not instantiate
  }

  /**
   * Runner function for benchmarks.
   *
   * @param args program arguments
   * @param benchmarkStateFactory producer function for {@link AbstractBenchmarkState}
   */
  public static void runWith(
      String[] args, Function<BenchmarkSettings, AbstractBenchmarkState<?>> benchmarkStateFactory) {

    int numOfThreads = Runtime.getRuntime().availableProcessors();
    Duration rampUpDuration = Duration.ofSeconds(numOfThreads);

    BenchmarkSettings settings =
        BenchmarkSettings.from(args)
            .injectors(numOfThreads)
            .messageRate(1) // workaround
            .warmUpDuration(Duration.ofSeconds(30))
            .rampUpDuration(rampUpDuration)
            .executionTaskDuration(Duration.ofSeconds(600))
            .consoleReporterEnabled(true)
            .durationUnit(TimeUnit.MILLISECONDS)
            .build();

    AbstractBenchmarkState<?> benchmarkState = benchmarkStateFactory.apply(settings);

    benchmarkState.runWithRampUp(
        (rampUpTick, state) -> state.createClient(),
        state -> {
          LatencyHelper latencyHelper = new LatencyHelper(state);
          Integer rateLimit =
              Optional.ofNullable(settings.find(RATE_LIMIT, null))
                  .map(Integer::parseInt)
                  .orElse(null);
          ClientMessage request =
              ClientMessage.builder().qualifier(QUALIFIER).rateLimit(rateLimit).build();

          return client ->
              (executionTick, task) ->
                  client
                      .requestStream(request, Schedulers.parallel())
                      .doOnError(th -> LOGGER.warn("Exception occured on requestStream: " + th))
                      .doOnNext(latencyHelper::calculate);
        },
        (state, client) -> client.close());
  }
}
