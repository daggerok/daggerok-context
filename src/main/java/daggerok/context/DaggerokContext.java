package daggerok.context;

import daggerok.context.Exceptions.BeanNotFoundException;
import daggerok.context.Exceptions.CreateNewInstanceException;
import daggerok.context.Finders.FinderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static daggerok.context.Requires.requireNonNull;

/**
 * High overview of DaggerokContext public API:
 * <p>
 *
 * Entry point: create uninitialized context using:
 * {@link DaggerokContext#create()}
 * {@link DaggerokContext#create(Class...)}
 * {@link DaggerokContext#create(Package...)}
 * {@link DaggerokContext#create(String...)}
 * <p>
 *
 * User configurations:
 * {@link DaggerokContext#withBasePackageClasses(Class...)}
 * {@link DaggerokContext#withBasePackageNames(String...)}
 * {@link DaggerokContext#withBasePackages(Package...)}
 * {@link DaggerokContext#withComponents(Class)}
 * {@link DaggerokContext#withInjectors(Class)}
 * {@link DaggerokContext#failOnInjectNullRef(boolean)}
 * {@link DaggerokContext#failOnBeanCreationError(boolean)}
 * {@link DaggerokContext#failOnUnknownReflectionsErrors(boolean)}
 * <p>
 *
 * Manual beans registration:
 * {@link DaggerokContext#register(String, Object)}
 * {@link DaggerokContext#register(Class, Object)}
 * <p>
 *
 * Search, create and inject everything we can:
 * {@link DaggerokContext#initialize()}
 * <p>
 *
 * Get bean from context - could be used before initialize() if bean was previously manually added:
 * {@link DaggerokContext#getBean(Class)}
 * {@link DaggerokContext#getBean(String, Class)}
 * {@link DaggerokContext#getBean(String)}
 */
public class DaggerokContext extends ConcurrentHashMap<Integer, HashSet<Constructor>> {

  private static final Logger log = LoggerFactory.getLogger(DaggerokContext.class);

  private final List<String> basePackages = new ArrayList<String>();
  private final ConcurrentHashMap<String, Object> beans = new ConcurrentHashMap<String, Object>();

  private Class<? extends Annotation> injectAnnotation = Inject.class;
  private Class<? extends Annotation> componentAnnotation = Singleton.class;
  private boolean failOnInjectNullRef = false;
  private boolean failOnBeanCreationError = false;
  private boolean failOnUnknownReflectionsErrors = false;

  /* public API */

  /* context creation */

  /**
   * Step 1: Initialize default application context.
   *
   * final DaggerokContext applicationContext = DaggerokContext.create();
   *
   * This will create empty application context with only 1 Bean: itself registered DaggerokContext
   *
   * @return context initialization.
   */
  public static DaggerokContext create() {
    return create(DaggerokContext.class);
  }

  /**
   * Step 1: Initialize application context by application classes.
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(App.class);
   *
   * @param baseClasses optional additional sources.
   * @return context initialization.
   */
  public static DaggerokContext create(final Class... baseClasses) {
    return new DaggerokContext(baseClasses);
  }

  /**
   * Step 1: Initialize application context by packages.
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(
   *   App1.class.getPackage(), App2.class.getPackage()
   * );
   *
   * @param basePackages base packages for annotation / beans scan. see Class.getPackage(),
   *                     usually it's package of application class with main method.
   * @return context initialization.
   */
  public static DaggerokContext create(final Package... basePackages) {
    return new DaggerokContext(basePackages);
  }

  /**
   * Step 1: Initialize application context by package names.
   *
   * final DaggerokContext applicationContext = DaggerokContext.create(
   *   "my.app.package1", "my.app.package2", "my.app.package3"
   * );
   *
   * @param basePackages base packages for annotation / beans scan.
   * @return context initialization.
   */
  public static DaggerokContext create(final String... basePackages) {
    return new DaggerokContext(basePackages);
  }

  /* context configuration */

  /**
   * Step 2: (Optional / if created default context) Add base ckasses to get packages for injections scan.
   *
   * @param baseClasses base classes to get it's packages to components and injections scan.
   * @return context initialization.
   */
  public DaggerokContext withBasePackageClasses(final Class... baseClasses) {
    requireNonNull(baseClasses, "base package classes");
    if (0 == baseClasses.length) return this;
    final Package[] packages = new Package[baseClasses.length];
    for (int i = 0; i < baseClasses.length; i++) {
      final Class aClass = baseClasses[i];
      requireNonNull(aClass, "a package class");
      packages[i] = aClass.getPackage();
    }
    withBasePackages(packages);
    return this;
  }

