package daggerok.app.bad;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Buggy2 {

  @Inject
  public Buggy2(final String ololo) {}
}
