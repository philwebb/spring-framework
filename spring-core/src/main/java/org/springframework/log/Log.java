/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.log;

/**
 * Extension to the java-commons-logging {@link org.apache.commons.logging.Log log}
 * interface that support {@code format} based logging and support for {@link LogCategory
 * log categories}.
 * <p>
 * This interface provides additional {@link String#format(String, Object...) format}
 * based logging methods that can be used as an alternative to usual
 * {@code isLevelEnabled()} methods. For example, the following two calls produce
 * identical results with similar performance characteristics when the {@code debug} level
 * is not enabled:
 *
 * <pre>
 * // Classic commons logging style
 * if (log.isDebugEnabled()) {
 * 	log.debug(&quot;Log message &quot; + name);
 * }
 *
 * // Alternative format style
 * log.debugf(&quot;Log message %s&quot;, name);
 * </pre>
 *
 * All {@code format} log calls can log an error by providing an additional
 * {@link Throwable} as the last argument.
 *
 * <p>If required, logs can write to multiple output {@link LogCategory categories}. This
 * allows users to tune log output in a more flexible way. For example, a
 * {@code org.springframework.log.SqlCategory} might be used to log all {@code SQL}
 * output. Existing classes would log appropriate output as follows:
 *
 * <pre>
 * static Log logger = LogFactory.getLog(SqlUtility.class);
 *
 * public void exampleMethod() {
 * 	// ...
 * 	logger.withCategory(SqlCategory.class).tracef(&quot;Running SQL %s&quot;, sql);
 * }
 * </pre>
 *
 * If a all output from a given log should be directed to category consider using the
 * {@link LogFactory#getLog(Class, Class, Class...)} method.
 *
 * <p>Category logging levels can be further customized by setting an logger attribute,
 * either {@link org.apache.commons.logging.LogFactory#setAttribute(String, Object)
 * programmatically} or via a {@code commons-logging.properties} file. The format of the
 * configuration is as follows:
 *
 * <pre>
 * &lt;attribute&gt; :== &lt;category-class&gt;=&lt;mappings&gt;
 * &lt;mappings&gt; :== &lt;mapping&gt; [, &lt;mapping&gt;]...
 * &lt;mapping&gt; :== &lt;logger-class&gt;.&lt;level&gt;->&lt;level&gt
 * &lt;level&gt; :== TRACE | DEBUG | INFO | WARN | ERROR | FATAL
 * </pre>
 *
 * For example, the following configuration will redirect all {@code DEBUG} output from
 * the {@code JdbcTemplate} when logged against the {@code SqlCategory}:
 *
 * <pre>
 * org.springframework.log.SqlCategory=org.springframework.jdbc.core.JdbcTemplate.DEBUG->INFO
 * </pre>
 *
 * @author Phillip Webb
 * @since 3.2
 * @see LogFactory
 * @see org.apache.commons.logging.Log
 * @see #tracef(String, Object, Object, Object...)
 * @see #debugf(String, Object, Object, Object...)
 * @see #infof(String, Object, Object, Object...)
 * @see #warnf(String, Object, Object, Object...)
 * @see #errorf(String, Object, Object, Object...)
 * @see #fatalf(String, Object, Object, Object...)
 * @see #withCategory(Class, Class...)
 *
 */
public interface Log extends org.apache.commons.logging.Log {

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code TRACE}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg the argument
	 */
	void tracef(String format, Object arg);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code TRACE}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 */
	void tracef(String format, Object arg1, Object arg2);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code TRACE}
	 * level. This variant incurs the hidden (but relatively small) cost of creating an
	 * Object[], even if this logger is disabled at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @param arguments additional arguments
	 */
	void tracef(String format, Object arg1, Object arg2, Object... arguments);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code DEBUG}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg the argument
	 */
	void debugf(String format, Object arg);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code DEBUG}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 */
	void debugf(String format, Object arg1, Object arg2);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code DEBUG}
	 * level. This variant incurs the hidden (but relatively small) cost of creating an
	 * Object[], even if this logger is disabled at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @param arguments additional arguments
	 */
	void debugf(String format, Object arg1, Object arg2, Object... arguments);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code DEBUG}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg the argument
	 */
	void infof(String format, Object arg);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code INFO}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 */
	void infof(String format, Object arg1, Object arg2);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code INFO}
	 * level. This variant incurs the hidden (but relatively small) cost of creating an
	 * Object[], even if this logger is disabled at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @param arguments additional arguments
	 */
	void infof(String format, Object arg1, Object arg2, Object... arguments);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code WARN}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg the argument
	 */
	void warnf(String format, Object arg);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code WARN}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 */
	void warnf(String format, Object arg1, Object arg2);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code WARN}
	 * level. This variant incurs the hidden (but relatively small) cost of creating an
	 * Object[], even if this logger is disabled at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @param arguments additional arguments
	 */
	void warnf(String format, Object arg1, Object arg2, Object... arguments);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code ERROR}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg the argument
	 */
	void errorf(String format, Object arg);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code ERROR}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 */
	void errorf(String format, Object arg1, Object arg2);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code ERROR}
	 * level. This variant incurs the hidden (but relatively small) cost of creating an
	 * Object[], even if this logger is disabled at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @param arguments additional arguments
	 */
	void errorf(String format, Object arg1, Object arg2, Object... arguments);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code FATAL}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg the argument
	 */
	void fatalf(String format, Object arg);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code FATAL}
	 * level. This form avoids superfluous object creation when the log level is disabled
	 * at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 */
	void fatalf(String format, Object arg1, Object arg2);

	/**
	 * Log a {@link String#format(String, Object...) formatted} message to {@code FATAL}
	 * level. This variant incurs the hidden (but relatively small) cost of creating an
	 * Object[], even if this logger is disabled at this level.
	 *
	 * @param format the format string
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @param arguments additional arguments
	 */
	void fatalf(String format, Object arg1, Object arg2, Object... arguments);

	/**
	 * Return a {@link Log} instance that will also log to the specified
	 * {@link LogCategory category} classes.  See {@link Log class level} documentation
	 * for details.
	 * @param categoryClass the category class to log to
	 * @param categoryClasses additional category classes to log to
	 * @return the log instance
	 */
	Log withCategory(Class<?> categoryClass, Class<?>... categoryClasses);
}
