package daggerok.app;

import daggerok.app.data.MyRepository;

import javax.inject.Inject;

public class MyService {

  private final MyRepository myRepository;
  private final MyOtherService myOtherService;

  @Inject
  public MyService(final MyRepository myRepository, final MyOtherService myOtherService) {
    this.myRepository = myRepository;
    this.myOtherService = myOtherService;
  }

  public String logic() {
    return myRepository.logic().toUpperCase() + ":" + myOtherService.logic();
  }
}
