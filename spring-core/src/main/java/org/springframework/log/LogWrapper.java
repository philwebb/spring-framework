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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Log} implementation that wraps java-commons-logging
 * {@link org.apache.commons.logging.Log logs}.
 *
 * @author Phillip Webb
 * @since 3.2
 */
class LogWrapper implements Log {


	private static final Object[] NO_ARGS = {};

	private static final Class<?>[] NO_CLASSES = {};

	private static final Throwable NO_THROWABLE = new Throwable();


	private final String name;

	private final org.apache.commons.logging.Log log;

	private final LogWrapper parent;

	private final LogWrapper root;


	/**
	 * Internal constructor.
	 * @param name the name of the logger
	 * @param log the underlying {@link org.apache.commons.logging.Log}
	 * @param parent a parent log or {@code null}
	 */
	private LogWrapper(String name, org.apache.commons.logging.Log log, LogWrapper parent) {
		this.name = name;
		this.log = log;
		this.parent = parent;
		LogWrapper root = parent;
		while(root != null && root.parent != null) {
			root = root.parent;
		}
		this.root = root;
	}


	public boolean isTraceEnabled() {
		return isEnabled(Level.TRACE);
	}

	public void trace(Object message) {
		log(Level.TRACE, message, NO_THROWABLE);
	}

	public void trace(Object message, Throwable t) {
		log(Level.TRACE, message, t);
	}

	public void tracef(String format, Object arg) {
		log(Level.TRACE, format, arg, null, NO_ARGS);
	}

	public void tracef(String format, Object arg1, Object arg2) {
		log(Level.TRACE, format, arg1, arg2, NO_ARGS);
	}

	public void tracef(String format, Object arg1, Object arg2, Object... arguments) {
		log(Level.TRACE, format, arg1, arg2, arguments);
	}

	public boolean isDebugEnabled() {
		return isEnabled(Level.DEBUG);
	}

	public void debug(Object message) {
		log(Level.DEBUG, message, NO_THROWABLE);
	}

	public void debug(Object message, Throwable t) {
		log(Level.DEBUG, message, t);
	}

	public void debugf(String format, Object arg) {
		log(Level.DEBUG, format, arg, null, NO_ARGS);
	}

	public void debugf(String format, Object arg1, Object arg2) {
		log(Level.DEBUG, format, arg1, arg2, NO_ARGS);
	}

	public void debugf(String format, Object arg1, Object arg2, Object... arguments) {
		log(Level.DEBUG, format, arg1, arg2, arguments);
	}

	public boolean isInfoEnabled() {
		return isEnabled(Level.INFO);
	}

	public void info(Object message) {
		log(Level.INFO, message, NO_THROWABLE);
	}

	public void info(Object message, Throwable t) {
		log(Level.INFO, message, t);
	}

	public void infof(String format, Object arg) {
		log(Level.INFO, format, arg, null, NO_ARGS);
	}

	public void infof(String format, Object arg1, Object arg2) {
		log(Level.INFO, format, arg1, arg2, NO_ARGS);
	}

	public void infof(String format, Object arg1, Object arg2, Object... arguments) {
		log(Level.INFO, format, arg1, arg2, arguments);
	}

	public boolean isWarnEnabled() {
		return isEnabled(Level.WARN);
	}

	public void warn(Object message) {
		log(Level.WARN, message, NO_THROWABLE);
	}

	public void warn(Object message, Throwable t) {
		log(Level.WARN, message, t);
	}

	public void warnf(String format, Object arg) {
		log(Level.WARN, format, arg, null, NO_ARGS);
	}

	public void warnf(String format, Object arg1, Object arg2) {
		log(Level.WARN, format, arg1, arg2, NO_ARGS);
	}

	public void warnf(String format, Object arg1, Object arg2, Object... arguments) {
		log(Level.WARN, format, arg1, arg2, arguments);
	}

	public boolean isErrorEnabled() {
		return isEnabled(Level.ERROR);
	}

	public void error(Object message) {
		log(Level.ERROR, message, NO_THROWABLE);
	}

	public void error(Object message, Throwable t) {
		log(Level.ERROR, message, t);
	}

	public void errorf(String format, Object arg) {
		log(Level.ERROR, format, arg, null, NO_ARGS);
	}

	public void errorf(String format, Object arg1, Object arg2) {
		log(Level.ERROR, format, arg1, arg2, NO_ARGS);
	}

