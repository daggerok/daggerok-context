package daggerok;

import daggerok.context.DaggerokContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DaggerokContextTest {

  @Test
  public void get_named_bean_safe_test() {


    final DaggerokContext applicationContext = DaggerokContext.create(Set.class)
                                                              .failOnInjectNullRef(true)
                                                              .register(Set.class,
                                                                        new HashSet<Integer>(Arrays.asList(2, 3, 4)))
                                                              .initialize();

    final Set hashSet = applicationContext.getBean("java.util.Set", Set.class);

    assertEquals(3, hashSet.size());
    assertFalse(hashSet.contains(0));
    assertFalse(hashSet.contains(1));
    assertTrue(hashSet.contains(2));
    assertTrue(hashSet.contains(3));
  }

  @Test
  public void hello_world_test() {


    final DaggerokContext applicationContext = DaggerokContext.create("java.lang")
                                                              .failOnInjectNullRef(true)
                                                              .register(String.class, "Hello, World!")
                                                              .initialize();

    assertEquals("Hello, World!", applicationContext.getBean(String.class));
  }

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

  @Test
  public void register_empty_context_test() {

    final DaggerokContext applicationContext = DaggerokContext.create()
                                                        .initialize();
    assertThat(applicationContext.size()).isEqualTo(1);

    final DaggerokContext updatedContext = applicationContext.register(String.class, "ololo");
    assertThat(updatedContext.size()).isEqualTo(2);

    final String bean = updatedContext.getBean(String.class);
    assertThat(bean).isEqualTo("ololo");
  }
}
