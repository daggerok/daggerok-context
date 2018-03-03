package daggerok.app.bad;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Buggy4 {

  @Inject
  public Buggy4(final Buggy3 ololo) {}
}
