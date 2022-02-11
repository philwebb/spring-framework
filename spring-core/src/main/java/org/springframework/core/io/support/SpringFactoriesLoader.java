/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io.support;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * General purpose factory loading mechanism for internal use within the framework.
 *
 * <p>{@code SpringFactoriesLoader} {@linkplain #loadFactories loads} and instantiates
 * factories of a given type from {@value #FACTORIES_RESOURCE_LOCATION} files which
 * may be present in multiple JAR files in the classpath. The {@code spring.factories}
 * file must be in {@link Properties} format, where the key is the fully qualified
 * name of the interface or abstract class, and the value is a comma-separated list of
 * implementation class names. For example:
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 *
 * where {@code example.MyService} is the name of the interface, and {@code MyServiceImpl1}
 * and {@code MyServiceImpl2} are two implementations.
 * <p>
 * Implementation classes <b>must</b> have a single unambiguous constructor that will be use
 * to create the instance. Private constructors are ignored.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 3.2
 */
public final class SpringFactoriesLoader {

	/**
	 * The location to look for factories.
	 * <p>Can be present in multiple JAR files.
	 */
	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";


	private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

	private static final FactoryInstantiationFailureHandler THROWING_HANDLER = FactoryInstantiationFailureHandler.throwing();

	static final Map<ClassLoader, Map<String, List<String>>> cache = new ConcurrentReferenceHashMap<>();


	private SpringFactoriesLoader() {
	}


	/**
	 * Load and instantiate the factory implementations of the given type from
	 * {@value #FACTORIES_RESOURCE_LOCATION}, using the given class loader.
	 * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
	 * <p>As of Spring Framework 5.3, if duplicate implementation class names are
	 * discovered for a given factory type, only one instance of the duplicated
	 * implementation type will be instantiated.
	 * @param factoryType the interface or abstract class representing the factory
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 * @throws IllegalArgumentException if any factory implementation class cannot
	 * be loaded or if an error occurs while instantiating any factory
	 */
	public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
		return loadFactories(factoryType, null, classLoader, null);
	}

	/**
	 * Load and instantiate the factory implementations of the given type from
	 * {@value #FACTORIES_RESOURCE_LOCATION}, using the given parameter resolver and class loader.
	 * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
	 * <p>As of Spring Framework 5.3, if duplicate implementation class names are
	 * discovered for a given factory type, only one instance of the duplicated
	 * implementation type will be instantiated.
	 * @param factoryType the interface or abstract class representing the factory
	 * @param parameterResolver strategy used to resolve constructor parameters by their type
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 * @throws IllegalArgumentException if any factory implementation class cannot
	 * be loaded or if an error occurs while instantiating any factory
	 * @since 6.0
	 */
	public static <T> List<T> loadFactories(Class<T> factoryType, ParameterResolver parameterResolver, @Nullable ClassLoader classLoader) {
		return loadFactories(factoryType, parameterResolver, classLoader, null);
	}

	/**
	 * Load and instantiate the factory implementations of the given type from
	 * {@value #FACTORIES_RESOURCE_LOCATION}, using the given class loader with custom failure
	 * handling provided by the given failure handler.
	 * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
	 * <p>As of Spring Framework 5.3, if duplicate implementation class names are
	 * discovered for a given factory type, only one instance of the duplicated
	 * implementation type will be instantiated.
	 * <p>For any factory implementation class that cannot be loaded or error that occurs while
	 * instantiating it, the given failure handler is called.
	 * @param factoryType the interface or abstract class representing the factory
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 * @param failureHandler the FactoryInstantiationFailureHandler to use for handling of factory instantiation failures
	 * @since 6.0
	 */
	public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader, FactoryInstantiationFailureHandler failureHandler) {
		Assert.notNull(factoryType, "'factoryType' must not be null");
		Assert.notNull(failureHandler, "'failureHandler' must not be null");
		return loadFactories(factoryType, null, classLoader, failureHandler);
	}

	/**
	 * Load and instantiate the factory implementations of the given type from
	 * {@value #FACTORIES_RESOURCE_LOCATION}, using the given arguments and class loader with custom
	 * failure handling provided by the given failure handler.
	 * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
	 * <p>As of Spring Framework 5.3, if duplicate implementation class names are
	 * discovered for a given factory type, only one instance of the duplicated
	 * implementation type will be instantiated.
	 * <p>For any factory implementation class that cannot be loaded or error that occurs while
	 * instantiating it, the given failure handler is called.
	 * @param factoryType the interface or abstract class representing the factory
	 * @param parameterResolver strategy used to resolve constructor parameters by their type
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 * @param failureHandler the FactoryInstantiationFailureHandler to use for handling of factory
	 * instantiation failures
	 * @since 6.0
	 */
	public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ParameterResolver parameterResolver,
			@Nullable ClassLoader classLoader, @Nullable FactoryInstantiationFailureHandler failureHandler) {

		Assert.notNull(factoryType, "'factoryType' must not be null");
		ClassLoader classLoaderToUse = (classLoader != null) ? classLoader : SpringFactoriesLoader.class.getClassLoader();
		List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoaderToUse);
		logger.trace(LogMessage.format("Loaded [%s] names: %s", factoryType.getName(), factoryImplementationNames));
		List<T> result = new ArrayList<>(factoryImplementationNames.size());
		FactoryInstantiationFailureHandler failureHandlerToUse = (failureHandler != null) ? failureHandler : THROWING_HANDLER;
		for (String factoryImplementationName : factoryImplementationNames) {
			T factory = instantiateFactory(factoryImplementationName, factoryType,
					parameterResolver, classLoaderToUse, failureHandlerToUse);
			if (factory != null) {
				result.add(factory);
			}
		}
		AnnotationAwareOrderComparator.sort(result);
		return result;
	}

	/**
	 * Load the fully qualified class names of factory implementations of the
	 * given type from {@value #FACTORIES_RESOURCE_LOCATION}, using the given
	 * class loader.
	 * <p>As of Spring Framework 5.3, if a particular implementation class name
	 * is discovered more than once for the given factory type, duplicates will
	 * be ignored.
	 * @param factoryType the interface or abstract class representing the factory
	 * @param classLoader the ClassLoader to use for loading resources; can be
	 * {@code null} to use the default
	 * @throws IllegalArgumentException if an error occurs while loading factory names
	 * @see #loadFactories
	 */
	public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = (classLoader != null) ? classLoader : SpringFactoriesLoader.class.getClassLoader();
		String factoryTypeName = factoryType.getName();
		return getAllFactories(classLoaderToUse).getOrDefault(factoryTypeName, Collections.emptyList());
	}

	private static Map<String, List<String>> getAllFactories(ClassLoader classLoader) {
		Map<String, List<String>> result = cache.get(classLoader);
		if (result != null) {
			return result;
		}
		result = loadAllFactories(classLoader);
		cache.put(classLoader, result);
		return result;
	}

	private static Map<String, List<String>> loadAllFactories(ClassLoader classLoader) {
		Map<String, List<String>> result;
		result = new HashMap<>();
		try {
			Enumeration<URL> urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
			while (urls.hasMoreElements()) {
				UrlResource resource = new UrlResource(urls.nextElement());
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				for (Map.Entry<?, ?> entry : properties.entrySet()) {
					String factoryTypeName = ((String) entry.getKey()).trim();
					String[] factoryImplementationNames =
							StringUtils.commaDelimitedListToStringArray((String) entry.getValue());
					for (String factoryImplementationName : factoryImplementationNames) {
						result.computeIfAbsent(factoryTypeName, key -> new ArrayList<>())
								.add(factoryImplementationName.trim());
					}
				}
			}
			result.replaceAll(SpringFactoriesLoader::toDistinctUnmodifiableList);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load factories from location [" +
					FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
		return Collections.unmodifiableMap(result);
	}

	private static List<String> toDistinctUnmodifiableList(String factoryType, List<String> implementations) {
		return implementations.stream().distinct().toList();
	}

	@Nullable
	private static <T> T instantiateFactory(String factoryImplementationName,
			Class<T> factoryType, @Nullable ParameterResolver parameterResolver,
			ClassLoader classLoader, FactoryInstantiationFailureHandler failureHandler) {
		try {
			Class<?> factoryImplementationClass = ClassUtils.forName(factoryImplementationName, classLoader);
			Assert.isTrue(factoryType.isAssignableFrom(factoryImplementationClass),
					() -> "Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
			FactoryInstantiator<T> factoryInstantiator = FactoryInstantiator.forClass(factoryImplementationClass);
			return factoryInstantiator.instantiate(parameterResolver);
		}
		catch (Throwable ex) {
			failureHandler.handleFailure(factoryType, factoryImplementationName, ex);
			return null;
		}
	}


	/**
	 * Instantiator used to create the factory instance.
	 * @param <T> the instance implementation type
	 */
	static final class FactoryInstantiator<T> {

		private final Constructor<T> constructor;

		private FactoryInstantiator(Constructor<T> constructor) {
			ReflectionUtils.makeAccessible(constructor);
			this.constructor = constructor;
		}

		T instantiate(ParameterResolver parameterResolver) throws Exception {
			Object[] args = resolveArgs(parameterResolver);
			return this.constructor.newInstance(args);
		}

		private Object[] resolveArgs(@Nullable ParameterResolver parameterResolver) {
			Class<?>[] types = this.constructor.getParameterTypes();
			return (parameterResolver != null) ?
					Arrays.stream(types).map(parameterResolver::resolve).toArray() :
					new Object[types.length];
		}

		@SuppressWarnings("unchecked")
		static <T> FactoryInstantiator<T> forClass(Class<?> factoryImplementationClass) {
			Constructor<?> constructor = null;
			for (Constructor<?> candidate : factoryImplementationClass.getDeclaredConstructors()) {
				if (!candidate.isSynthetic() && !candidate.isVarArgs()
						&& !Modifier.isPrivate(candidate.getModifiers())) {
					Assert.state(constructor == null, "Class [" + factoryImplementationClass.getName() + "] has multiple non-private constructors");
					constructor = candidate;
				}
			}
			Assert.state(constructor != null,"Class [" + factoryImplementationClass.getName() + "] has no suitable constructor");
			return new FactoryInstantiator<>((Constructor<T>) constructor);
		}

	}


	/**
	 * Strategy for handling a failure that occurs when instantiating a factory.
	 *
	 * @since 6.0
	 */
	@FunctionalInterface
	public interface FactoryInstantiationFailureHandler {

		/**
		 * Handle the {@code failure} that occurred when instantiating the {@code factoryImplementationName}
		 * that was expected to be of the given {@code factoryType}.
		 * @param factoryType the type of the factory
		 * @param factoryImplementationName the name of the factory implementation
		 * @param failure the failure that occurred
		 * @see #throwing()
		 * @see #logging
		 */
		void handleFailure(Class<?> factoryType, String factoryImplementationName, Throwable failure);

		/**
		 * Return a new {@link FactoryInstantiationFailureHandler} that handles
		 * errors by throwing an {@link IllegalArgumentException}.
		 * @return a new {@link FactoryInstantiationFailureHandler} instance
		 */
		static FactoryInstantiationFailureHandler throwing() {
			return throwing(IllegalArgumentException::new);
		}

		/**
		 * Return a new {@link FactoryInstantiationFailureHandler} that handles
		 * errors by throwing an exception.
		 * @param exceptionFactory factory used to create the exception
		 * @return a new {@link FactoryInstantiationFailureHandler} instance
		 */
		static FactoryInstantiationFailureHandler throwing(BiFunction<String, Throwable, ? extends RuntimeException> exceptionFactory) {
			return handleMessage((message, failure) -> {
				throw exceptionFactory.apply(message.get(), failure);
			});
		}

		/**
		 * Return a new {@link FactoryInstantiationFailureHandler} that handles
		 * errors by logging trace messages.
		 * @param logger the logger used to log message
		 * @return a new {@link FactoryInstantiationFailureHandler} instance
		 */
		static FactoryInstantiationFailureHandler logging(Log logger) {
			return handleMessage((message, failure) -> logger.trace(LogMessage.of(message), failure));
		}

		/**
		 * Return a new {@link FactoryInstantiationFailureHandler} that handles
		 * errors with using a standard formatted message.
		 * @param messageHandler the message handler used to handle the problem
		 * @return a new {@link FactoryInstantiationFailureHandler} instance
		 */
		static FactoryInstantiationFailureHandler handleMessage(BiConsumer<Supplier<String>, Throwable> messageHandler) {
			return (factoryType, factoryImplementationName, failure) -> {
				Supplier<String> message = () -> "Unable to instantiate factory class [" + factoryImplementationName +
						"] for factory type [" + factoryType.getName() + "]";
				messageHandler.accept(message, failure);
			};
		}

	}


	/**
	 * Strategy for resolving constructor parameters based on their type.
	 *
	 * @since 6.0
	 */
	@FunctionalInterface
	public interface ParameterResolver {

		/**
		 * Resolve the given parameter type if possible.
		 * @param <T> the parameter type
		 * @param parameterType the parameter type
		 * @return the resolved parameter value or {@code null}
		 */
		@Nullable
		<T> T resolve(Class<T> parameterType);

		/**
		 * Create a new composed {@link ParameterResolver} by combining this resolver
		 * with the given type and value.
		 * @param <T> the parameter type
		 * @param parameterType the parameter type
		 * @param value the parameter value
		 * @return a new composite {@link ParameterResolver} instance
		 */
		default <T> ParameterResolver and(Class<T> parameterType, T value) {
			return and(ParameterResolver.of(parameterType, value));
		}

		/**
		 * Create a new composed {@link ParameterResolver} by combining this resolver
		 * with the given type and value.
		 * @param <T> the parameter type
		 * @param parameterType the parameter type
		 * @param valueSupplier the parameter value supplier
		 * @return a new composite {@link ParameterResolver} instance
		 */
		default <T> ParameterResolver andSupplied(Class<T> parameterType, Supplier<T> valueSupplier) {
			return and(ParameterResolver.ofSupplied(parameterType, valueSupplier));
		}

		/**
		 * Create a new composed {@link ParameterResolver} by combining this resolver
		 * with the given resolver.
		 * @param parameterResolver the parameter resolver to add
		 * @return a new composite {@link ParameterResolver} instance
		 */
		default ParameterResolver and(ParameterResolver parameterResolver) {
			return from(parameterType -> {
				Object resolved = resolve(parameterType);
				return (resolved != null) ? resolved : parameterResolver.resolve(parameterType);
			});
		}

		/**
		 * Factory method that can be used to create a {@link ParameterResolver}
		 * that resolves only the given type.
		 * @param <T> the
		 * @param parameterType the parameter type
		 * @param value the parameter value
		 * @return a new {@link ParameterResolver} instance
		 */
		static <T> ParameterResolver of(Class<T> parameterType, T value) {
			return ofSupplied(parameterType, (Supplier<T>) () -> value);
		}

		/**
		 * Factory method that can be used to create a {@link ParameterResolver}
		 * that resolves only the given type.
		 * @param <T> the
		 * @param parameterType the parameter type
		 * @param valueSupplier the parameter value supplier
		 * @return a new {@link ParameterResolver} instance
		 */
		static <T> ParameterResolver ofSupplied(Class<T> parameterType, Supplier<T> valueSupplier) {
			return from(candidateType -> candidateType.equals(parameterType) ? valueSupplier.get() : null);
		}

		/**
		 * Factory method that creates a new {@link ParameterResolver} from a
		 * lambda friendly function. The given function is provided with the
		 * value type and must provide an instance of that type or {@code null}.
		 * @param function the resolver function
		 * @return a new {@link ParameterResolver} instance backed by the function
		 */
		static ParameterResolver from(Function<Class<?>, Object> function) {
			return new ParameterResolver() {

				@SuppressWarnings("unchecked")
				@Override
				public <P> P resolve(Class<P> parameterType) {
					return (P) function.apply(parameterType);
				}

			};
		}

	}

}
