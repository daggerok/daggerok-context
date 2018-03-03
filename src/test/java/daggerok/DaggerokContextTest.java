package daggerok;

import daggerok.app.MyService;
import daggerok.context.DaggerokContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DaggerokContextTest {

  @Test
  public void application_context_test() {

    final DaggerokContext applicationContext = DaggerokContext.create("").initialize();

    final MyService myService = applicationContext.getBean(MyService.class);

    assertEquals("LOGIC:LOGIC", myService.logic());
  }

  @Test
  public void all_packages_scan_performance_functional_test() {

    final DaggerokContext applicationContext = DaggerokContext.create(Package.getPackages()).initialize();

    final MyService myService = applicationContext.getBean(MyService.class);

    assertEquals("LOGIC:LOGIC", myService.logic());
  }
}
