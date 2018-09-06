package daggerok.context;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;

import static daggerok.context.Requires.requireNonNull;
import static daggerok.context.Requires.requireNotEmpty;

class Finders {

  private Finders() {}

  static class FinderBuilder {
    private static final Logger log = LoggerFactory.getLogger(FinderBuilder.class);

    private final Finder finder;

    private FinderBuilder(Finder finder) {
      this.finder = finder;
    }

    static FinderBuilder builder() {
      return new FinderBuilder(new Finder());
    }

    FinderBuilder basePackages(final List<String> basePackages) {
      finder.basePackages = basePackages;
      return this;
    }

    FinderBuilder componentAnnotation(final Class<? extends Annotation> componentAnnotation) {
      finder.componentAnnotation = componentAnnotation;
      return this;
    }

    FinderBuilder injectAnnotation(final Class<? extends Annotation> injectAnnotation) {
      finder.injectAnnotation = injectAnnotation;
      return this;
    }

    FinderBuilder failOnUnknownReflectionsErrors(final boolean failOnUnknownReflectionsErrors) {
      finder.failOnUnknownReflectionsErrors = failOnUnknownReflectionsErrors;
      return this;
    }

    Finder build() {
      requireNonNull(finder.basePackages, "finder.basePackages", log);
      requireNonNull(finder.componentAnnotation, "finder.componentAnnotation", log);
      requireNonNull(finder.injectAnnotation, "finder.injectAnnotation", log);
      requireNonNull(finder.failOnUnknownReflectionsErrors, "finder.failOnUnknownReflectionsErrors", log);
      return finder;
    }
  }

  static class Finder {
    private static final Logger log = LoggerFactory.getLogger(Finder.class);

    private List<String> basePackages = null;
    private Class<? extends Annotation> componentAnnotation = null;
    private Class<? extends Annotation> injectAnnotation = null;
    private Boolean failOnUnknownReflectionsErrors = null;

    private Finder() {}

    /* reflections vendor API */

    /**
     * Search base package beans scan for injectors constructors.
     *
     * @return list of classes injectors containing constructors annotated with @{@link Inject}
     * or it's injectAnnotation replacement.
     */
    List<Constructor> findAllInjects() {
      requireNotEmpty(basePackages, "list of base packages may not be empty.", log);
      final MethodAnnotationsScanner scanner = new MethodAnnotationsScanner();
      final Set<Constructor> injects = new HashSet<Constructor>();
      for (final String basePackage : basePackages) {
        try {
          if (log.isDebugEnabled()) log.debug("processing package '{}' for {} injectors",
                                              basePackage, injectAnnotation.getName());
          final Reflections reflections = new Reflections(basePackage, scanner);
          injects.addAll(reflections.getConstructorsAnnotatedWith(injectAnnotation));
        }
        catch (final Throwable e) {
          if (log.isDebugEnabled()) log.debug("Reflections filed: {}", e.getLocalizedMessage());
          if (!failOnUnknownReflectionsErrors) continue; // skip any ReflectionsExceptions...
          log.error(e.getLocalizedMessage(), e);
          throw new Exceptions.WrappedReflectionsException(e);
        }
      }
      return new ArrayList<Constructor>(injects);
    }

    /**
     * Search for beans scan component classes.
     *
     * @return list of component classes annotated with @{@link Singleton} or it's componentAnnotation replacement
     */
    List<Class> findAllComponents() {
      requireNotEmpty(basePackages, "list of base packages may not be empty.", log);
      final Set<Class> components = new HashSet<Class>();
      for (final String basePackage : basePackages) {
        try {
          if (log.isDebugEnabled()) log.debug("processing package '{}' for {} components",
                                              basePackage, componentAnnotation.getName());
          final Reflections reflections = new Reflections(basePackage);
          // Searching all @Singleton classes
          components.addAll(reflections.getTypesAnnotatedWith(componentAnnotation));
        }
        catch (final Throwable e) {
          if (log.isDebugEnabled()) log.debug("Reflections filed: {}", e.getLocalizedMessage());
          if (!failOnUnknownReflectionsErrors) continue; // skip any ReflectionsExceptions...
          log.error(e.getLocalizedMessage(), e);
          throw new Exceptions.WrappedReflectionsException(e);
        }
      }
      return new ArrayList<Class>(components);
    }

    /**
     * Search for components according to it's constructor parameters amount.
     *
     * @param count amount of constructor parameters to be found.
     * @param isEqual indicates equality of nonEquality to previous count argument.
     * @return list of component classes annotated with @{@link Singleton} or it's componentAnnotation replacement
     *         as well as classes containing constructor annotated with @{@link Inject}.
     */
    List<Constructor> findAllComponentsConstructorsByParameterCountAndEqual(final int count, boolean isEqual) {
      final Set<Constructor> allConstructors = new HashSet<Constructor>(findAllInjects());
      final Set<Constructor> constructors = new HashSet<Constructor>();

      for (final Class component : findAllComponents()) {
        allConstructors.addAll(Arrays.asList(component.getConstructors()));
      }

      for (final Constructor constructor : allConstructors) {
        final int parametersCount = constructor.getParameterTypes().length;
        final boolean searchCriteriaIsMatched = isEqual
            ? count == parametersCount : count != parametersCount;
        if (searchCriteriaIsMatched) constructors.add(constructor);
      }

      return new ArrayList<Constructor>(constructors);
    }
  }
}
