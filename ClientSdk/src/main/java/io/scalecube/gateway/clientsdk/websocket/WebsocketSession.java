package io.scalecube.gateway.clientsdk.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.scalecube.gateway.clientsdk.ClientMessage;
import io.scalecube.gateway.clientsdk.codec.ClientMessageCodec;
import java.util.logging.Level;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyPipeline.SendOptions;
import reactor.ipc.netty.http.websocket.WebsocketInbound;
import reactor.ipc.netty.http.websocket.WebsocketOutbound;

final class WebsocketSession {

  private final WebsocketInbound inbound;
  private final WebsocketOutbound outbound;
  private final ClientMessageCodec<ByteBuf> codec;

  WebsocketSession(
      WebsocketInbound inbound, WebsocketOutbound outbound, ClientMessageCodec<ByteBuf> codec) {
    this.inbound = inbound;
    this.outbound = outbound;
    this.codec = codec;
  }

  public Mono<Void> send(ClientMessage message) {
    return Mono.defer(
        () ->
            outbound
                .options(SendOptions::flushOnEach)
                .sendObject(Mono.just(message).map(codec::encode).map(BinaryWebSocketFrame::new))
                .then()
                .log("<<< SEND", Level.INFO));
  }

  public Flux<ClientMessage> receive() {
    return Flux.defer(
        () ->
            inbound
                .aggregateFrames()
                .receive()
                .map(ByteBuf::retain)
                .map(codec::decode)
                .log(">>> RECEIVE", Level.INFO));
  }

  public Mono<Void> close() {
    return Mono.defer(
        () ->
            outbound
                .sendObject(new CloseWebSocketFrame(1000, "close"))
                .then()
                .log("<<< CLOSE", Level.INFO));
  }

  public Mono<Void> onClose(Runnable runnable) {
    return Mono.defer(() -> inbound.context().onClose(runnable).onClose());
  }
}
