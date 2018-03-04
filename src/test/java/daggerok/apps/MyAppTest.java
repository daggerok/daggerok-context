package daggerok.apps;

import daggerok.apps.myapp.MyService;
import daggerok.context.DaggerokContext;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class MyAppTest {

  private ByteArrayOutputStream captureOutput() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    return outputStream;
  }

  @Test
  public void my_app_test() {

    final DaggerokContext applicationContext = DaggerokContext.create(MyAppTest.class).initialize();
    final MyService myService = applicationContext.getBean(MyService.class);

    final ByteArrayOutputStream testOutput1 = captureOutput();
    myService.serviceMethod();
    final String actual1 = testOutput1.toString();

    assertTrue(actual1.contains("MyClient.clientMethod"));
    assertTrue(actual1.contains("0"));
    assertTrue(actual1.contains("MyRepository.repositoryMethod"));

    applicationContext.getBean(HashMap.class).put("message", "hello!");

    final ByteArrayOutputStream testOutput2 = captureOutput();
    myService.serviceMethod();
    assertTrue(testOutput2.toString().contains("hello!"));
  }
}
