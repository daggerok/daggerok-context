package daggerok.apps.myapp.rest;

import javax.inject.Inject;
import java.util.HashMap;

public class MyClient {

  private final HashMap<String, Object> config;

  @Inject
  public MyClient(final HashMap<String, Object> config) {
    this.config = config;
  }

  public void clientMethod() {
    System.out.println("MyClient.clientMethod");
    final Object message = config.get("message");
    System.out.println(null == message ? config.size() : message.toString());
  }
}
