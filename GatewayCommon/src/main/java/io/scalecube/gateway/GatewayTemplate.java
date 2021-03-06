package io.scalecube.gateway;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.scalecube.services.gateway.Gateway;
import io.scalecube.services.gateway.GatewayConfig;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.FutureMono;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.resources.LoopResources;

public abstract class GatewayTemplate implements Gateway {

  private static final int BOSS_THREADS_NUM = 1;

  private EventLoopGroup bossGroup; // calculated

  protected final LoopResources prepareLoopResources(
      boolean preferNative,
      ThreadFactory bossThreadFactory,
      GatewayConfig config,
      Executor workerThreadPool) {

    EventLoopGroup workerGroup =
        (EventLoopGroup) Optional.ofNullable(config.workerThreadPool()).orElse(workerThreadPool);

    if (workerGroup == null) {
      return null;
    }

    bossGroup =
        preferNative
            ? new EpollEventLoopGroup(BOSS_THREADS_NUM, bossThreadFactory)
            : new NioEventLoopGroup(BOSS_THREADS_NUM, bossThreadFactory);

    return new GatewayLoopResources(preferNative, bossGroup, workerGroup);
  }

  protected final HttpServer prepareHttpServer(
      LoopResources loopResources, GatewayMetrics gatewayMetrics, int port) {
    return HttpServer.create(
        opts -> {
          opts.listenAddress(new InetSocketAddress(port));
          if (loopResources != null) {
            opts.loopResources(loopResources);
          }
          opts.afterNettyContextInit(
              nc -> {
                gatewayMetrics.incConnection();
                nc.onClose(gatewayMetrics::decConnection);
              });
        });
  }

  protected final Mono<Void> shutdownBossGroup() {
    //noinspection unchecked
    return Mono.defer(
        () ->
            bossGroup == null
                ? Mono.empty()
                : FutureMono.from((Future) ((EventLoopGroup) bossGroup).shutdownGracefully()));
  }
}
