package daggerok.apps.app;

import daggerok.apps.app.services.HelloService;

import javax.inject.Inject;

public class Main {

  private final HelloService helloService;

  @Inject
  public Main(final HelloService helloService) {
    this.helloService = helloService;
  }

  public void sayHello(final String buddy) {
    System.out.println(helloService.greeting(buddy));
  }
}
