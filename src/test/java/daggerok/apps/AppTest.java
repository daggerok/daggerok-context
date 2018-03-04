package daggerok.apps;

import daggerok.apps.app.MyService;
import daggerok.context.DaggerokContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppTest {

  @Test
  public void app_test() {

    final DaggerokContext applicationContext = DaggerokContext.create("").initialize();
    final MyService myService = applicationContext.getBean(MyService.class);

    assertEquals("LOGIC:LOGIC", myService.logic());
  }

  @Test
  public void all_packages_scan_performance_test() {

    final DaggerokContext applicationContext = DaggerokContext.create(Package.getPackages()).initialize();
    final MyService myService = applicationContext.getBean(MyService.class);

    assertEquals("LOGIC:LOGIC", myService.logic());
  }
}
