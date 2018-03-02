package daggerok.context;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class DaggerokContext {

  private List<String> basePackages = singletonList("");
  private Class<? extends Annotation> injectAnnotation = Inject.class;
  private Class<? extends Annotation> componentAnnotation = Singleton.class;
  private ConcurrentHashMap<String, Object> beans = new ConcurrentHashMap<String, Object>();

  /* context configuration */

  public <T extends Annotation> DaggerokContext setInjectAnnotation(final Class<T> injectAnnotation) {
    requireNonNull(injectAnnotation, "inject constructor annotation may not be null.");
    this.injectAnnotation = injectAnnotation;
    return this;
  }

  public <T extends Annotation> DaggerokContext setComponentAnnotation(final Class<T> componentAnnotation) {
    requireNonNull(componentAnnotation, "singleton component annotation may not be null.");
    this.componentAnnotation = componentAnnotation;
    return this;
  }

  public DaggerokContext setBasePackages(final String... basePackages) {
    requireNonNull(basePackages, "base packages may not be null.");
    if (basePackages.length > 0) {
      this.basePackages = asList(basePackages);
    }
    return this;
  }

  public DaggerokContext setBasePackageClasses(final Class... basePackageClasses) {
    requireNonNull(basePackageClasses, "base package classes may not be null.");
    if (0 == basePackageClasses.length) return this;
    this.basePackages = new ArrayList<String>();
    for (final Class aClass : basePackageClasses) {
      if (null != aClass) this.basePackages.add(aClass.getPackage().getName());
    }
    return this;
  }

  public <T> DaggerokContext register(final Class<T> beanType, final T instance) {
    requireNonNull(beanType, "bean type may not be null.");
    final String typeName = beanType.getName();
    register(typeName, instance);
    return this;
  }

  public <T> DaggerokContext register(final String beanName, final T instance) {
    requireNonNull(beanName, "bean name type name may not be null.");
    requireNonNull(instance, "instance may not be null.");
    beans.put(beanName, instance);
    return this;
  }

  /* entry point */

  public static DaggerokContext create() {
    final DaggerokContext daggerokContext = new DaggerokContext();
    daggerokContext.createNoArgSingletonBeans();
    return daggerokContext.register(DaggerokContext.class, daggerokContext);
  }

  /* consuming beans */

  public <T> T getBean(final Class<T> type) {
    requireNonNull(type, "bean type may not be null.");
    return getBean(type.getName());
  }

  @SuppressWarnings("unchecked")
  public <T> T getBean(final String typeName) {
    requireNonNull(typeName, "bean name may not be null.");
    return (T) beans.get(typeName);
  }

  /* helpers */

  private DaggerokContext createNoArgSingletonBeans() {

    final List<Constructor> constructors = findNoArgSingletonsConstructors();

    for (final Constructor constructor : constructors) {
      final Class type = constructor.getDeclaringClass();
      final Object instance = newInstance(constructor);
      register(type.getName(), type.cast(instance));
    }

    return this;
  }

  private List<Constructor> findNoArgSingletonsConstructors() {

    final Set<Class> components = new HashSet<Class>();

    for (final String basePackage : basePackages) {
      final Reflections reflections = new Reflections(basePackage);
      components.addAll(reflections.getTypesAnnotatedWith(componentAnnotation));
    }

    final List<Constructor> result = new ArrayList<Constructor>();

    for (final Class aClass : components) {
      final Constructor[] constructors = aClass.getConstructors();
      for (final Constructor constructor : constructors) {
        if (0 == constructor.getParameterTypes().length)
          result.add(constructor);
      }
    }

    return result;
  }

  public DaggerokContext injectBeans() {

    final List<Constructor> injects = findInjects();

    for (final Constructor constructor : injects) {

      final Class type = constructor.getDeclaringClass();
      final Class[] parameterTypes = constructor.getParameterTypes();
      final List<Object> result = new ArrayList<Object>();

      for (final Class aClass : parameterTypes) {
        result.add(beans.get(aClass.getName()));
      }

      final Object[] parameters = result.toArray();
      final Object instance = newInstance(constructor, parameters);

      register(type.getName(), type.cast(instance));
    }

    return this;
  }

  @SuppressWarnings("unchecked")
  private Object newInstance(final Constructor constructor, final Object... parameters) {
    try {
      return constructor.newInstance(parameters);
    } catch (Throwable e) {
      Class type = constructor.getDeclaringClass();
      throw new DaggerokCreateNewInstanceException(type, e.getLocalizedMessage());
    }
  }

  private List<Constructor> findInjects() {

    final List<Constructor> result = new ArrayList<Constructor>();

    for (final String basePackage : basePackages) {
      final Reflections reflections = new Reflections(basePackage, new MethodAnnotationsScanner());
      result.addAll(reflections.getConstructorsAnnotatedWith(injectAnnotation));
    }

    return result;
  }

  private void requireNonNull(final Object o, String messages) {
    if (null == o) throw new NullPointerException(messages);
  }

  private void requireNonNull(final Object o) {
    requireNonNull(o, "argument may not be null.");
  }

  private DaggerokContext() { }
}
