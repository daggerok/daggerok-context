package daggerok.context;

import lombok.extern.slf4j.Slf4j;
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

/**
 * {@link DaggerokContext#create(Class...)}
 * {@link DaggerokContext#create(Package...)}
 * {@link DaggerokContext#create(String...)}
 * <p>
 * {@link DaggerokContext#create(Class, Class...)}
 * {@link DaggerokContext#create(Class, Package...)}
 * {@link DaggerokContext#create(Class, String...)}
 * <p>
 * {@link DaggerokContext#register(String, Object)}
 * {@link DaggerokContext#register(Class, Object)}
 * <p>
 * {@link DaggerokContext#setInjectAnnotation(Class)}
 * {@link DaggerokContext#setFailOnInjectNullRef(boolean)}
 * <p>
 * {@link DaggerokContext#injectBeans()}
 * <p>
 * {@link DaggerokContext#getBean(Class)}
 * {@link DaggerokContext#getBean(String)}
 */
@Slf4j
public class DaggerokContext {

  private List<String> basePackages = new ArrayList<String>();
  private Class<? extends Annotation> injectAnnotation = Inject.class;
  private Class<? extends Annotation> componentAnnotation = Singleton.class;
  private ConcurrentHashMap<String, Object> beans = new ConcurrentHashMap<String, Object>();
  private boolean failOnInjectNullRef = false;

  /* private construct */

  private <C extends Class> DaggerokContext(final C... sources) {
    setBasePackages(sources);
  }

  private <P extends Package> DaggerokContext(final P... packages) {
    setBasePackages(packages);
  }

  private DaggerokContext(final String... packageNames) {
    setBasePackages(packageNames);
  }

  private DaggerokContext setBasePackages(final Class... basePackageClasses) {

    requireNonNull(basePackageClasses, "base classes vararg may not be null.");

    if (0 == basePackageClasses.length) return this;

    final Package[] packages = new Package[basePackageClasses.length];

    for (int i = 0; i < basePackageClasses.length; i++) {

      final Class aClass = basePackageClasses[i];

      requireNonNull(aClass, "class may not be null.");
      packages[i] = aClass.getPackage();
    }

    setBasePackages(packages);
    return this;
  }

  private DaggerokContext setBasePackages(final Package... basePackages) {

    requireNonNull(basePackages, "base packages vararg may not be null.");

    if (0 == basePackages.length) return this;

    final String[] packageNames = new String[basePackages.length];

    for (int i = 0; i < basePackages.length; i++) {

      final Package aPackage = basePackages[i];

      requireNonNull(aPackage, "package may not be null.");
      packageNames[i] = aPackage.getName();
    }

    setBasePackages(packageNames);
    return this;
  }

  private DaggerokContext setBasePackages(final String... basePackages) {

    requireNonNull(basePackages, "base package names vararg may not be null.");

    if (0 == basePackages.length) return this;

    final ArrayList<String> result = new ArrayList<String>();

    for (final String basePackage : basePackages) {
      if ("".equals(basePackage)) {
        log.warn("Detected empty base package!");
        log.warn("We are recommend do not use default or empty base packages for scan");
        log.warn("Please, create context with proper base package for scan.DaggerokContext.create(componentAnnotation)");
        log.warn("See: DaggerokContext.create() API for details");
      }
      if (null != basePackage) result.add(basePackage);
    }

    if (result.size() > 0) this.basePackages.addAll(result);

    return this;
  }

  private DaggerokContext build() {
    this.createNoArgSingletonBeans();
    return this.register(DaggerokContext.class, this);
  }

  /* context initialization */

  /**
   * Step 1: Create application context by application classes.
   * <p>
   * <pre><code>
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(App.class);
   *
   * </pre></code>
   *
   * @param basePackageClasses source - base classes to get packages for annotation / beans scan.
   * @param <C>                could be any class, usually it's class with main method.
   * @return context initialization.
   */
  public static <C extends Class> DaggerokContext create(final C... basePackageClasses) {
    return new DaggerokContext(basePackageClasses).build();
  }

  /**
   * Step 1: Create application context by packages.
   * <p>
   * <pre><code>
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(
   *   App1.class.getPackage(), App2.class.getPackage()
   * );
   *
   * </pre></code>
   *
   * @param basePackages base packages for annotation / beans scan.
   * @param <P>          see Class.getPackage(), usually it's package of application class with main method.
   * @return context initialization.
   */
  public static <P extends Package> DaggerokContext create(final P... basePackages) {
    return new DaggerokContext(basePackages).build();
  }

