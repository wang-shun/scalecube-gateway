package io.scalecube.gateway.http;

import io.scalecube.services.Microservices;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import reactor.ipc.netty.http.client.HttpClient;

public class GatewayHttpExtension implements AfterAllCallback {

  private HttpClient client;
  private InetSocketAddress address;

  public GatewayHttpExtension startGateway(Microservices gateway) {
    address = gateway.gatewayAddress("HttpGateway", HttpGateway.class);
    client = HttpClient.create(address.getPort());
    return this;
  }

  public InetSocketAddress httpAddress() {
    return address;
  }

  public HttpClient client() {
    return client;
  }

  @Override
  public void afterAll(ExtensionContext context) {
    // noop
  }
}
