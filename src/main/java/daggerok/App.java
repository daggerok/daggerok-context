package daggerok;

import daggerok.context.DaggerokContext;

import java.util.Arrays;

import static java.lang.String.format;

public class App {

  public static final DaggerokContext DAGGEROK_CONTEXT = DaggerokContext.create();

  static {

  }

  public static void main(String[] args) {
    System.out.println(format("daggerok context main stub %s", Arrays.asList(args)));
  }
}