  /**
   * Step 1: Create application context by package names.
   * <p>
   * <pre><code>
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(
   *   "my.app.package1", "my.app.package2", "my.app.package3"
   * );
   *
   * </pre></code>
   *
   * @param basePackages base packages for annotation / beans scan.
   * @return context initialization.
   */
  public static DaggerokContext create(final String... basePackages) {
    return new DaggerokContext(basePackages).build();
  }

  private <T extends Annotation> DaggerokContext setComponentAnnotation(final Class<T> componentAnnotation) {

    requireNonNull(componentAnnotation, "singleton component annotation may not be null.");

    this.componentAnnotation = componentAnnotation;
    return this;
  }

  /**
   * Step 1: Optionally create application context by component annotation and application classes.
   * <p>
   * <pre><code>
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(
   *   Singleton.class, App.class
   * );
   *
   * </pre></code>
   *
   * @param basePackageClasses source - base classes to get packages for annotation / beans scan.
   * @param <A>                could be any annotation, to be used instead of @{@link Singleton}.
   * @param <C>                could be any class, usually it's class with main method.
   * @return context initialization.
   */
  public static <A extends Annotation, C extends Class> DaggerokContext create(final Class<A> componentAnnotation,
                                                                               final C... basePackageClasses) {

    return new DaggerokContext(basePackageClasses).setComponentAnnotation(componentAnnotation)
                                                  .build();
  }

  /**
   * Step 1: Optionally create application context by component annotation and packages.
   * <p>
   * <pre><code>
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(
   *   Singleton.class,
   *   App1.class.getPackage(),
   *   App2.class.getPackage()
   * );
   *
   * </pre></code>
   *
   * @param basePackages base packages for annotation / beans scan.
   * @param <A>          could be any annotation, to be used instead of @{@link Singleton}.
   * @param <P>          see Class.getPackage(), usually it's package of application class with main method.
   * @return context initialization.
   */
  public static <A extends Annotation, P extends Package> DaggerokContext create(final Class<A> componentAnnotation,
                                                                                 final P... basePackages) {

    return new DaggerokContext(basePackages).setComponentAnnotation(componentAnnotation)
                                            .build();
  }

  /**
   * Step 1: Optionally create application context by component annotation and package names.
   * <p>
   * <pre><code>
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(
   *   Singleton.class,
   *   "my.app.package1",
   *   "my.app.package2",
   *   "my.app.package3"
   * );
   *
   * </pre></code>
   *
   * @param basePackages base packages for annotation / beans scan.
   * @param <A>          could be any annotation, to be used instead of @{@link Singleton}.
   * @return context initialization.
   */
  public static <A extends Annotation> DaggerokContext create(final Class<A> componentAnnotation,
                                                              final String... basePackages) {

    return new DaggerokContext(basePackages).setComponentAnnotation(componentAnnotation)
                                            .build();
  }

  /* context configuration */

  /**
   * Step 2: Optionally register bean by it's class.
   * <p>
   * <pre><code>
   *
   * final MyBean myBean = new Bean("bean initialization...");
   *
   * applicationContext.register(MyBean.class, myBean);
   * applicationContext.register(MyService.class, new MyService(myBean));
   *
   * </pre></code>
   *
   * @param beanType bean class.
   * @param instance bean instance.
   * @param <T>      can any bean instance.
   * @return context configuration.
   */
  public <T> DaggerokContext register(final Class<T> beanType, final T instance) {

    requireNonNull(beanType, "bean type may not be null.");

    final String typeName = beanType.getName();

    register(typeName, instance);
    return this;
  }

  /**
   * Step 2: Optionally register bean by it's full (FQDN) class name.
   * <p>
   * <pre><code>
   *
   * package my.app;
   * // ....
   *
   * final MyBean myBean = new Bean("bean initialization...");
   * final MyOtherBean myOtherBean = new MyOtherBean(myBean);
   *
   * applicationContext.register("my.app.MyBean", myBean);
   * applicationContext.register(myOtherBean.getClass().getName(), myOtherBean);
   * applicationContext.register(MyService.class.getClass().getName(), new MyService(myOtherBean));
   *
   * </pre></code>
   *
   * @param beanName FQDN class name.
   * @param instance bean instance.
   * @param <T>      can any bean instance.
   * @return context configuration.
   */
  public <T> DaggerokContext register(final String beanName, final T instance) {

    requireNonNull(beanName, "bean name type name may not be null.");
    requireNonNull(instance, "instance may not be null.");

    beans.put(beanName, instance);
    return this;
  }

