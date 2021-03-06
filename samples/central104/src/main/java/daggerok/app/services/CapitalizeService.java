package daggerok.apps.app.services;

import daggerok.apps.app.mappers.InputMapper;

import javax.inject.Inject;

public class CapitalizeService {

  private final InputMapper inputMapper;

  @Inject
  public CapitalizeService(InputMapper inputMapper) {
    this.inputMapper = inputMapper;
  }

  public String capitalize(final String input) {

    final String trimmed = inputMapper.trimmed(input);
    if (null == trimmed) return null;

    final String head = trimmed.substring(0, 1).toUpperCase();
    final String tail = trimmed.length() > 1 ? trimmed.substring(1, trimmed.length()) : "";
    return head + tail;
  }
}
