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

import org.apache.commons.logging.LogConfigurationException;

/**
 * Factory for creating {@link Log} instances. Designed to be used internally as a drop-in
 * replacement for {@link org.apache.commons.logging.LogFactory}. The {@code Log}
 * instances returned from this factory provide {@link Log additional capabilities} whilst
 * remaining type compatible with {@link org.apache.commons.logging.Log}.
 *
 * <p><a href="http://commons.apache.org/logging/commons-logging-1.1.1/guide.html">Apache
 * Commons Logging</a> remains as the underling logging facade. This factory can be
 * configured in the same way as any standard
 * {@link org.apache.commons.logging.LogFactory}.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see Log
 * @see org.apache.commons.logging.LogFactory
 */
public abstract class LogFactory {

	/**
	 * Return a logger for the specific {@code class}.
	 * @param clazz Class from which a log name will be derived
	 * @return a {@code Log} instance
	 * @exception LogConfigurationException if a suitable {@code Log} instance cannot
	 *            be returned
	 * @see #getLog(Class, Class, Class...)
	 */
	public static Log getLog(Class<?> clazz) throws LogConfigurationException {
		return LogWrapper.get(clazz);
	}

	/**
	 * Return a logger for the specific {@code class} with support for additional
	 * {@link LogCategory categories}.
	 * @param clazz Class from which a log name will be derived
	 * @param categoryClass the {@link LogCategory category} class (or {@code null}).
	 * @param categoryClasses additional {@link LogCategory category} classes (if required).
	 * @return a {@link Log} instance
	 * @exception LogConfigurationException if a suitable {@code Log} instance cannot
	 *            be returned
	 * @see Log#withCategory(Class, Class...)
	 * @see #getLog(String, Class, Class...)
	 */
	public static Log getLog(Class<?> clazz, Class<?> categoryClass,
			Class<?>... categoryClasses) throws LogConfigurationException {
		return LogWrapper.get(clazz).withCategory(categoryClass, categoryClasses);
	}

	/**
	 * Return a logger for the specific {@code name}.
	 * @param name the log name
	 * @return a {@code Log} instance
	 * @exception LogConfigurationException if a suitable {@code Log} instance cannot
	 *            be returned
	 * @see #getLog(String, Class, Class...)
	 */
	public static Log getLog(String name) throws LogConfigurationException {
		return LogWrapper.get(name);
	}

	/**
	 * Return a logger for the specific {@code name} with support for additional
	 * {@link LogCategory categories}.
	 * @param name the log name
	 * @param categoryClass the {@link LogCategory category} class (or {@code null}).
	 * @param categoryClasses additional {@link LogCategory category} classes (if required).
	 * @return the {@link Log} instance
	 * @exception LogConfigurationException if a suitable {@code Log} instance cannot
	 *            be returned
	 * @see Log#withCategory(Class, Class...)
	 * @see #getLog(Class, Class, Class...)
	 */
	public static Log getLog(String name, Class<?> categoryClass,
			Class<?>... categoryClasses) throws LogConfigurationException {
		return LogWrapper.get(name).withCategory(categoryClass, categoryClasses);
	}
}