  /**
   * Step 3: Optionally set @{@link Inject} replacement.
   * Must be executed after DaggerokContext.create() was created but before DaggerokContext.injectBeans()
   *
   * @param injectAnnotation replacements for {@link Inject} annotation sor injections scan.
   * @param <T>              can be any annotation.
   * @return context configuration.
   */
  public <T extends Annotation> DaggerokContext setInjectAnnotation(final Class<T> injectAnnotation) {

    requireNonNull(injectAnnotation, "inject constructor annotation may not be null.");

    this.injectAnnotation = injectAnnotation;
    return this;
  }

  /**
   * Step 3: Optionally set if application context bootstrap must fail on any null injection.
   * Must be executed before DaggerokContext.injectBeans()
   *
   * @param failOnInjectNullRef if set to true, application will fail on any null @{@link Inject}s. Default: false.
   * @return context configuration.
   */
  public DaggerokContext setFailOnInjectNullRef(final boolean failOnInjectNullRef) {
    this.failOnInjectNullRef = failOnInjectNullRef;
    return this;
  }

  /* initialized context */

  /**
   * Step 4: Required for @{@link Inject} constructor bean injections.
   * Must be executed as latest step of context configuration, before usage as application context.
   *
   * @return initialized context.
   */
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
      final Object bean = type.cast(instance);

      if (null == bean) {
        log.warn("Bean {} and was resulted in {}", type.getName(), bean);
      }

      register(type.getName(), bean);
    }

    return this;
  }

  /**
   * Gets bean instance by it's class type.
   * <p>
   * <pre><code>
   *
   *   final MyBean myBean = applicationContext.getBean(MyBean.class);
   *
   * </code></pre>
   *
   * @param type bean class type.
   * @param <T>  can be any registered bean:
   *             - manually (explicitly);
   *             - automatically (implicitly) by scanning @{@link Singleton}s with default
   *             or public no-arg constructor in basePackages;
   * @return bean from context if registered otherwise null.
   */
  public <T> T getBean(final Class<T> type) {
    requireNonNull(type, "bean type may not be null.");
    return getBean(type.getName());
  }

  /**
   * Gets bean instance by it's class type.
   * <p>
   * <pre><code>
   *
   *   final MyBean myBean = applicationContext.getBean(MyBean.class);
   *
   * </code></pre>
   *
   * @param typeName full (FQDN) class name. Can be any registered bean:
   *                 - manually (explicitly);
   *                 - automatically (implicitly) by scanning @{@link Singleton}s with default
   *                 or public no-arg constructor in basePackages;
   * @return bean from context if registered otherwise null.
   */
  @SuppressWarnings("unchecked")
  public <T> T getBean(final String typeName) {
    requireNonNull(typeName, "bean name may not be null.");
    return (T) beans.get(typeName);
  }

  /* private API */

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

    requireNotEmpty(basePackages, "list of base packages may not be empty.");

    final Set<Class> components = new HashSet<Class>();

    for (final String basePackage : basePackages) {
      final Reflections reflections = new Reflections(basePackage);
      components.addAll(reflections.getTypesAnnotatedWith(componentAnnotation));
    }

    final List<Constructor> result = new ArrayList<Constructor>();

    for (final Class aClass : components) {

      final Constructor[] constructors = aClass.getConstructors();

      for (final Constructor constructor : constructors) {
        if (0 == constructor.getParameterTypes().length) result.add(constructor);
      }
    }

    return result;
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

    requireNotEmpty(basePackages, "list of base packages may not be empty.");

    final List<Constructor> result = new ArrayList<Constructor>();

    for (final String basePackage : basePackages) {
      final Reflections reflections = new Reflections(basePackage, new MethodAnnotationsScanner());
      result.addAll(reflections.getConstructorsAnnotatedWith(injectAnnotation));
    }

    return result;
  }

  /* helpers */

  private static void requireNonNull(final Object o, final String messages) {
    if (null == o) throw new NullPointerException(messages);
  }

  private void requireNotEmpty(final List<String> basePackages, final String messages) {

    requireNonNull(basePackages, messages);

    final ArrayList<String> result = new ArrayList<String>();

    for (final String packageName : basePackages) {
      if (null != packageName) result.add(packageName);
    }

    if (result.size() < 1) throw new IllegalStateException(messages);
  }
}
