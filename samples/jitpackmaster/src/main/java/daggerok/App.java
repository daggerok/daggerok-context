package daggerok;

import daggerok.apps.app.Main;
import daggerok.context.DaggerokContext;

public class App {
  public static void main(String[] args) {

    final String name = args.length > 0 ? args[0] : "maksimko!";

    final DaggerokContext applicationContext = DaggerokContext.create(App.class)
                                                              .initialize();
    final Main main = applicationContext.getBean(Main.class);

    main.sayHello(name);
  }
}
