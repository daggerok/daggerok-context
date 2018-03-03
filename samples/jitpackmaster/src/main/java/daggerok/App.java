package daggerok;

import daggerok.app.Main;
import daggerok.app.mappers.InputMapper;
import daggerok.app.services.CapitalizeService;
import daggerok.app.validators.InputValidator;
import daggerok.context.DaggerokContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
  public static void main(String[] args) {

    final DaggerokContext applicationContext = DaggerokContext.create(App.class.getPackage());

    applicationContext.register(InputMapper.class, new InputMapper(applicationContext.getBean(InputValidator.class)))
                      .register(CapitalizeService.class, new CapitalizeService(applicationContext.getBean(InputMapper.class)))
                      .injectBeans();

    final Main main = applicationContext.getBean(Main.class);

    final String name = args.length > 0 ? args[0] : "maksimko!";
    main.sayHello(name);
  }
}