	public void errorf(String format, Object arg1, Object arg2, Object... arguments) {
		log(Level.ERROR, format, arg1, arg2, arguments);
	}

	public boolean isFatalEnabled() {
		return isEnabled(Level.FATAL);
	}

	public void fatal(Object message) {
		log(Level.FATAL, message, NO_THROWABLE);
	}

	public void fatal(Object message, Throwable t) {
		log(Level.FATAL, message, t);
	}

	public void fatalf(String format, Object arg) {
		log(Level.FATAL, format, arg, null, NO_ARGS);
	}

	public void fatalf(String format, Object arg1, Object arg2) {
		log(Level.FATAL, format, arg1, arg2, NO_ARGS);
	}

	public void fatalf(String format, Object arg1, Object arg2, Object... arguments) {
		log(Level.FATAL, format, arg1, arg2, arguments);
	}

	public Log withCategory(Class<?> categoryClass, Class<?>... categoryClasses) {
		LogWrapper log = get(categoryClass, this);
		for (Class<?> additionalCategoryClass : (categoryClasses != null ? categoryClasses : NO_CLASSES)) {
			log = get(additionalCategoryClass, log);
		}
		return log;
	}

	private void log(Level level, String format, Object arg1, Object arg2,
			Object... arguments) {
		if (isEnabled(level)) {
			FormattedMessage message = new FormattedMessage(format, arg1, arg2, arguments);
			log(level, message.toString(), message.getThrowable());
		}
	}

	private void log(final Level level, Object message, Throwable t) {
		Level translatedLevel = (this.root == null ? level : LevelTranslator.get(this.name).translate(this.root.name, level));
		translatedLevel.log(this.log, message, t);
		if(this.parent != null) {
			this.parent.log(level, message, t);
		}
	}

	private boolean isEnabled(Level level) {
		return level.isEnabled(this.log) || (this.parent != null && this.parent.isEnabled(level));
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", this.name).append("log", this.log).toString();
	}


	public static LogWrapper get(Class<?> clazz) {
		if(clazz == null) {
			throw new IllegalArgumentException("Class must not be null");
		}
		return get(clazz.getName(), null);
	}

	private static LogWrapper get(Class<?> clazz, LogWrapper parent) {
		if(clazz == null) {
			throw new IllegalArgumentException("Class must not be null");
		}
		return get(clazz.getName(), parent);
	}

	public static LogWrapper get(String name) {
		return get(name, null);
	}

	/**
	 * Factory method used to create a new {@link LogWrapper} instance.
	 * @param name the name of the logger
	 * @param parent the parent or {@code null}
	 * @return a {@link LogWrapper} instance
	 */
	private static LogWrapper get(String name, LogWrapper parent) {
		// FIXME We should cache here based on the name and the parent
		if(name == null) {
			throw new IllegalArgumentException("Name must not be null");
		}
		return new LogWrapper(name, LogFactory.getLog(name), parent);
	}


	/**
	 * Logging levels and mappings back to underling
	 * {@link org.apache.commons.logging.Log} methods.
	 */
	private enum Level {

		TRACE {
			@Override
			public boolean isEnabled(org.apache.commons.logging.Log log) {
				return log.isTraceEnabled();
			}
			@Override
			public void log(org.apache.commons.logging.Log log, Object message, Throwable t) {
				if(t == NO_THROWABLE) {
					log.trace(message);
				} else {
					log.trace(message, t);
				}
			};
		},

		DEBUG {
			@Override
			public boolean isEnabled(org.apache.commons.logging.Log log) {
				return log.isDebugEnabled();
			}
			@Override
			public void log(org.apache.commons.logging.Log log, Object message, Throwable t) {
				if(t == NO_THROWABLE) {
					log.debug(message);
				} else {
					log.debug(message, t);
				}
			};
		},

		INFO {
			@Override
			public boolean isEnabled(org.apache.commons.logging.Log log) {
				return log.isInfoEnabled();
			}
			@Override
			public void log(org.apache.commons.logging.Log log, Object message, Throwable t) {
				if(t == NO_THROWABLE) {
					log.info(message);
				} else {
					log.info(message, t);
				}
			};
		},

		WARN {
			@Override
			public boolean isEnabled(org.apache.commons.logging.Log log) {
				return log.isWarnEnabled();
			}
			@Override
			public void log(org.apache.commons.logging.Log log, Object message, Throwable t) {
				if(t == NO_THROWABLE) {
					log.warn(message);
				} else {
					log.warn(message, t);
				}
			};
		},

