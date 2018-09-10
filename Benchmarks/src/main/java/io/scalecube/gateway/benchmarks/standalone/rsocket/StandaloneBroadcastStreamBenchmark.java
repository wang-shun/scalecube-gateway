package io.scalecube.gateway.benchmarks.standalone.rsocket;

import io.scalecube.gateway.benchmarks.BroadcastStreamBenchmark;
import io.scalecube.gateway.benchmarks.standalone.StandaloneBenchmarkState;
import io.scalecube.gateway.clientsdk.Client;
import io.scalecube.gateway.clientsdk.ClientSettings;

public class StandaloneBroadcastStreamBenchmark {

  /**
   * Main runner.
   *
   * @param args program arguments
   */
  public static void main(String[] args) {
    BroadcastStreamBenchmark.runWith(
        args,
        benchmarkSettings ->
            new StandaloneBenchmarkState(
                benchmarkSettings,
                "rsws",
                (address, loopResources) ->
                    Client.onRSocket(
                        ClientSettings.builder()
                            .address(address)
                            .loopResources(loopResources)
                            .build())));
  }
}
