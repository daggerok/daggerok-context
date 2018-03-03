package daggerok.context;

import static java.lang.String.format;

public class Exceptions {

  public static class WrappedReflectionsException extends RuntimeException {
    public WrappedReflectionsException(final Throwable origCause) {
      super(format("Reflections filed: %s", origCause.getLocalizedMessage()), origCause);
    }
  }

  public static class BeanNotFoundException extends RuntimeException {
    public BeanNotFoundException(final Class type) {
      super(format("Injecting bean '%s' wasn't found and resulted in null.", type.getName()));
    }
  }

  public static class CreateNewInstanceException extends RuntimeException {
    public CreateNewInstanceException(final Class type, final String error) {
      super(format("cannot instantiate '%s'. %s.", type.getName(), error));
    }
  }

  private Exceptions() {}
}
