package daggerok.app.validators;

import javax.inject.Singleton;

import static java.lang.String.format;

@Singleton
public class InputValidator {

  public boolean isValid(final String input) {
    return null != input && input.trim().length() > 0;
  }
}