  /**
   * Step 2: (Optional / if created default context) Add base package names for injections scan.
   *
   * @param basePackageNames base packages to components and injections scan.
   * @return context initialization.
   */
  public DaggerokContext withBasePackageNames(final String... basePackageNames) {
    requireNonNull(basePackageNames, "base package");
    if (0 == basePackageNames.length) return this;
    final ArrayList<String> packages = new ArrayList<String>();
    for (final String basePackage : basePackageNames) {
      if ("".equals(basePackage)) log.warn(
          "Detected empty base package! We don't recommend use all included empty packages. It can decrease " +
              "performance dramatically and cause some unknown errors depends on 3rd party libraries your are " +
              "using. Please, create context with proper base package for scan including your application " +
              "components only"
      );
      if (null != basePackage) packages.add(basePackage);
    }
    if (packages.size() > 0) this.basePackages.addAll(packages);
    return this;
  }

  /**
   * Step 2: (Optional / if created default context) Add packages for injections scan.
   *
   * @param basePackages base packages to components and injections scan.
   * @return context initialization.
   */
  public DaggerokContext withBasePackages(final Package... basePackages) {
    requireNonNull(basePackages, "base packages");
    if (0 == basePackages.length) return this;
    final String[] packageNames = new String[basePackages.length];
    for (int i = 0; i < basePackages.length; i++) {
      final Package aPackage = basePackages[i];
      requireNonNull(aPackage, "a package");
      packageNames[i] = aPackage.getName();
    }
    withBasePackageNames(packageNames);
    return this;
  }

  /**
   * Step 2: Optionally configure component annotation. Default: {@link Singleton}
   *
   * Must be performed before applicationContext.initialize()
   *
   * applicationContext.withComponents(Singleton.class);
   *
   * @param componentAnnotation user specific components annotation.
   * @param <A> could be any annotation.
   * @return context initialization. Throw {@link NullPointerException} if passed component annotation is null.
   */
  public <A extends Annotation> DaggerokContext withComponents(final Class<A> componentAnnotation) {
    requireNonNull(componentAnnotation, "component annotation");
    this.componentAnnotation = componentAnnotation;
    return this;
  }

  /**
   * Step 2: Optionally configure injector annotation. Default: {@link Inject}
   *
   * Must be performed before applicationContext.initialize()
   *
   * applicationContext.withInjectors(Inject.class);
   *
   * @param injectAnnotation replacements for {@link Inject} annotation sor injections scan.
   * @param <A> could be any annotation.
   * @return context initialization. Throw {@link NullPointerException} if passed injector annotation is null.
   */
  public <A extends Annotation> DaggerokContext withInjectors(final Class<A> injectAnnotation) {
    requireNonNull(injectAnnotation, "inject annotation");
    this.injectAnnotation = injectAnnotation;
    return this;
  }

  /**
   * Step 2: Optionally configure if application context bootstrap must fail on any null injection.
   *
   * Must be performed before applicationContext.initialize(),
   * Can be useful for debug.
   *
   * @param failOnInjectNullRef if set to true, application will fail on any null @{@link Inject}s. Default: false.
   * @return context configuration.
   */
  public DaggerokContext failOnInjectNullRef(final boolean failOnInjectNullRef) {
    this.failOnInjectNullRef = failOnInjectNullRef;
    return this;
  }

  /**
   * Step 2: Optionally configure if application context bootstrap must fail on bean creation new instance errors.
   * Default: false.
   *
   * Must be performed before applicationContext.initialize(),
   * We believe you don't need enable it, unless debug purposes.
   *
   * @param failOnBeanCreationError if set to true, application will fail on any new beans creations errors.
   *                                Default: false.
   * @return context configuration.
   */
  public DaggerokContext failOnBeanCreationError(final boolean failOnBeanCreationError) {
    this.failOnBeanCreationError = failOnBeanCreationError;
    return this;
  }

