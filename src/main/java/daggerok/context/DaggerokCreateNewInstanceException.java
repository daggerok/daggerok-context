package daggerok.context;

import static java.lang.String.format;

public class DaggerokCreateNewInstanceException extends RuntimeException {
  public DaggerokCreateNewInstanceException(final Class type, final String error) {
    super(format("cannot instantiate '%s'. %s.", type.getName(), error));
  }
}
