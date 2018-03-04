package daggerok;

import daggerok.context.DaggerokContext;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class DaggerokContextTest {

  @Test
  public void manual_and_custom_bean_registration_test() {

    final DaggerokContext applicationContext = DaggerokContext.create("")
                                                              .register("java.util.Map", singletonMap("hello", "world"))
                                                              .register(String.class, "Hello, World!")
                                                              .initialize();
    final Map hashMap = applicationContext.getBean(Map.class);

    assertEquals(1, hashMap.size());
    assertEquals("world", hashMap.get("hello"));
    assertEquals("Hello, World!", applicationContext.getBean(String.class));
  }

  @Test
  public void map_registration_test() {

    final DaggerokContext applicationContext = DaggerokContext.create("").initialize();
    final Map<String, String> classMap = applicationContext.getBean(Map.class);
    final Map<String, String> stringMap = applicationContext.getBean("java.util.Map");

    assertEquals(classMap, stringMap);
  }

  @Test
  public void string_registration_test() {

    final DaggerokContext applicationContext = DaggerokContext.create("")
                                                              .register(String.class, "Hello, World!")
                                                              .register("myOtherString", "my other string")
                                                              .initialize();

    final String classString = applicationContext.getBean(String.class);
    final String namedString = applicationContext.getBean("java.lang.String");

    assertEquals(namedString, classString);

    assertEquals("my other string", applicationContext.getBean("myOtherString"));
  }
}
