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

package org.springframework.beans.factory.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumer;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.SuppliedRootBeanDefinitionBuilder.Using;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutowiredInstantiationArgumentsResolver}.
 *
 * @author pwebb
 * @since 6.0
 */
class AutowiredInstantiationArgumentsResolverTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	//
	// @Test
	// void createWhenClassTypeIsNullThrowsException() {
	// assertThatIllegalArgumentException().isThrownBy(() -> new
	// SuppliedRootBeanDefinitionBuilder((Class<?>) null))
	// .withMessage("'beanType' must not be null");
	// }
	//
	// @Test
	// void createWhenResolvableTypeIsNullThrowsException() {
	// assertThatIllegalArgumentException()
	// .isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder((ResolvableType) null))
	// .withMessage("'beanType' must not be null");
	// }
	//
	// @Test
	// void createWhenResolvableTypeCannotBeResolvedThrowsException() {
	// assertThatIllegalArgumentException()
	// .isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(ResolvableType.NONE))
	// .withMessage("'beanType' must be resolvable");
	// }
	//
	// @Test
	// void usingConstructorWhenNoConstructorFoundThrowsException() {
	// SuppliedRootBeanDefinitionBuilder builder = new
	// SuppliedRootBeanDefinitionBuilder(SingleArgConstructor.class);
	// assertThatIllegalArgumentException().isThrownBy(() ->
	// builder.usingConstructor(Integer.class)).withMessage(
	// "No constructor with type(s) [java.lang.Integer] found on " +
	// SingleArgConstructor.class.getName());
	// }
	//
	// @Test
	// void usingConstructorWhenFound() {
	//
	// SuppliedRootBeanDefinitionBuilder builder = new
	// SuppliedRootBeanDefinitionBuilder(SingleArgConstructor.class);
	// assertThat(builder.usingConstructor(String.class)).isNotNull();
	// }
	//
	// @Test
	// void usingMethodWhenNoMethodFoundThrowsException() {
	// SuppliedRootBeanDefinitionBuilder builder = new
	// SuppliedRootBeanDefinitionBuilder(String.class);
	// assertThatIllegalArgumentException()
	// .isThrownBy(() -> builder.usingFactoryMethod(SingleArgFactory.class, "single",
	// Integer.class))
	// .withMessage("No method 'single' with type(s) [java.lang.Integer] found on "
	// + SingleArgFactory.class.getName());
	// }
	//
	// @Test
	// void usingWhenMethodOnInterface() {
	// this.beanFactory.registerSingleton("factory", new MethodOnInterfaceImpl());
	// SuppliedRootBeanDefinitionBuilder builder = new
	// SuppliedRootBeanDefinitionBuilder(String.class);
	// Using using = builder.usingFactoryMethod(MethodOnInterface.class, "test");
	// Instantiator instantiator = new Instantiator(using);
	// assertThat(using.resolvedBy(this.beanFactory,
	// instantiator).getInstanceSupplier().get()).isEqualTo("Test");
	// }
	//
	// @Test
	// void usingMethodWhenFound() {
	// SuppliedRootBeanDefinitionBuilder builder = new
	// SuppliedRootBeanDefinitionBuilder(String.class);
	// assertThat(builder.usingFactoryMethod(SingleArgFactory.class, "single",
	// String.class)).isNotNull();
	// }
	//
	// @Test
	// void instanceSupplierUsesInjectedBeanNameWhenNoBeanNameSpecified() {
	// RootBeanDefinition beanDefinition =
	// RootBeanDefinition.supply(SingleArgConstructor.class)
	// .usingConstructor(String.class).resolvedBy(this.beanFactory, new Instantiator());
	// this.beanFactory.registerBeanDefinition("myTest", beanDefinition);
	// assertThatExceptionOfType(UnsatisfiedDependencyException.class)
	// .isThrownBy(() -> this.beanFactory.getBean(SingleArgConstructor.class))
	// .withMessageContaining("bean with name 'myTest'");
	// }
	//
	// @Test
	// void resolveNoArgConstructor() {
	// RootBeanDefinition beanDefinition = new RootBeanDefinition(NoArgConstructor.class);
	// this.beanFactory.registerBeanDefinition("test", beanDefinition);
	// RegisteredBean registeredBean = new RegisteredBean("test", this.beanFactory);
	// Object[] resolved =
	// AutowiredInstantiationArgumentsResolver.forConstructor().resolve(registeredBean);
	// assertThat(resolved).isEmpty();
	// }
	//

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveSingleArgConstructor(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThat(source.getResolver().resolve(registeredBean)).containsExactly("1");
	}

	@ParameterizedResolverTest(Sources.INNER_CLASS_SINGLE_ARG)
	void resolvedNestedSingleArgConstructor(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThat(source.getResolver().resolve(registeredBean)).containsExactly("1");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveRequiredDependencyNotPresentThrowsUnsatisfiedDependencyException(Source source) {
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> source.getResolver().resolve(registeredBean)).satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("test");
					assertThat(ex.getInjectionPoint()).isNotNull();
					assertThat(ex.getInjectionPoint().getMember()).isEqualTo(source.lookupExecutable(registeredBean));
				});
	}

	@Test
	void resolveInInstanceSupplierWithSelfReferenceThrowsException() {
		// SingleArgFactory.single(...) expects a String to be injected
		// and our own bean is a String so it's a valid candidate
		this.beanFactory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(InstanceSupplier.of((registeredBean) -> {
			Object[] args = AutowiredInstantiationArgumentsResolver
					.forFactoryMethod(SingleArgFactory.class, "single", String.class).resolve(registeredBean);
			return new SingleArgFactory().single((String) args[0]);
		}));
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> this.beanFactory.getBean("test"))
				.havingCause().isInstanceOf(UnsatisfiedDependencyException.class);
	}

	@ParameterizedResolverTest(Sources.ARRAY_OF_BEANS)
	void resolveArrayOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat((Object[]) arguments[0]).containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.ARRAY_OF_BEANS)
	void resolveRequiredArrayOfBeansInjectEmptyArray(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat((Object[]) arguments[0]).isEmpty();
	}

	@ParameterizedResolverTest(Sources.LIST_OF_BEANS)
	void resolveListOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isInstanceOf(List.class).asList().containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.LIST_OF_BEANS)
	void resolveRequiredListOfBeansInjectEmptyList(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat((List<?>) arguments[0]).isEmpty();
	}

	@ParameterizedResolverTest(Sources.SET_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveSetOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat((Set<String>) arguments[0]).containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.SET_OF_BEANS)
	void resolveRequiredSetOfBeansInjectEmptySet(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat((Set<?>) arguments[0]).isEmpty();
	}

	@ParameterizedResolverTest(Sources.MAP_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveMapOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat((Map<String, String>) arguments[0]).containsExactly(entry("one", "1"), entry("two", "2"));
	}

	@ParameterizedResolverTest(Sources.MAP_OF_BEANS)
	void resolveRequiredMapOfBeansInjectEmptySet(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat((Map<?, ?>) arguments[0]).isEmpty();
	}

	@ParameterizedResolverTest(Sources.MULTI_ARGS)
	void resolveMultiArgsConstructor(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
		this.beanFactory.registerResolvableDependency(ResourceLoader.class, resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(3);
		assertThat(arguments[0]).isEqualTo(resourceLoader);
		assertThat(arguments[1]).isEqualTo(environment);
		assertThat(((ObjectProvider<?>) arguments[2]).getIfAvailable()).isEqualTo("1");
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveMixedArgsConstructorWithUserValue(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
		this.beanFactory.registerResolvableDependency(ResourceLoader.class, resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registerBean = source.registerBean(this.beanFactory, (beanDefinition) -> {
			beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, "user-value");
		});
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(3);
		assertThat(arguments[0]).isEqualTo(resourceLoader);
		assertThat(arguments[1]).isEqualTo("user-value");
		assertThat(arguments[2]).isEqualTo(environment);
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveMixedArgsConstructorWithUserBeanReference(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
		this.beanFactory.registerResolvableDependency(ResourceLoader.class, resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory, (beanDefinition) -> {
			beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference("two"));
		});
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(3);
		assertThat(arguments[0]).isEqualTo(resourceLoader);
		assertThat(arguments[1]).isEqualTo("2");
		assertThat(arguments[2]).isEqualTo(environment);
	}

	@Test
	void resolveUserValueWithTypeConversionRequired() {
		Source source = new Source(CharDependency.class,
				AutowiredInstantiationArgumentsResolver.forConstructor(char.class));
		RegisteredBean registerBean = source.registerBean(this.beanFactory, (beanDefinition) -> {
			beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, "\\");
		});
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isInstanceOf(Character.class).isEqualTo('\\');
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveUserValueWithBeanReference(Source source) {
		this.beanFactory.registerSingleton("stringBean", "string");
		RegisteredBean registerBean = source.registerBean(this.beanFactory, (beanDefinition) -> beanDefinition
				.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference("stringBean")));
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveUserValueWithBeanDefinition(Source source) {
		AbstractBeanDefinition userValue = BeanDefinitionBuilder.rootBeanDefinition(String.class, () -> "string")
				.getBeanDefinition();
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, userValue));
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveUserValueThatIsAlreadyResolved(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		BeanDefinition mergedBeanDefinition = this.beanFactory.getMergedBeanDefinition("test");
		ValueHolder valueHolder = new ValueHolder('a');
		valueHolder.setConvertedValue("this is an a");
		mergedBeanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, valueHolder);
		Object[] arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isEqualTo("this is an a");
	}

	/**
	 * Parameterized {@link Using} test backed by a {@link Sources}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@ArgumentsSource(SourcesArguments.class)
	static @interface ParameterizedResolverTest {

		Sources value();

	}

	/**
	 * {@link ArgumentsProvider} delegating to the {@link Sources}.
	 */
	static class SourcesArguments implements ArgumentsProvider, AnnotationConsumer<ParameterizedResolverTest> {

		private Sources source;

		@Override
		public void accept(ParameterizedResolverTest annotation) {
			this.source = annotation.value();
		}

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			return this.source.provideArguments(context);
		}

	}

	/**
	 * Sources for parameterized tests.
	 */
	enum Sources {

		SINGLE_ARG {

			@Override
			protected void setup() {
				add(SingleArgConstructor.class, AutowiredInstantiationArgumentsResolver.forConstructor(String.class));
				add(String.class, AutowiredInstantiationArgumentsResolver.forFactoryMethod(SingleArgFactory.class,
						"single", String.class));
			}

		},

		INNER_CLASS_SINGLE_ARG {

			@Override
			protected void setup() {
				add(Enclosing.InnerSingleArgConstructor.class,
						AutowiredInstantiationArgumentsResolver.forConstructor(String.class));
				add(String.class, AutowiredInstantiationArgumentsResolver
						.forFactoryMethod(Enclosing.InnerSingleArgFactory.class, "single", String.class));
			}

		},

		ARRAY_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver.forConstructor(String[].class));
				add(String.class, AutowiredInstantiationArgumentsResolver.forFactoryMethod(BeansCollectionFactory.class,
						"array", String[].class));
			}

		},

		LIST_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver.forConstructor(List.class));
				add(String.class, AutowiredInstantiationArgumentsResolver.forFactoryMethod(BeansCollectionFactory.class,
						"list", List.class));
			}

		},

		SET_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver.forConstructor(Set.class));
				add(String.class, AutowiredInstantiationArgumentsResolver.forFactoryMethod(BeansCollectionFactory.class,
						"set", Set.class));
			}

		},

		MAP_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver.forConstructor(Map.class));
				add(String.class, AutowiredInstantiationArgumentsResolver.forFactoryMethod(BeansCollectionFactory.class,
						"map", Map.class));
			}

		},

		MULTI_ARGS {

			@Override
			protected void setup() {
				add(MultiArgsConstructor.class, AutowiredInstantiationArgumentsResolver
						.forConstructor(ResourceLoader.class, Environment.class, ObjectProvider.class));
				add(String.class, AutowiredInstantiationArgumentsResolver.forFactoryMethod(MultiArgsFactory.class,
						"multiArgs", ResourceLoader.class, Environment.class, ObjectProvider.class));
			}

		},

		MIXED_ARGS {

			@Override
			protected void setup() {
				add(MixedArgsConstructor.class, AutowiredInstantiationArgumentsResolver
						.forConstructor(ResourceLoader.class, String.class, Environment.class));
				add(String.class, AutowiredInstantiationArgumentsResolver.forFactoryMethod(MixedArgsFactory.class,
						"mixedArgs", ResourceLoader.class, String.class, Environment.class));
			}

		};

		private final List<Arguments> arguments;

		private Sources() {
			this.arguments = new ArrayList<>();
			setup();
		}

		protected abstract void setup();

		protected final void add(Class<?> beanClass, AutowiredInstantiationArgumentsResolver resolver) {
			this.arguments.add(Arguments.of(new Source(beanClass, resolver)));
		}

		final Stream<Arguments> provideArguments(ExtensionContext context) {
			return this.arguments.stream();
		}

	}

	static class Source {

		private final Class<?> beanClass;

		private final AutowiredInstantiationArgumentsResolver resolver;

		public Source(Class<?> beanClass, AutowiredInstantiationArgumentsResolver resolver) {
			this.beanClass = beanClass;
			this.resolver = resolver;
		}

		RegisteredBean registerBean(DefaultListableBeanFactory beanFactory) {
			return registerBean(beanFactory, (beanDefinition) -> {
			});
		}

		RegisteredBean registerBean(DefaultListableBeanFactory beanFactory,
				Consumer<RootBeanDefinition> beanDefinitionCustomizer) {
			String beanName = "test";
			RootBeanDefinition beanDefinition = new RootBeanDefinition(this.beanClass);
			beanDefinition.setInstanceSupplier(() -> {
				throw new BeanCurrentlyInCreationException(beanName);
			});
			beanDefinitionCustomizer.accept(beanDefinition);
			beanFactory.registerBeanDefinition(beanName, beanDefinition);
			return new RegisteredBean(beanName, beanFactory);
		}

		AutowiredInstantiationArgumentsResolver getResolver() {
			return this.resolver;
		}

		Executable lookupExecutable(RegisteredBean registeredBean) {
			return this.resolver.getLookup().get(registeredBean);
		}

		@Override
		public String toString() {
			return this.resolver.getLookup() + " with bean class " + ClassUtils.getShortName(this.beanClass);
		}

	}

	static class NoArgConstructor {

	}

	static class SingleArgConstructor {

		public SingleArgConstructor(String s) {
		}

	}

	static class SingleArgFactory {

		String single(String s) {
			return s;
		}

	}

	static class Enclosing {

		class InnerSingleArgConstructor {

			InnerSingleArgConstructor(String s) {
			}

		}

		class InnerSingleArgFactory {

			String single(String s) {
				return s;
			}

		}

	}

	static class BeansCollectionConstructor {

		public BeansCollectionConstructor(String[] beans) {

		}

		public BeansCollectionConstructor(List<String> beans) {

		}

		public BeansCollectionConstructor(Set<String> beans) {

		}

		public BeansCollectionConstructor(Map<String, String> beans) {

		}

	}

	static class BeansCollectionFactory {

		public String array(String[] beans) {
			return "test";
		}

		public String list(List<String> beans) {
			return "test";
		}

		public String set(Set<String> beans) {
			return "test";
		}

		public String map(Map<String, String> beans) {
			return "test";
		}

	}

	static class MultiArgsConstructor {

		public MultiArgsConstructor(ResourceLoader resourceLoader, Environment environment,
				ObjectProvider<String> provider) {
		}
	}

	static class MultiArgsFactory {

		String multiArgs(ResourceLoader resourceLoader, Environment environment, ObjectProvider<String> provider) {
			return "test";
		}
	}

	static class MixedArgsConstructor {

		public MixedArgsConstructor(ResourceLoader resourceLoader, String test, Environment environment) {

		}

	}

	static class MixedArgsFactory {

		String mixedArgs(ResourceLoader resourceLoader, String test, Environment environment) {
			return "test";
		}

	}

	static class CharDependency {

		CharDependency(char escapeChar) {
		}

	}

	static interface MethodOnInterface {

		default String test() {
			return "Test";
		}

	}

	static class MethodOnInterfaceImpl implements MethodOnInterface {

	}

}