		ERROR {
			@Override
			public boolean isEnabled(org.apache.commons.logging.Log log) {
				return log.isErrorEnabled();
			}
			@Override
			public void log(org.apache.commons.logging.Log log, Object message, Throwable t) {
				if(t == NO_THROWABLE) {
					log.error(message);
				} else {
					log.error(message, t);
				}
			};
		},

		FATAL {
			@Override
			public boolean isEnabled(org.apache.commons.logging.Log log) {
				return log.isFatalEnabled();
			}
			@Override
			public void log(org.apache.commons.logging.Log log, Object message, Throwable t) {
				if(t == NO_THROWABLE) {
					log.fatal(message);
				} else {
					log.fatal(message, t);
				}
			};
		};

		public abstract boolean isEnabled(org.apache.commons.logging.Log log);

		public abstract void log(org.apache.commons.logging.Log log, Object message, Throwable t);
	}


	/**
	 * Internal class used to construct formatted messages.
	 */
	private static class FormattedMessage {

		private final String string;

		private final Throwable throwable;


		public FormattedMessage(String format, Object arg1, Object arg2,
				Object... arguments) {
			Object[] allArguments = concat(arg1, arg2, arguments);
			this.string = String.format(format, allArguments);
			this.throwable = findThrowable(allArguments);
		}


		private Object[] concat(Object arg1, Object arg2, Object... arguments) {
			Object[] concat = new Object[arguments.length+2];
			concat[0] = arg1;
			concat[1] = arg2;
			System.arraycopy(arguments, 0, concat, 2, arguments.length);
			return concat;
		}

		private Throwable findThrowable(Object[] arguments) {
			int lastArgument = arguments.length-1;
			while(arguments[lastArgument] == null && lastArgument >= 0) {
				lastArgument--;
			}
			if(lastArgument >= 0 && arguments[lastArgument] instanceof Throwable) {
				return (Throwable) arguments[lastArgument];
			}
			return NO_THROWABLE;
		}

		@Override
		public String toString() {
			return this.string;
		}

		public Throwable getThrowable() {
			return this.throwable;
		}
	}


	/**
	 * Internal class used to translate {@link Level}s based on configuration.
	 */
	private static class LevelTranslator {


		private static final LevelTranslator NONE = new LevelTranslator(null, null);

		private static final Pattern PATTERN = Pattern.compile("^([\\w\\.]+)\\.([A-Z]+)->([A-Z]+)$");


		private static Map<String, LevelTranslator> cache = new ConcurrentHashMap<String, LevelTranslator>();


		private final Map<NameAndLevel, Level> mappings = new HashMap<NameAndLevel, Level>();


		public LevelTranslator(String name, String attribute) {
			if(attribute != null) {
				for (String mapping : StringUtils.commaDelimitedListToStringArray(attribute)) {
					try {
						Matcher matcher = PATTERN.matcher(mapping.trim());
						Assert.state(matcher.matches(), "Unrecognized pattern");
						NameAndLevel nameAndLevel = new NameAndLevel(
								matcher.group(1), Level.valueOf(matcher.group(2)));
						this.mappings.put(nameAndLevel, Level.valueOf(matcher.group(3)));
					} catch (Exception e) {
						throw new IllegalStateException("Unable to parse '" + mapping
								+ "' for log config '" + name + "'", e);
					}
				}
			}
		}


		public Level translate(String name, Level level) {
			Level translatedLevel = this.mappings.get(new NameAndLevel(name, level));
			return (translatedLevel != null ? translatedLevel : level);
		}


		public static LevelTranslator get(String name) {
			LevelTranslator translator = cache.get(name);
			if(translator == null) {
				Object attribute = LogFactory.getFactory().getAttribute(name);
				translator = (attribute == null ? NONE : new LevelTranslator(name, attribute.toString()));
				cache.put(name, translator);
			}
			return translator;
		}
	}


	/**
	 * Allows a log name and {@link Level} to be used as a mapping key.
	 */
	private static final class NameAndLevel {

		private final String name;

		private final Level level;


		public NameAndLevel(String name, Level level) {
			this.name = name;
			this.level = level;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || obj.getClass() != NameAndLevel.class) {
				return false;
			}
			NameAndLevel other = (NameAndLevel) obj;
			return this.name.equals(other.name) && this.level.equals(other.level);

		}

		@Override
		public int hashCode() {
			return this.name.hashCode() * 31 + this.level.hashCode();
		}
	}
}
