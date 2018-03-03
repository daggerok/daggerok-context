package daggerok;

import daggerok.app.MyOtherService;
import daggerok.app.MyService;
import daggerok.app.data.MyRepository;
import daggerok.context.DaggerokContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DaggerokContextTest {

  @Test
  public void testContext() {
    final DaggerokContext applicationContext = DaggerokContext.create();
    /*
    // DaggerokContext.create() should do next automatically (inject no-args Singletons):
    applicationContext.register(MyRepository.class, new MyRepository());
    */

    final MyRepository myRepository = applicationContext.getBean(MyRepository.class);
    applicationContext.register(MyOtherService.class, new MyOtherService(myRepository));

    applicationContext.injectBeans();
    /*
    // applicationContext.injectBeans() should do next automatically (inject constructors annotated with @Inject using beans from context):
    final MyOtherService myOtherService = applicationContext.getBean(MyOtherService.class);
    applicationContext.register(MyService.class, new MyService(myRepository, myOtherService));
    */

    final MyService myService = applicationContext.getBean(MyService.class);
    assertEquals("LOGIC:LOGIC", myService.logic());
  }
}
