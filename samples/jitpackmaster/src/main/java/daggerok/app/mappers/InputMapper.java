package daggerok.apps.app.mappers;

import daggerok.apps.app.validators.InputValidator;

import javax.inject.Inject;

public class InputMapper {

  private final InputValidator inputValidator;

  @Inject
  public InputMapper(final InputValidator inputValidator) {
    this.inputValidator = inputValidator;
  }

  public String trimmed(final String input) {
    return inputValidator.isValid(input)
        ? input.trim() : input;
  }
}
