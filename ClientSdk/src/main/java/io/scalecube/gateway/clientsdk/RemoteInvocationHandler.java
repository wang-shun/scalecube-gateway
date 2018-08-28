package io.scalecube.gateway.clientsdk;

import io.scalecube.gateway.clientsdk.codec.ClientMessageCodec;
import io.scalecube.services.methods.MethodInfo;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public class RemoteInvocationHandler implements InvocationHandler {

  private final ClientTransport transport;
  private final Map<Method, MethodInfo> methods;
  private final ClientMessageCodec messageCodec;

  public RemoteInvocationHandler(
    ClientTransport transport, Map<Method, MethodInfo> methods, ClientMessageCodec messageCodec) {
    this.transport = transport;
    this.methods = methods;
    this.messageCodec = messageCodec;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    MethodInfo methodInfo = methods.get(method);

    ClientMessage request =
      ClientMessage.builder()
        .qualifier(methodInfo.qualifier())
        .data(methodInfo.parameterCount() != 0 ? args[0] : null)
        .build();

    Class<?> responseType = methodInfo.parameterizedReturnType();

    switch (methodInfo.communicationMode()) {
      case REQUEST_RESPONSE:
        return transport
            .requestResponse(request)
            .map(clientMessage -> messageCodec.decodeData(clientMessage, responseType))
            .map(ClientMessage::data);
      case REQUEST_STREAM:
        return transport
            .requestStream(request)
            .map(clientMessage -> messageCodec.decodeData(clientMessage, responseType))
            .map(ClientMessage::data);
      default:
        throw new IllegalArgumentException("Unsupported communication mode");
    }
  }
}
