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
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.SuppliedRootBeanDefinitionBuilder.Using;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowableBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SuppliedRootBeanDefinitionBuilder}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class SuppliedRootBeanDefinitionBuilderTests {

	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void accessViaRootBeanDefinition() {
		ResolvableType stringType = ResolvableType.forClass(String.class);
		SuppliedRootBeanDefinitionBuilder byClass = RootBeanDefinition.supply(String.class);
		assertThat(byClass).extracting("beanName").isNull();
		assertThat(byClass).extracting("beanClass").isEqualTo(String.class);
		assertThat(byClass).extracting("beanType").isNull();
		SuppliedRootBeanDefinitionBuilder byType = RootBeanDefinition.supply(stringType);
		assertThat(byType).extracting("beanName").isNull();
		assertThat(byType).extracting("beanClass").isEqualTo(String.class);
		assertThat(byType).extracting("beanType").isEqualTo(stringType);
		SuppliedRootBeanDefinitionBuilder namedByClass = RootBeanDefinition.supply("test", String.class);
		assertThat(namedByClass).extracting("beanName").isEqualTo("test");
		assertThat(namedByClass).extracting("beanClass").isEqualTo(String.class);
		assertThat(namedByClass).extracting("beanType").isNull();
		SuppliedRootBeanDefinitionBuilder namedByType = RootBeanDefinition.supply("test", stringType);
		assertThat(namedByType).extracting("beanName").isEqualTo("test");
		assertThat(namedByType).extracting("beanClass").isEqualTo(String.class);
		assertThat(byType).extracting("beanType").isEqualTo(stringType);
	}

	@Test
	void createWhenClassTypeIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, (Class<?>) null))
				.withMessage("'beanType' must not be null");
	}

	@Test
	void createWhenResolvableTypeIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, (ResolvableType) null))
				.withMessage("'beanType' must not be null");
	}

	@Test
	void createWhenResolvableTypeCannotBeResolvedThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, ResolvableType.NONE))
				.withMessage("'beanType' must be resolvable");
	}

	@Test
	void usingConstructorWhenNoConstructorFoundThrowsException() {
		SuppliedRootBeanDefinitionBuilder builder = new SuppliedRootBeanDefinitionBuilder(null,
				SingleArgConstructor.class);
		assertThatIllegalArgumentException().isThrownBy(() -> builder.usingConstructor(Integer.class)).withMessage(
				"No constructor with type(s) [java.lang.Integer] found on " + SingleArgConstructor.class.getName());
	}

	@Test
	void usingConstructorWhenFound() {
		SuppliedRootBeanDefinitionBuilder builder = new SuppliedRootBeanDefinitionBuilder(null,
				SingleArgConstructor.class);
		assertThat(builder.usingConstructor(String.class)).isNotNull();
	}

	@Test
	void usingMethodWhenNoMethodFoundThrowsException() {
		SuppliedRootBeanDefinitionBuilder builder = new SuppliedRootBeanDefinitionBuilder(null, String.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> builder.usingFactoryMethod(SingleArgFactory.class, "single", Integer.class))
				.withMessage("No method 'single' with type(s) [java.lang.Integer] found on "
						+ SingleArgFactory.class.getName());
	}

	@Test
	void usingWhenMethodOnInterface() {
		this.beanFactory.registerSingleton("factory", new MethodOnInterfaceImpl());
		SuppliedRootBeanDefinitionBuilder builder = new SuppliedRootBeanDefinitionBuilder(null, String.class);
		Using using = builder.usingFactoryMethod(MethodOnInterface.class, "test");
		Instantiator instantiator = new Instantiator(using);
		assertThat(using.resolvedBy(beanFactory, instantiator).getInstanceSupplier().get()).isEqualTo("Test");
	}

	@Test
	void usingMethodWhenFound() {
		SuppliedRootBeanDefinitionBuilder builder = new SuppliedRootBeanDefinitionBuilder(null, String.class);
		assertThat(builder.usingFactoryMethod(SingleArgFactory.class, "single", String.class)).isNotNull();
	}

	@Test
	void resolveNoArgConstructor() {
		Using using = RootBeanDefinition.supply(NoArgConstructor.class).usingConstructor();
		assertThat(new Instantiator(using).apply()).isEmpty();
	}

	@ParameterizedTestUsing(Source.SINGLE_ARG)
	void resolveSingleArgConstructor(Using using) {
		beanFactory.registerSingleton("one", "1");
		assertThat(new Instantiator(using).apply()).containsExactly("1");
	}

	@ParameterizedTestUsing(Source.SINGLE_ARG)
	void resolveRequiredDependencyNotPresentThrowsUnsatisfiedDependencyException(Using using) {
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> new Instantiator(using).apply()).satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("test");
					assertThat(ex.getInjectionPoint()).isNotNull();
					assertThat(ex.getInjectionPoint().getMember()).isEqualTo(using.getExecutable());
				});
	}

	@ParameterizedTestUsing(Source.ARRAY_OF_BEANS)
	void resolveArrayOfBeans(Using using) {
		beanFactory.registerSingleton("one", "1");
		beanFactory.registerSingleton("two", "2");
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat((Object[]) arguments[0]).containsExactly("1", "2");
	}

	@ParameterizedTestUsing(Source.ARRAY_OF_BEANS)
	void resolveRequiredArrayOfBeansInjectEmptyArray(Using using) {
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat((Object[]) arguments[0]).isEmpty();
	}

	@ParameterizedTestUsing(Source.LIST_OF_BEANS)
	void resolveListOfBeans(Using using) {
		beanFactory.registerSingleton("one", "1");
		beanFactory.registerSingleton("two", "2");
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isInstanceOf(List.class).asList().containsExactly("1", "2");
	}

	@ParameterizedTestUsing(Source.LIST_OF_BEANS)
	void resolveRequiredListOfBeansInjectEmptyList(Using using) {
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat((List<?>) arguments[0]).isEmpty();
	}

	@ParameterizedTestUsing(Source.SET_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveSetOfBeans(Using using) {
		beanFactory.registerSingleton("one", "1");
		beanFactory.registerSingleton("two", "2");
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat((Set<String>) arguments[0]).containsExactly("1", "2");
	}

	@ParameterizedTestUsing(Source.SET_OF_BEANS)
	void resolveRequiredSetOfBeansInjectEmptySet(Using using) {
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat((Set<?>) arguments[0]).isEmpty();
	}

	@ParameterizedTestUsing(Source.MAP_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveMapOfBeans(Using using) {
		beanFactory.registerSingleton("one", "1");
		beanFactory.registerSingleton("two", "2");
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat((Map<String, String>) arguments[0]).containsExactly(entry("one", "1"), entry("two", "2"));
	}

	@ParameterizedTestUsing(Source.MAP_OF_BEANS)
	void resolveRequiredMapOfBeansInjectEmptySet(Using using) {
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat((Map<?, ?>) arguments[0]).isEmpty();
	}

	@ParameterizedTestUsing(Source.MULTI_ARGS)
	void resolveMultiArgsConstructor(Using using) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
		beanFactory.registerResolvableDependency(ResourceLoader.class, resourceLoader);
		beanFactory.registerSingleton("environment", environment);
		beanFactory.registerSingleton("one", "1");
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(3);
		assertThat(arguments[0]).isEqualTo(resourceLoader);
		assertThat(arguments[1]).isEqualTo(environment);
		assertThat(((ObjectProvider<?>) arguments[2]).getIfAvailable()).isEqualTo("1");
	}

	@ParameterizedTestUsing(Source.MIXED_ARGS)
	void resolveMixedArgsConstructorWithUserValue(Using using) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
		beanFactory.registerResolvableDependency(ResourceLoader.class, resourceLoader);
		beanFactory.registerSingleton("environment", environment);
		Object[] arguments = new Instantiator(using).apply((beanDefinition) -> {
			beanDefinition.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, "user-value");
		});
		assertThat(arguments).hasSize(3);
		assertThat(arguments[0]).isEqualTo(resourceLoader);
		assertThat(arguments[1]).isEqualTo("user-value");
		assertThat(arguments[2]).isEqualTo(environment);
	}

	@ParameterizedTestUsing(Source.MIXED_ARGS)
	void resolveMixedArgsConstructorWithUserBeanReference(Using using) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
		beanFactory.registerResolvableDependency(ResourceLoader.class, resourceLoader);
		beanFactory.registerSingleton("environment", environment);
		beanFactory.registerSingleton("one", "1");
		beanFactory.registerSingleton("two", "2");
		Object[] arguments = new Instantiator(using).apply((beanDefinition) -> {
			beanDefinition.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, new RuntimeBeanReference("two"));
		});
		assertThat(arguments).hasSize(3);
		assertThat(arguments[0]).isEqualTo(resourceLoader);
		assertThat(arguments[1]).isEqualTo("2");
		assertThat(arguments[2]).isEqualTo(environment);
	}

	@Test
	void resolveUserValueWithTypeConversionRequired() {
		Using using = RootBeanDefinition.supply("test", CharDependency.class).usingConstructor(char.class);
		Object[] arguments = new Instantiator(using).apply((beanDefinition) -> {
			beanDefinition.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, "\\");
		});
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isInstanceOf(Character.class).isEqualTo('\\');
	}

	@ParameterizedTestUsing(Source.SINGLE_ARG)
	void resolveUserValueWithBeanReference(Using using) {
		beanFactory.registerSingleton("stringBean", "string");
		Object[] arguments = new Instantiator(using).apply((beanDefinition) -> {
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
					new RuntimeBeanReference("stringBean"));
		});
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isEqualTo("string");
	}

	@ParameterizedTestUsing(Source.SINGLE_ARG)
	void resolveUserValueWithBeanDefinition(Using using) {
		AbstractBeanDefinition userValue = BeanDefinitionBuilder.rootBeanDefinition(String.class, () -> "string")
				.getBeanDefinition();
		Object[] arguments = new Instantiator(using).apply((beanDefinition) -> {
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, userValue);
		});
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isEqualTo("string");
	}

	@ParameterizedTestUsing(Source.SINGLE_ARG)
	void resolveUserValueThatIsAlreadyResolved(Using using) {
		Object[] arguments = new Instantiator(using).apply((beanDefinition) -> {
			ValueHolder valueHolder = new ValueHolder('a');
			valueHolder.setConvertedValue("this is an a");
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, valueHolder);
		});
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isEqualTo("this is an a");
	}

	@ParameterizedTestUsing(Source.SINGLE_ARG)
	void createInvokeFactory(Using using) {
		beanFactory.registerSingleton("one", "1");
		Object[] arguments = new Instantiator(using).apply();
		assertThat(arguments).hasSize(1);
		assertThat(arguments[0]).isEqualTo("1");
	}

	/**
	 * Parameterized {@link Using} test backed by a {@link Source}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@ArgumentsSource(SourceArguments.class)
	static @interface ParameterizedTestUsing {

		Source value();

	}

	/**
	 * {@link ArgumentsProvider} delegating to the {@link Source}.
	 */
	static class SourceArguments implements ArgumentsProvider, AnnotationConsumer<ParameterizedTestUsing> {

		private Source source;

		@Override
		public void accept(ParameterizedTestUsing annotation) {
			this.source = annotation.value();
		}

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			return source.provideArguments();
		}

	}

	/**
	 * Sources for parameterized tests.
	 */
	enum Source {

		SINGLE_ARG {

			@Override
			protected void setup() {
				add(RootBeanDefinition.supply("test", SingleArgConstructor.class).usingConstructor(String.class));
				add(RootBeanDefinition.supply("test", String.class).usingFactoryMethod(SingleArgFactory.class, "single",
						String.class));
			}

		},

		ARRAY_OF_BEANS {

			@Override
			protected void setup() {
				add(RootBeanDefinition.supply("test", BeansCollectionConstructor.class)
						.usingConstructor(String[].class));
				add(RootBeanDefinition.supply("test", String.class).usingFactoryMethod(BeansCollectionFactory.class,
						"array", String[].class));
			}

		},

		LIST_OF_BEANS {

			@Override
			protected void setup() {
				add(RootBeanDefinition.supply("test", BeansCollectionConstructor.class).usingConstructor(List.class));
				add(RootBeanDefinition.supply("test", String.class).usingFactoryMethod(BeansCollectionFactory.class,
						"list", List.class));
			}

		},

		SET_OF_BEANS {

			@Override
			protected void setup() {
				add(RootBeanDefinition.supply("test", BeansCollectionConstructor.class).usingConstructor(Set.class));
				add(RootBeanDefinition.supply("test", String.class).usingFactoryMethod(BeansCollectionFactory.class,
						"set", Set.class));
			}

		},

		MAP_OF_BEANS {

			@Override
			protected void setup() {
				add(RootBeanDefinition.supply("test", BeansCollectionConstructor.class).usingConstructor(Map.class));
				add(RootBeanDefinition.supply("test", String.class).usingFactoryMethod(BeansCollectionFactory.class,
						"map", Map.class));
			}

		},

		MULTI_ARGS {

			@Override
			protected void setup() {
				add(RootBeanDefinition.supply("test", MultiArgsConstructor.class).usingConstructor(ResourceLoader.class,
						Environment.class, ObjectProvider.class));
				add(RootBeanDefinition.supply("test", String.class).usingFactoryMethod(MultiArgsFactory.class,
						"multiArgs", ResourceLoader.class, Environment.class, ObjectProvider.class));
			}

		},

		MIXED_ARGS {

			@Override
			protected void setup() {
				add(RootBeanDefinition.supply("test", MixedArgsConstructor.class).usingConstructor(ResourceLoader.class,
						String.class, Environment.class));
				add(RootBeanDefinition.supply("test", String.class).usingFactoryMethod(MixedArgsFactory.class,
						"mixedArgs", ResourceLoader.class, String.class, Environment.class));
			}

		};

		private final List<Arguments> arguments;

		private Source() {
			this.arguments = new ArrayList<>();
			setup();
		}

		protected abstract void setup();

		protected final void add(Using using) {
			this.arguments.add(Arguments.of(using));
		}

		final Stream<Arguments> provideArguments() {
			return this.arguments.stream();
		};

	}

	/**
	 * Instantiator function uses reflection and retains arguments for further assetions.
	 */
	private class Instantiator implements ThrowableBiFunction<BeanFactory, Object[], Object> {

		private final Using using;

		private Object[] arguments;

		public Instantiator(Using using) {
			this.using = using;
		}

		public Object[] apply() {
			return apply((bd) -> {
			});
		}

		public Object[] apply(Consumer<RootBeanDefinition> customizer) {
			return applyTo(beanFactory, customizer);
		}

		public Object[] applyTo(DefaultListableBeanFactory beanFactory, Consumer<RootBeanDefinition> customizer) {
			RootBeanDefinition beanDefinition = this.using.resolvedBy(beanFactory, this);
			customizer.accept(beanDefinition);
			beanDefinition.getInstanceSupplier().get();
			return this.arguments;
		}

		@Override
		public Object applyWithException(BeanFactory beanFactory, Object[] arguments) throws Exception {
			this.arguments = arguments;
			Executable executable = this.using.getExecutable();
			if (executable instanceof Constructor<?> constructor) {
				ReflectionUtils.makeAccessible(constructor);
				return constructor.newInstance(arguments);
			}
			if (executable instanceof Method method) {
				ReflectionUtils.makeAccessible(method);
				Object target = getTarget(beanFactory, method);
				return ReflectionUtils.invokeMethod(method, target, arguments);
			}
			throw new IllegalStateException();
		}

		private Object getTarget(BeanFactory beanFactory, Method method)
				throws InstantiationException, IllegalAccessException, IllegalArgumentException,
				InvocationTargetException, NoSuchMethodException, SecurityException {
			try {
				return beanFactory.getBean(method.getDeclaringClass());
			}
			catch (NoSuchBeanDefinitionException ex) {
				Constructor<?> constructor = method.getDeclaringClass().getDeclaredConstructor();
				ReflectionUtils.makeAccessible(constructor);
				return constructor.newInstance();
			}
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
