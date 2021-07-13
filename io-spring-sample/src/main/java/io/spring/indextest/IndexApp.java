
package io.spring.indextest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

import io.spring.bean.config.ConcurrentHashFilter;
import io.spring.bean.config.ConcurrentHashFilter.HashCodeConsumer;
import io.spring.bean.config.ConcurrentHashFilter.HashCodesExtractor;

public class IndexApp {

	public static void main(String[] args) throws IOException {
		List<Class<?>> beanClasses = getClasses("org.springframework");
		List<Class<?>> contextClasses = getClasses("org.springframework.context");
		// beanClasses.forEach(System.out::println);
		System.out.println(beanClasses.size());
		Container container = new WithoutIndex();
		long start = System.nanoTime();
		beanClasses.forEach((t) -> container.add(t));
		for (Class<?> candidate : beanClasses) {
			//System.out.println(candidate);
			container.doWithBeansOfType(candidate, (bean) -> {
			});
			container.doWithBeansOfType(candidate, (bean) -> {
			});
		}
		for (Class<?> candidate : contextClasses) {
			container.doWithBeansOfType(candidate, (bean) -> {
			});
			container.doWithBeansOfType(candidate, (bean) -> {
			});
		}
		// container.doWithBeansOfType(Object.class, (bean) -> {
		// });
		 System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
	}

	private static List<Class<?>> getClasses(String packageName) throws IOException {
		List<Class<?>> classes = new ArrayList<>();
		CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
				+ ClassUtils.convertClassNameToResourcePath(packageName) + "/**/*.class";
		Resource[] resources = resolver.getResources(packageSearchPath);
		for (Resource resource : resources) {
			try {
				if (resource.isReadable()) {
					MetadataReader metadataReader = readerFactory.getMetadataReader(
							resource);
					String name = metadataReader.getClassMetadata().getClassName();
					classes.add(ClassUtils.forName(name, null));
				}
			} catch (Throwable ex) {
			}
		}
		return classes;
	}

	private static abstract class Container {

		abstract void doWithBeansOfType(Class<?> type, Consumer<Bean> action);

		abstract Bean add(Class<?> beanClass);

	}

	private static class WithoutIndex extends Container {

		final Set<Bean> beans = Collections.newSetFromMap(new ConcurrentHashMap<>());

		WithoutIndex() {
		}

		Bean add(Class<?> beanClass) {
			Bean bean = new Bean(beanClass);
			this.beans.add(bean);
			return bean;
		}

		void doWithBeansOfType(Class<?> type, Consumer<Bean> action) {
			for (Bean bean : this.beans) {
				if (!bean.beanClass.getName().startsWith("java.")
						&& type.isAssignableFrom(bean.beanClass)) {
					action.accept(bean);
				}
			}
		}

	}

	private static class WithIndex extends WithoutIndex {

		private final ConcurrentHashFilter<Bean, Class<?>> filter;

		WithIndex() {
			this.filter = new ConcurrentHashFilter<>(new TypeHashCodesExtractor(),
					this.beans::iterator);
		}

		@Override
		Bean add(Class<?> beanClass) {
			Bean bean = super.add(beanClass);
			this.filter.add(bean);
			return bean;
		}

		@Override
		void doWithBeansOfType(Class<?> type, Consumer<Bean> action) {
			this.filter.doWithCandidates(type, (candidate) -> {
				if (this.beans.contains(candidate)
						&& !candidate.beanClass.getName().startsWith("java.")
						&& type.isAssignableFrom(candidate.beanClass)) {
					action.accept(candidate);
				}
			});
		}

	}

	private static class TypeHashCodesExtractor
			implements HashCodesExtractor<Bean, Class<?>> {

		@Override
		public void extractAttributes(Bean value, HashCodeConsumer<Class<?>> attributes) {
			addAll(value.beanClass, attributes);
		}

		private void addAll(Class<?> type, HashCodeConsumer<Class<?>> attributes) {
			if (!type.getName().startsWith("java.")) {
				// System.err.println(">> " + type);
				attributes.accept(type);
			}
			for (Class<?> iface : type.getInterfaces()) {
				addAll(iface, attributes);
			}
			if (type.getSuperclass() != null) {
				addAll(type.getSuperclass(), attributes);
			}
		}

	}

	private static class Bean {

		final Class<?> beanClass;

		public Bean(Class<?> beanClass) {
			this.beanClass = beanClass;
		}

		@Override
		public String toString() {
			return beanClass.toString();
		}

		@Override
		public int hashCode() {
			return beanClass.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return beanClass.equals(((Bean) obj).beanClass);
		}

	}

}
