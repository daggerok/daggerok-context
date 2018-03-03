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

    final DaggerokContext applicationContext = DaggerokContext.create();

    applicationContext.register(InputMapper.class, new InputMapper(applicationContext.getBean(InputValidator.class)))
                      .register(CapitalizeService.class, new CapitalizeService(applicationContext.getBean(InputMapper.class)))
                      .setBasePackageClasses(App.class)
                      .injectBeans();

    final Main main = applicationContext.getBean(Main.class);
    main.sayHello("maksimko!");
  }
}
