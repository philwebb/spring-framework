/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.BeanRegistry.Spec;
import org.springframework.beans.factory.BeanRegistry.SupplierContext;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder;

/**
 * Abstract base class for {@link BeanRegistrar} implementations that register HTTP
 * service proxy beans.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
public abstract class HttpServiceProxyBeanRegistrar<S extends HttpServiceProxyBeanRegistrar<S>>
		implements BeanRegistrar {

	protected final List<Class<?>> types;

	protected final List<BasePackage> basePackages;

	protected final @Nullable String using;

	protected final List<BiConsumer<HttpServiceProxyFactory.Builder, SupplierContext>> httpServiceProxyFactoryCustomizers;

	protected final List<BiConsumer<BeanRegistry.Spec<?>, Class<?>>> beanRegistrationCustomizers;

	/**
	 * Create a new empty {@link HttpServiceProxyBeanRegistrar}.
	 */
	protected HttpServiceProxyBeanRegistrar() {
		this.types = Collections.emptyList();
		this.basePackages = Collections.emptyList();
		this.using = null;
		this.httpServiceProxyFactoryCustomizers = Collections.emptyList();
		this.beanRegistrationCustomizers = Collections.emptyList();
	}

	/**
	 * Create a new {@link HttpServiceProxyBeanRegistrar} instance.
	 * @param types the type to register
	 * @param basePackages the base packages to scan
	 * @param using the {@link HttpServiceProxyFactoryProvider} id to use
	 * @param httpServiceProxyFactoryCustomizers the {@link HttpServiceProxyFactory}
	 * customizers to apply
	 * @param beanRegistrationCustomizers the bean registration customizers to apply
	 */
	protected HttpServiceProxyBeanRegistrar(List<Class<?>> types, List<BasePackage> basePackages,
			@Nullable String using,
			List<BiConsumer<HttpServiceProxyFactory.Builder, SupplierContext>> httpServiceProxyFactoryCustomizers,
			List<BiConsumer<BeanRegistry.Spec<?>, Class<?>>> beanRegistrationCustomizers) {
		this.types = types;
		this.basePackages = basePackages;
		this.using = using;
		this.httpServiceProxyFactoryCustomizers = httpServiceProxyFactoryCustomizers;
		this.beanRegistrationCustomizers = beanRegistrationCustomizers;
	}

	/**
	 * Return a new instance of this registrar that additionally adds the HTTP service
	 * types.
	 * @param types the types to add
	 * @return a new registrar instance
	 */
	public S add(Class<?>... types) {
		return copy(merge(this.types, types), this.basePackages, this.using, this.httpServiceProxyFactoryCustomizers,
				this.beanRegistrationCustomizers);
	}

	/**
	 * Return a new instance of this registrar that additionally scans given package for
	 * HTTP service types.
	 * @param types the types to add
	 * @return a new registrar instance
	 */
	public S scan(String... basePackageNames) {
		return scan(Arrays.stream(basePackageNames).map(BasePackage::of).toArray(BasePackage[]::new));
	}

	/**
	 * Return a new instance of this registrar that additionally scans given package for
	 * HTTP service types.
	 * @param types the types to add
	 * @return a new registrar instance
	 */
	public S scan(Class<?>... basePackageClasses) {
		return scan(Arrays.stream(basePackageClasses).map(BasePackage::of).toArray(BasePackage[]::new));
	}

	/**
	 * Return a new instance of this registrar that additionally scans given package for
	 * HTTP service types.
	 * @param types the types to add
	 * @return a new registrar instance
	 */
	public S scan(BasePackage... basePackages) {
		return copy(this.types, merge(this.basePackages, basePackages), this.using,
				this.httpServiceProxyFactoryCustomizers, this.beanRegistrationCustomizers);
	}

	/**
	 * Return a new instance of this registrar that uses a unique
	 * {@link HttpServiceProxyFactoryProvider} bean to lookup the
	 * {@link HttpServiceProxyFactory} by ID.
	 * @param id the ID to pass to the provider
	 * @return a new registrar instance
	 */
	public S using(String id) {
		return copy(this.types, basePackages, id, this.httpServiceProxyFactoryCustomizers,
				this.beanRegistrationCustomizers);
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link HttpServiceProxyFactory} builder.
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public S proxyFactory(Consumer<HttpServiceProxyFactory.Builder> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return proxyFactory(asBiConsumer(customizer));
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link HttpServiceProxyFactory} builder.
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public S proxyFactory(BiConsumer<HttpServiceProxyFactory.Builder, SupplierContext> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return copy(this.types, this.basePackages, this.using,
				merge(this.httpServiceProxyFactoryCustomizers, customizer), this.beanRegistrationCustomizers);
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link BeanRegistry.Spec spec} used to register the bean
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public S beanRegistration(Consumer<BeanRegistry.Spec<?>> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return beanRegistration(asBiConsumer(customizer));
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link BeanRegistry.Spec spec} used to register the bean
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public S beanRegistration(BiConsumer<BeanRegistry.Spec<?>, Class<?>> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return copy(this.types, this.basePackages, this.using, this.httpServiceProxyFactoryCustomizers,
				merge(this.beanRegistrationCustomizers, customizer));
	}

	/**
	 * Return a {@link BeanRegistrar} that registers a single
	 * {@link HttpServiceProxyFactoryProvider} bean that contains the proxies that would
	 * otherwise be created as beans.
	 * @param id the ID that the registed {@link HttpServiceProxyFactoryProvider} supports
	 * @return a new {@link BeanRegistrar} the registers the
	 * {@link HttpServiceProxyFactoryProvider} bean
	 */
	public BeanRegistrar provider(String id) {
		Assert.hasText(id, "'id' must not be empty");
		return (registry, env) -> registry.registerBean(HttpServiceProxyFactoryProvider.class,
				(spec) -> spec.supplier((supplierContext) -> supplyProvider(supplierContext, id)));
	}

	@Override
	public void register(BeanRegistry registry, Environment env) {
		collectBeanClasses().forEach((beanClass) -> registerBean(registry, beanClass));
	}

	private HttpServiceProxyFactoryProvider supplyProvider(SupplierContext supplierContext, String id) {
		HttpServiceProxyFactory proxyFactory = supplyProxyFactory(supplierContext);
		return (requestedId) -> (!id.equals(requestedId)) ? null : proxyFactory;
	}

	private <T> void registerBean(BeanRegistry registry, Class<T> beanClass) {
		registry.registerBean(beanClass, (spec) -> {
			spec.supplier((supplierContext) -> createProxy(beanClass, supplierContext));
			this.beanRegistrationCustomizers.forEach((customizer) -> customizer.accept(spec, beanClass));
		});
	}

	private <T> T createProxy(Class<T> beanClass, SupplierContext supplierContext) {
		return supplyProxyFactory(supplierContext).createClient(beanClass);
	}

	private HttpServiceProxyFactory supplyProxyFactory(SupplierContext supplierContext) {
		if (StringUtils.hasText(this.using)) {
			return supplierContext.bean(HttpServiceProxyFactoryProvider.class)
				.getRequiredHttpServiceProxyFactory(this.using);
		}
		HttpServiceProxyFactory.Builder builder = HttpServiceProxyFactory
			.builderFor(supplyHttpExchangeAdapter(supplierContext));
		this.httpServiceProxyFactoryCustomizers.forEach((customizer) -> customizer.accept(builder, supplierContext));
		return builder.build();
	}

	private Set<Class<?>> collectBeanClasses() {
		Scanner scanner = new Scanner();
		Set<Class<?>> beanClasses = new LinkedHashSet<>();
		this.types.forEach(beanClasses::add);
		beanClasses.addAll(scanner.scan(this.basePackages));
		return Collections.unmodifiableSet(beanClasses);
	}

	/**
	 * Supply the {@link HttpExchangeAdapter} that should be used with the
	 * {@link HttpServiceProxyFactory}.
	 * @param supplierContext the supplier context
	 * @return a {@link HttpExchangeAdapter} instance
	 */
	protected abstract HttpExchangeAdapter supplyHttpExchangeAdapter(SupplierContext supplierContext);

	/**
	 * Factory method to be implemented by subclasses in order to create a new instance of
	 * the correct type.
	 * @param types the type to register
	 * @param basePackages the base packages to scan
	 * @param using the {@link HttpServiceProxyFactoryProvider} id to use
	 * @param httpServiceProxyFactoryCustomizers the {@link HttpServiceProxyFactory}
	 * customizers to apply
	 * @param beanRegistrationCustomizers the bean registration customizers to apply
	 * @return a new instance of the correct type
	 * @see #HttpServiceProxyBeanRegistrar(List, List, List, List)
	 */
	protected abstract S copy(List<Class<?>> types, List<BasePackage> basePackages, @Nullable String using,
			List<BiConsumer<Builder, SupplierContext>> httpServiceProxyFactoryCustomizers,
			List<BiConsumer<Spec<?>, Class<?>>> beanRegistrations);

	/**
	 * Helper method to create a new list from an existing list and an additional element.
	 * @param <T> the element type
	 * @param list the existing list
	 * @param additional the additional element
	 * @return a new merged list
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	protected static <T> List<T> merge(List<T> list, T... additional) {
		List<T> result = new ArrayList<T>(list);
		result.addAll(List.of(additional));
		return Collections.unmodifiableList(result);
	}

	/**
	 * Helper method to convert a {@link Consumer} to a {@link BiConsumer} by simply
	 * ignoring that second argument.
	 * @param <T> the first accepted type
	 * @param <U> the second accepted type
	 * @param consumer the consumer to call
	 * @return a new consumer that calls the bi-consumer
	 */
	protected static <T, U> BiConsumer<T, U> asBiConsumer(Consumer<T> consumer) {
		return (t, u) -> consumer.accept(t);
	}

	/**
	 * A single base package that can be scanned. By default all classes in the package
	 * will be included, use the {@code include...} and {@code exclude...} methods if
	 * further restrictions are required. All matching includes, and no matching excludes
	 * are required for a scanned class to be used.
	 */
	public static class BasePackage {

		private static final @Nullable Pattern NO_PATTERN = null;

		private final String name;

		private final List<TypeFilter> includes;

		private final List<TypeFilter> excludes;

		private BasePackage(String name, List<TypeFilter> includes, List<TypeFilter> excludes) {
			this.name = name;
			this.includes = includes;
			this.excludes = excludes;
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link AnnotationTypeFilter} include.
		 * @param targetType the type to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage include(Class<?> targetType) {
			return include(new AssignableTypeFilter(targetType));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link AnnotationTypeFilter} include.
		 * @param annotationType the annotation type to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage includeAnnotated(Class<? extends Annotation> annotationType) {
			return include(new AnnotationTypeFilter(annotationType));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link RegexPatternTypeFilter} include.
		 * @param regex the regex to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage include(String regex) {
			Assert.notNull(regex, "'regex' must not be null");
			return include(Pattern.compile(regex));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link RegexPatternTypeFilter} include.
		 * @param pattern the pattern to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage include(Pattern pattern) {
			return include(new RegexPatternTypeFilter(pattern));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional {@link TypeFilter}
		 * include.
		 * @param typeFilter the type filter to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage include(TypeFilter typeFilter) {
			return new BasePackage(this.name, merge(this.includes, typeFilter), this.excludes);
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link AssignableTypeFilter} exclude.
		 * @param targetType the type to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage exclude(Class<?> targetType) {
			return exclude(new AssignableTypeFilter(targetType));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link AnnotationTypeFilter} exclude.
		 * @param annotationType the annotation type to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage excludeAnnotated(Class<? extends Annotation> annotationType) {
			return exclude(new AnnotationTypeFilter(annotationType));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link RegexPatternTypeFilter} exclude.
		 * @param regex the regex to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage exclude(String regex) {
			Assert.notNull(regex, "'regex' must not be null");
			return exclude(Pattern.compile(regex));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional
		 * {@link RegexPatternTypeFilter} exclude.
		 * @param pattern the pattern to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage exclude(Pattern pattern) {
			return exclude(new RegexPatternTypeFilter(pattern));
		}

		/**
		 * Return a new {@link BasePackage} instance with an additional {@link TypeFilter}
		 * exclude.
		 * @param pattern the pattern to match
		 * @return a new {@link BasePackage} instance
		 */
		public BasePackage exclude(TypeFilter typeFilter) {
			return new BasePackage(this.name, this.includes, merge(this.excludes, typeFilter));
		}

		String name() {
			return this.name;
		}

		public static BasePackage of(String packageName) {
			return new BasePackage(packageName, Collections.emptyList(), Collections.emptyList());
		}

		public static BasePackage of(Class<?> packageClass) {
			return new BasePackage(packageClass.getName(), Collections.emptyList(), Collections.emptyList());
		}

	}

	/**
	 * {@link ClassPathScanningCandidateComponentProvider} used for scanning.
	 */
	private static class Scanner extends ClassPathScanningCandidateComponentProvider {

		private static final InstanceSupplier<?> SUPPLIER_FOR_CANDIDATE_DETECTION = registeredBean -> new Object();

		Scanner() {
			super(false);
			setProxyFactory((componentMetadata) -> SUPPLIER_FOR_CANDIDATE_DETECTION);
		}

		Set<Class<?>> scan(Collection<BasePackage> basePackages) {
			Set<Class<?>> scannedClasses = new LinkedHashSet<>();
			for (BasePackage basePackage : basePackages) {
				resetFilters(false);
				basePackage.includes.forEach(this::addIncludeFilter);
				basePackage.excludes.forEach(this::addIncludeFilter);
				findCandidateComponents(basePackage.name()).stream()
					.map(AbstractBeanDefinition.class::cast)
					.map(this::resolveBeanClass)
					.forEach(scannedClasses::add);
			}
			return Collections.unmodifiableSet(scannedClasses);
		}

		private Class<?> resolveBeanClass(AbstractBeanDefinition beanDefinition) {
			try {
				Class<?> beanClass = beanDefinition.resolveBeanClass(null);
				Assert.state(beanClass != null, () -> "No bean class name set");
				return beanClass;
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