  /**
   * Step 2: Optionally configure if application context bootstrap must fail on unknown possible exceptions catches.
   * Default: false.
   *
   * Must be performed before applicationContext.initialize(),
   * Use it only if you know what you are doing and you believe you configured out everything.
   *
   * @param failOnUnknownErrors if set to true, application will fail on any null @{@link Inject}s. Default: false.
   * @return context configuration.
   */
  public DaggerokContext failOnUnknownReflectionsErrors(final boolean failOnUnknownErrors) {
    this.failOnUnknownReflectionsErrors = failOnUnknownErrors;
    return this;
  }

  /* manual context registration */

  /**
   * Step 3: Optionally in addition manually register bean by it's class.
   *
   * final MyBean myBean = new Bean("bean initialization...");
   *
   * applicationContext.register(MyBean.class, myBean);
   * applicationContext.register(MyService.class, new MyService(myBean));
   *
   * @param beanType bean class.
   * @param instance bean instance.
   * @param <T>      can any bean instance.
   * @return context configuration.
   */
  public <T> DaggerokContext register(final Class<T> beanType, final T instance) {
    requireNonNull(beanType, "bean type");
    register(beanType.getName(), instance);
    return this;
  }

  /**
   * Step 3: Optionally in addition manually register bean by it's full (FQDN) class name.
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
   * @param beanName FQDN class name.
   * @param instance bean instance.
   * @param <T>      can any bean instance.
   * @return context configuration.
   */
  public <T> DaggerokContext register(final String beanName, final T instance) {
    requireNonNull(beanName, "bean name type");
    requireNonNull(instance, "instance");
    beans.put(beanName, instance);
    return this;
  }

  /* context initialization */

  /**
   * Step 4: Context initialization.
   *
   * Scanning for components and registering beans for application context will be returned.
   *
   * @return context configuration.
   */
  public DaggerokContext initialize() {
    return findAndRegisterAllBeans().register(DaggerokContext.class, this);
  }

  /* context usage */

  /**
   * Step 5: Gets bean instance by it's class type.
   *
   * Can be used before applicationContext.initialize() only if bean you are looking for previously was manually added.
   *
   *   final MyBean myBean = applicationContext.getBean(MyBean.class);
   *
   * @param type bean class type.
   * @param <T>  can be any registered bean:
   *             - manually (explicitly);
   *             - automatically (implicitly) by scanning @{@link Singleton}s with default
   *             or public no-arg constructor in basePackages;
   * @return bean from context if registered otherwise null.
   */
  public <T> T getBean(final Class<T> type) {
    requireNonNull(type, "bean type");
    return getBean(type.getName());
  }

  /**
   * Step 5: Gets named bean instance by it's type.
   *
   * Can be used before applicationContext.initialize() only if bean you are looking for previously was manually added.
   *
   *   final MyBean myBean = applicationContext.getBean(MyBean.class);
   *
   * @param <T>  bean type
   * @param type bean class
   * @param name full (FQDN) class name. Can be any registered bean:
   *             - manually (explicitly);
   *             - automatically (implicitly) by scanning @{@link Singleton}s with default
   *             or public no-arg constructor in basePackages;
   * @return bean from context if registered otherwise null.
   */
  public <T> T getBean(final String name, final Class<T> type) {
    requireNonNull(name, "bean name");
    return type.cast(beans.get(name));
  }

  /**
   * Step 5: Gets named bean instance (unsafe ).
   *
   * Can be used before applicationContext.initialize() only if bean you are looking for previously was manually added.
   *
   *   final MyBean myBean = applicationContext.getBean(MyBean.class);
   *
   * @param <T>      bean type
   * @param typeName full (FQDN) class name. Can be any registered bean:
   *                 - manually (explicitly);
   *                 - automatically (implicitly) by scanning @{@link Singleton}s with default
   *                 or public no-arg constructor in basePackages;
   * @return bean from context if registered otherwise null.
   */
  @SuppressWarnings("unchecked")
  public <T> T getBean(final String typeName) {
    requireNonNull(typeName, "bean name");
    return (T) beans.get(typeName);
  }

  /* overrides */

  @Override
  public int size() {
    return beans.size();
  }

  /* private API */

  /* construct context required components base package scan initialization */

  private <C extends Class> DaggerokContext(final C... sources) {
    withBasePackageClasses(sources);
  }

  private <P extends Package> DaggerokContext(final P... packages) {
    withBasePackages(packages);
  }

  private DaggerokContext(final String... packageNames) {
    withBasePackageNames(packageNames);
  }

  /* components base package scan initialization */

  /* helpers and DRY methods */

