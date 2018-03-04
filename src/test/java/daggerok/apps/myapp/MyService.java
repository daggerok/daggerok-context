package daggerok.apps.myapp;

import daggerok.apps.myapp.data.MyRepository;
import daggerok.apps.myapp.rest.MyClient;

import javax.inject.Inject;

public class MyService {

  private final MyClient myClient;
  private final MyRepository myRepository;

  @Inject
  public MyService(final MyClient myClient, final MyRepository myRepository) {
    this.myClient = myClient;
    this.myRepository = myRepository;
  }

  public void serviceMethod() {
    myClient.clientMethod();
    myRepository.repositoryMethod();
  }
}
