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

package org.springframework.web.service.registry;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * An immutable collection of HTTP service types that can be specified explicitly or
 * discovered by scanning a package.
 *
 * The HTTP service types can be specified directly, or discovered by scanning base
 * packages.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 * @see HttpServiceProxyRegistry
 */
public final class HttpServiceTypes implements Iterable<Class<?>> {

	private static final HttpServiceTypes EMPTY = new HttpServiceTypes(Collections.emptyList(),
			Collections.emptyList());

	private final List<Class<?>> serviceTypes;

	private final List<BasePackage> basePackages;

	private volatile @Nullable Set<Class<?>> resolved;

	/**
	 * Create a new {@link HttpServiceTypes} instance.
	 * @param id the group ID
	 * @param serviceTypes the HTTP service types
	 * @param basePackages the base packages to scan
	 * @param proxyFactoryPostProcessors factory post processors to apply
	 */
	private HttpServiceTypes(List<Class<?>> serviceTypes, List<BasePackage> basePackages) {
		this.serviceTypes = serviceTypes;
		this.basePackages = basePackages;
	}

	/**
	 * Return a new instance that additionally adds the HTTP service types.
	 * @param serviceTypes the types to add
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public HttpServiceTypes withServiceTypes(Class<?>... serviceTypes) {
		return new HttpServiceTypes(merge(this.serviceTypes, serviceTypes), this.basePackages);
	}

	/**
	 * Return a new instance that additionally scans given package for HTTP service types.
	 * @param basePackageNames the package names to scan
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public HttpServiceTypes withScan(String... basePackageNames) {
		return withScan(Arrays.stream(basePackageNames).map(BasePackage::of).toArray(BasePackage[]::new));
	}

	/**
	 * Return a new instance that additionally scans given package for HTTP service types.
	 * @param serviceTypes the types to add
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public HttpServiceTypes withScan(Class<?>... basePackageClasses) {
		return withScan(Arrays.stream(basePackageClasses).map(BasePackage::of).toArray(BasePackage[]::new));
	}

	/**
	 * Return a a new instance that additionally scans given package for HTTP service
	 * types.
	 * @param serviceTypes the types to add
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public HttpServiceTypes withScan(BasePackage... basePackages) {
		return new HttpServiceTypes(this.serviceTypes, merge(this.basePackages, basePackages));
	}

	@Override
	public Iterator<Class<?>> iterator() {
		return resolve().iterator();
	}

	private Set<Class<?>> resolve() {
		Set<Class<?>> resolved = this.resolved;
		if (resolved == null) {
			resolved = collectBeanClasses();
			this.resolved = resolved;
		}
		return resolved;
	}

	private Set<Class<?>> collectBeanClasses() {
		Scanner scanner = new Scanner();
		Set<Class<?>> beanClasses = new LinkedHashSet<>();
		this.serviceTypes.forEach(beanClasses::add);
		beanClasses.addAll(scanner.scan(this.basePackages));
		return Collections.unmodifiableSet(beanClasses);
	}

	/**
	 * Helper method to create a new list from an existing list and an additional element.
	 * @param <T> the element type
	 * @param list the existing list
	 * @param additional the additional element
	 * @return a new merged list
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	private static <T> List<T> merge(List<T> list, T... additional) {
		List<T> result = new ArrayList<T>(list);
		result.addAll(List.of(additional));
		return Collections.unmodifiableList(result);
	}

	/**
	 * Factory method to create new {@link HttpServiceTypes} instance containing the
	 * specified service types.
	 * @param serviceTypes the service types
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public static HttpServiceTypes of(Class<?>... serviceTypes) {
		return of().withServiceTypes(serviceTypes);
	}

	/**
	 * Factory method to create new {@link HttpServiceTypes} instance containing service
	 * types scanned from the given base packages.
	 * @param basePackageNames the package names to scan
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public static HttpServiceTypes ofScan(String... basePackageNames) {
		return of().withScan(basePackageNames);
	}

	/**
	 * Factory method to create new {@link HttpServiceTypes} instance containing service
	 * types scanned from the given base packages.
	 * @param basePackageClasses classes used to identify the packages to scan
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public static HttpServiceTypes ofScan(Class<?>... basePackageClasses) {
		return of().withScan(basePackageClasses);
	}

	/**
	 * Factory method to create new {@link HttpServiceTypes} instance containing service
	 * types scanned from the given base packages.
	 * @param basePackages the base packages to scan
	 * @return a new {@link HttpServiceTypes} instance
	 */
	public static HttpServiceTypes ofScan(BasePackage... basePackages) {
		return of().withScan(basePackages);
	}

	/**
	 * Factory method to return an empty {@link HttpServiceTypes} instance.
	 * @return a empty {@link HttpServiceTypes} instance
	 */
	public static HttpServiceTypes of() {
		return EMPTY;
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

		/**
		 * Return the name of the package.
		 * @return the package name
		 */
		String name() {
			return this.name;
		}

		/**
		 * Factory method to create new {@link BasePackage} of the given package name.
		 * @param packageName the package name
		 * @return a new {@link BasePackage} instance
		 */
		public static BasePackage of(String packageName) {
			return new BasePackage(packageName, Collections.emptyList(), Collections.emptyList());
		}

		/**
		 * Factory method to create new {@link BasePackage} of a package name taken from
		 * the given class.
		 * @param packageClass a class contained in the package
		 * @return a new {@link BasePackage} instance
		 */
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