  /**
   * Scan for component classes and context initialization.
   *
   * @return context initialization.
   */
  private DaggerokContext findAndRegisterAllBeans() {
    createNoArgComponents();
    injectConstructorsInstances();
    return this;
  }

  /**
   * Try to find and initialize all no-arg components and injector parameters into context.
   *
   * flow:
   *
   * - validate base package for beans scan
   * - find all no arg components annotated with @{@link Singleton}
   *   as well as all no-arg parameters of constructor annotated with @{@link Inject}
   * - create new instance and put it into context
   *
   * @return DaggerokContext
   */
  private DaggerokContext createNoArgComponents() {

    final List<Constructor> constructors = FinderBuilder
        .builder()
        .basePackages(basePackages)
        .componentAnnotation(componentAnnotation)
        .injectAnnotation(injectAnnotation)
        .failOnUnknownReflectionsErrors(failOnUnknownReflectionsErrors)
        .build()
        .findAllComponentsConstructorsByParameterCountAndEqual(0, true);

    for (final Constructor constructor : constructors) {

      final Class<?> type = constructor.getDeclaringClass();

      if (log.isDebugEnabled()) log.debug("injecting {}...", type);
      injectAndRegister(type, constructor);
    }

    return this;
  }

  /**
   * Try to find and initialize all beans and injectors with retry = O(n^2).
   *
   * flow:
   *
   * - find all constructors annotated with @Inject with args > 0
   *  - create tree map with key: nr of constructor arguments and value: non resolved beans classes set
   *  - for each entry in map get key (nr of constructor args) and unresolved set
   *    - create injectorsLeft counter equal to unresolved set size and loopedCounter equal to injectorsLeft
   *    - for each unresolved set item try:
   *      - find bean in context
   *        - if bean vas found:
   *          - remove class from unresolved set
   *          - decrease injectorsLeft counter
   *        - otherwise try get all parameters for newInstance call
   *          - if all parameters exists (non null):
   *          - create instance with constructor injection
   *          - put it in context
   *          - remove class from unresolved set
   *          - decrease injectorsLeft counter
   *    - if injectorsLeft zero break to next entry
   *    - else if injectorsLeft == loopedCounter:
   *      - throw error and stop context bootstrap
   *      - or skip to next entry depends on fail-on condition configurations
   *
   * @return context initialization.
   */
  private DaggerokContext injectConstructorsInstances() {

    final List<Constructor> injects = findParametrizedInjectConstructors();
    final TreeMap<Integer, HashSet<Constructor>> toBeInitialized = getInjectorsMap(injects);
    final AtomicInteger beansLeft = new AtomicInteger(countTotalItemsValues(toBeInitialized));
    final AtomicInteger retry = new AtomicInteger(beansLeft.get());

    while (beansLeft.get() > 0 && retry.get() > 0) {

      for (final Map.Entry<Integer, HashSet<Constructor>> parametersCountToInjectors : toBeInitialized.entrySet()) {

        final AtomicInteger beforeCounter = new AtomicInteger(beansLeft.get());
        final HashSet<Constructor> constructors = parametersCountToInjectors.getValue();

        for (final Constructor constructor : constructors) {

          final Class<?> type = constructor.getDeclaringClass();

          if (null != getBean(type)) // bean already exists in context
            decrementIfValid(parametersCountToInjectors, constructors, constructor, beansLeft);

          else {

            final Class[] parameterTypes = constructor.getParameterTypes();
            final ArrayList<Object> params = parseParams(parameterTypes);

            if (params.size() != parameterTypes.length) continue;
            if (null != injectAndRegister(type, constructor, params.toArray())) // bean was created with injections
              decrementIfValid(parametersCountToInjectors, constructors, constructor, beansLeft);
          }
        }

        if (beansLeft.get() < 1) continue; // there are no unresolved beans left for initialization, bye-bye...
        if (beansLeft.get() == beforeCounter.get()) retry.decrementAndGet(); // nothing changes sins last iteration, retry--
        beforeCounter.set(beansLeft.get());
      }
    }

    return this;
  }

  /**
   * @return list of classes injectors with more than zero arguments.
   */
  private List<Constructor> findParametrizedInjectConstructors() {
    final List<Constructor> injects = FinderBuilder
        .builder()
        .basePackages(basePackages)
        .componentAnnotation(componentAnnotation)
        .injectAnnotation(injectAnnotation)
        .failOnUnknownReflectionsErrors(failOnUnknownReflectionsErrors)
        .build()
        .findAllInjects();
    final Set<Constructor> parametrizedConstructors = new HashSet<Constructor>();

    for (final Constructor constructor : injects) {
      if (constructor.getParameterTypes().length > 0) parametrizedConstructors.add(constructor);
    }

    return new ArrayList<Constructor>(parametrizedConstructors);
  }

