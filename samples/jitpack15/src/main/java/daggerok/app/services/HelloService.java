package daggerok.app.services;

import javax.inject.Inject;

import static java.lang.String.format;

public class HelloService {

  private final CapitalizeService capitalizeService;

  @Inject
  public HelloService(final CapitalizeService capitalizeService) {
    this.capitalizeService = capitalizeService;
  }

  public String greeting(final String name) {
    final String capitalizedName = capitalizeService.capitalize(name);
    return format("Hello, %s", capitalizedName);
  }
}
