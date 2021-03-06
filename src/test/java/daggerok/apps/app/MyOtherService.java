package daggerok.apps.app;

import daggerok.apps.app.data.MyRepository;

import javax.inject.Inject;

public class MyOtherService {

  private final MyRepository myRepository;

  @Inject
  public MyOtherService(final MyRepository myRepository) {
    this.myRepository = myRepository;
  }

  public String logic() {
    return myRepository.logic().toUpperCase();
  }
}
