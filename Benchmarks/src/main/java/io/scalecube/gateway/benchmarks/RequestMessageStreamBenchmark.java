package io.scalecube.gateway.benchmarks;

import com.codahale.metrics.Timer;
import io.scalecube.benchmarks.BenchmarksSettings;
import io.scalecube.gateway.benchmarks.example.ExampleService;
import io.scalecube.gateway.clientsdk.ClientMessage;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class RequestMessageStreamBenchmark {

  private static final String QUALIFIER =
      "/" + ExampleService.QUALIFIER + "/requestInfiniteMessageStream";
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

  private RequestMessageStreamBenchmark() {
    // Do not instantiate
  }

  /**
   * Runner function for benchmarks.
   *
   * @param args program arguments
   * @param benchmarkStateFactory producer function for {@link AbstractBenchmarkState}
   */
  public static void runWith(
      String[] args,
      Function<BenchmarksSettings, AbstractBenchmarkState<?>> benchmarkStateFactory) {

    BenchmarksSettings settings =
        BenchmarksSettings.from(args)
            .injectors(1000)
            .messageRate(100_000)
            .rampUpDuration(Duration.ofSeconds(60))
            .executionTaskDuration(Duration.ofSeconds(300))
            .consoleReporterEnabled(true)
            .durationUnit(TimeUnit.MILLISECONDS)
            .build();

    AbstractBenchmarkState<?> benchmarkState = benchmarkStateFactory.apply(settings);

    ClientMessage clientMessage =
        ClientMessage.builder()
            .qualifier(QUALIFIER)
            .header(
                "executionTaskInterval",
                String.valueOf(settings.executionTaskInterval().toMillis()))
            .header(
                "messagesPerExecutionInterval",
                String.valueOf(settings.messagesPerExecutionInterval()))
            .build();

    benchmarkState.runWithRampUp(
        (rampUpTick, state) -> state.createClient(),
        state -> {
          Timer timer = state.timer("latency.timer");
          Timer serviceToGatewayTimer = state.timer("latency.service-to-gw-timer");
          Timer gatewayToClientTimer = state.timer("latency.gw-to-client-timer");

          return (executionTick, client) ->
              client
                  .requestStream(clientMessage)
                  .doOnNext(
                      message -> {
                        long serviceReceivedTime =
                            Long.parseLong(message.headers().get("srv-recd-time"));
                        timer.update(
                            System.currentTimeMillis() - serviceReceivedTime,
                            TimeUnit.MILLISECONDS);

                        calculateReturnLatency(
                            message, serviceToGatewayTimer, gatewayToClientTimer);
                      });
        },
        (state, client) -> client.close());
  }

  private static void calculateReturnLatency(
      ClientMessage message, Timer serviceToGatewayTimer, Timer gatewayToClientTimer) {
    final Map<String, String> headers = message.headers();

    String serviceReceivedTime = headers.get("srv-recd-time");
    String gwReceivedFromServiceTime = headers.get("gw-recd-from-srv-time");
    String clientReceivedTime = headers.get("client-recd-time");

    if (gwReceivedFromServiceTime == null
        || serviceReceivedTime == null
        || clientReceivedTime == null) {
      return;
    }

    long serviceToGatewayTime =
        Long.parseLong(gwReceivedFromServiceTime) - Long.parseLong(serviceReceivedTime);
    serviceToGatewayTimer.update(serviceToGatewayTime, TIME_UNIT);

    long gatewayToClientTime =
        Long.parseLong(clientReceivedTime) - Long.parseLong(gwReceivedFromServiceTime);
    gatewayToClientTimer.update(gatewayToClientTime, TIME_UNIT);
  }
}
