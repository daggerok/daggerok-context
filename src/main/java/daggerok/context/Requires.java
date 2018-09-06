package daggerok.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

class Requires {

  private static final Logger log = LoggerFactory.getLogger(Requires.class);

  /* internal validation API */

  static void requireNotEmpty(final List<String> basePackages, final String messages) {
    requireNotEmpty(basePackages, messages, log);
  }

  /**
   * Throws {@link NullPointerException} if list is null.
   * Throws {@link IllegalStateException} if list doesn't contains non-null / non-empty package names.
   *
   * @param basePackages list of base package names.
   * @param messages error message.
   * @param log logger.
   */
  static void requireNotEmpty(final List<String> basePackages, final String messages, final Logger log) {

    requireNonNull(basePackages, "base packages");

    final ArrayList<String> result = new ArrayList<String>();

    for (final String packageName : basePackages) {
      if (null == packageName) continue;
      //// allow empty package
      //if ("".equals(packageName.trim())) continue;
      result.add(packageName);
    }

    if (result.size() > 0) return;

    final IllegalStateException exception = new IllegalStateException(messages);
    log.error(exception.getLocalizedMessage(), exception);
    throw exception;
  }

  /**
   * Throws {@link NullPointerException} if given argument is null.
   * @param o could be anything.
   * @param variableName variable name to be used in error message.
   * @param log logger.
   */
  static void requireNonNull(final Object o, final String variableName, final Logger log) {
    if (null != o) return;
    final NullPointerException exception = new NullPointerException(format("%s may not be null.", variableName));
    log.error(exception.getLocalizedMessage(), exception);
    throw exception;
  }

  static void requireNonNull(final Object o, final String variableName) {
    requireNonNull(o, variableName, log);
  }

  private Requires() {}
}