  /**
   * Beans dependency tree resolution.
   *
   * @param injects inject to be parsed for getting constructors.
   * @return sorted map by argument count to constructors set.
   */
  private TreeMap<Integer, HashSet<Constructor>> getInjectorsMap(final List<Constructor> injects) {
    requireNonNull(injects, "injects");
    final TreeMap<Integer, HashSet<Constructor>> unresolved = new TreeMap<Integer, HashSet<Constructor>>();
    for (final Constructor constructor : injects) {
      final int count = constructor.getParameterTypes().length;
      final HashSet<Constructor> container = unresolved.get(count);
      final HashSet<Constructor> constructors = null == container ? new HashSet<Constructor>() : container;
      constructors.add(constructor);
      unresolved.put(count, constructors);
    }
    return unresolved;
  }

  private int countTotalItemsValues(final TreeMap<Integer, HashSet<Constructor>> map) {
    int total = 0;
    for (final Entry<Integer, HashSet<Constructor>> item : map.entrySet()) {
      total += item.getValue().size();
    }
    return total;
  }

  private void decrementIfValid(final Map.Entry<Integer, HashSet<Constructor>> entry,
                                final HashSet<Constructor> constructors,
                                final Constructor constructor,
                                final AtomicInteger counter) {

    final HashSet<Constructor> updated = new HashSet<Constructor>(constructors);
    updated.remove(constructor);
    entry.setValue(updated);
    counter.decrementAndGet();
  }

  /**
   * @param parameterTypes bean types.
   * @return beans from application context according to it's type.
   */
  private ArrayList<Object> parseParams(final Class[] parameterTypes) {
    final ArrayList<Object> params = new ArrayList<Object>();
    for (final Class<?> type : parameterTypes) {
      final Object bean = getBean(type);
      if (null != bean) params.add(bean);
      else for (final Constructor constructor : type.getConstructors()) {
        if (0 != constructor.getParameterTypes().length) continue;
        final Object instance = injectAndRegister(type, constructor);
        if (null == instance) continue;
        params.add(getBean(type));
        break;
      }
    }
    return params;
  }

  /**
   * Internal: create bean instance.
   * Will throw {@link NullPointerException} if bean is null and failOnInjectNullRef enabled.
   *
   * @param type bean type.
   * @param constructor constructor to be used for bean creation.
   * @param params parameters to be pass in constructor.
   * @param <T> could be any class.
   * @return registered bean or throw exception according to failOnInjectNullRef and failOnUnknownReflectionsErrors
   *         configurations.
   */
  private <T> T injectAndRegister(final Class<T> type, final Constructor constructor, final Object... params) {

    final Object instance = newInstance(constructor, params);
    final Object bean = type.cast(instance);

    if (null == bean) {

      if (failOnInjectNullRef) {
        final BeanNotFoundException exception = new BeanNotFoundException(type);
        log.error(exception.getLocalizedMessage(), exception);
        throw exception;
      }

      if (log.isDebugEnabled()) log.debug("Injecting bean {} and was resulted in null.", type.getName());
    }

    register(type.getName(), bean);
    return getBean(type);
  }

  /**
   * Creates new instance using constructor and reflection.
   *
   * @param constructor constructor to be used for bean instantiation.
   * @param parameters constructor parameters. If not present or null - NoArgConstructor will be used.
   * @return new bean instance.
   */
  @SuppressWarnings("unchecked")
  private Object newInstance(final Constructor constructor, final Object... parameters) {
    try {

      return constructor.newInstance(parameters);

    } catch (final Throwable e) {

      final Class type = constructor.getDeclaringClass();

      if (log.isDebugEnabled()) log.debug("Creation bean {} failed: {}", type.getName(), e.getLocalizedMessage());
      if (!failOnBeanCreationError) return null;

      final CreateNewInstanceException error = new CreateNewInstanceException(type, e.getLocalizedMessage());
      log.error("Bean instance '{}' creation with parameters '{}' failed.", type.getName(), parameters, error);
      throw error;
    }
  }
}
