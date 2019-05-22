/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import example.scannable.CustomComponent;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.NamedStubDao;
import example.scannable.StubFooDao;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Test;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation2.NamedStubDao2;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertSame;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ClassPathBeanDefinitionScannerTests {

	private static final String BASE_PACKAGE = "example.scannable";


	@Test
	public void testSimpleScanWithDefaultFiltersAndPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean("thoreau")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
		context.refresh();

		FooServiceImpl fooService = context.getBean("fooServiceImpl", FooServiceImpl.class);
		assertThat(context.getDefaultListableBeanFactory().containsSingleton("myNamedComponent")).isTrue();
		assertEquals("bar", fooService.foo(123));
		assertEquals("bar", fooService.lookupFoo(123));
		assertThat(context.isPrototype("thoreau")).isTrue();
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndPrimaryLazyBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.scan(BASE_PACKAGE);
		scanner.scan("org.springframework.context.annotation5");
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean("otherFooDao")).isTrue();
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("otherFooDao")).isFalse();
		assertThat(context.getBeanFactory().containsSingleton("fooServiceImpl")).isFalse();
		FooServiceImpl fooService = context.getBean("fooServiceImpl", FooServiceImpl.class);
		assertThat(context.getBeanFactory().containsSingleton("otherFooDao")).isTrue();
		assertEquals("other", fooService.foo(123));
		assertEquals("other", fooService.lookupFoo(123));
	}

	@Test
	public void testDoubleScan() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);
		scanner.scan(BASE_PACKAGE);

		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean("thoreau")).isTrue();
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndNoPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(7, beanCount);

		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndOverridingBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("stubFooDao", new RootBeanDefinition(TestBean.class));
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		// should not fail!
		scanner.scan(BASE_PACKAGE);
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndDefaultBeanNameClash() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		scanner.scan("org.springframework.context.annotation3");
		assertThatIllegalStateException().isThrownBy(() ->
				scanner.scan(BASE_PACKAGE))
			.withMessageContaining("stubFooDao")
			.withMessageContaining(StubFooDao.class.getName());
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndOverriddenEqualNamedBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("myNamedDao", new RootBeanDefinition(NamedStubDao.class));
		int initialBeanCount = context.getBeanDefinitionCount();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		int scannedBeanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, scannedBeanCount);
		assertEquals(initialBeanCount + scannedBeanCount, context.getBeanDefinitionCount());
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndOverriddenCompatibleNamedBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(NamedStubDao.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		context.registerBeanDefinition("myNamedDao", bd);
		int initialBeanCount = context.getBeanDefinitionCount();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		int scannedBeanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, scannedBeanCount);
		assertEquals(initialBeanCount + scannedBeanCount, context.getBeanDefinitionCount());
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndSameBeanTwice() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		// should not fail!
		scanner.scan(BASE_PACKAGE);
		scanner.scan(BASE_PACKAGE);
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndSpecifiedBeanNameClash() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		scanner.scan("org.springframework.context.annotation2");
		assertThatIllegalStateException().isThrownBy(() ->
				scanner.scan(BASE_PACKAGE))
			.withMessageContaining("myNamedDao")
			.withMessageContaining(NamedStubDao.class.getName())
			.withMessageContaining(NamedStubDao2.class.getName());
	}

	@Test
	public void testCustomIncludeFilterWithoutDefaultsButIncludingPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, beanCount);
		assertThat(context.containsBean("messageBean")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
	}

	@Test
	public void testCustomIncludeFilterWithoutDefaultsAndNoPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, beanCount);
		assertThat(context.containsBean("messageBean")).isTrue();
		assertThat(context.containsBean("serviceInvocationCounter")).isFalse();
		assertThat(context.containsBean("fooServiceImpl")).isFalse();
		assertThat(context.containsBean("stubFooDao")).isFalse();
		assertThat(context.containsBean("myNamedComponent")).isFalse();
		assertThat(context.containsBean("myNamedDao")).isFalse();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
	}

	@Test
	public void testCustomIncludeFilterAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(13, beanCount);
		assertThat(context.containsBean("messageBean")).isTrue();
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
	}

	@Test
	public void testCustomAnnotationExcludeFilterAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addExcludeFilter(new AnnotationTypeFilter(Aspect.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(11, beanCount);
		assertThat(context.containsBean("serviceInvocationCounter")).isFalse();
		assertThat(context.containsBean("fooServiceImpl")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
	}

	@Test
	public void testCustomAssignableTypeExcludeFilterAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(11, beanCount);
		assertThat(context.containsBean("fooServiceImpl")).isFalse();
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
	}

	@Test
	public void testCustomAssignableTypeExcludeFilterAndDefaultsWithoutPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, beanCount);
		assertThat(context.containsBean("fooServiceImpl")).isFalse();
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isFalse();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isFalse();
	}

	@Test
	public void testMultipleCustomExcludeFiltersAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		scanner.addExcludeFilter(new AnnotationTypeFilter(Aspect.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(10, beanCount);
		assertThat(context.containsBean("fooServiceImpl")).isFalse();
		assertThat(context.containsBean("serviceInvocationCounter")).isFalse();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
	}

	@Test
	public void testCustomBeanNameGenerator() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(12, beanCount);
		assertThat(context.containsBean("fooServiceImpl")).isFalse();
		assertThat(context.containsBean("fooService")).isTrue();
		assertThat(context.containsBean("serviceInvocationCounter")).isTrue();
		assertThat(context.containsBean("stubFooDao")).isTrue();
		assertThat(context.containsBean("myNamedComponent")).isTrue();
		assertThat(context.containsBean("myNamedDao")).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME)).isTrue();
		assertThat(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
	}

	@Test
	public void testMultipleBasePackagesWithDefaultsOnly() {
		GenericApplicationContext singlePackageContext = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner singlePackageScanner = new ClassPathBeanDefinitionScanner(singlePackageContext);
		GenericApplicationContext multiPackageContext = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner multiPackageScanner = new ClassPathBeanDefinitionScanner(multiPackageContext);
		int singlePackageBeanCount = singlePackageScanner.scan(BASE_PACKAGE);
		assertEquals(12, singlePackageBeanCount);
		multiPackageScanner.scan(BASE_PACKAGE, "org.springframework.dao.annotation");
		// assertTrue(multiPackageBeanCount > singlePackageBeanCount);
	}

	@Test
	public void testMultipleScanCalls() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int initialBeanCount = context.getBeanDefinitionCount();
		int scannedBeanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, scannedBeanCount);
		assertEquals(scannedBeanCount, context.getBeanDefinitionCount() - initialBeanCount);
		int addedBeanCount = scanner.scan("org.springframework.aop.aspectj.annotation");
		assertEquals(initialBeanCount + scannedBeanCount + addedBeanCount, context.getBeanDefinitionCount());
	}

	@Test
	public void testBeanAutowiredWithAnnotationConfigEnabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("myBf", new RootBeanDefinition(StaticListableBeanFactory.class));
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);
		context.refresh();

		FooServiceImpl fooService = context.getBean("fooService", FooServiceImpl.class);
		StaticListableBeanFactory myBf = (StaticListableBeanFactory) context.getBean("myBf");
		MessageSource ms = (MessageSource) context.getBean("messageSource");
		assertThat(fooService.isInitCalled()).isTrue();
		assertEquals("bar", fooService.foo(123));
		assertEquals("bar", fooService.lookupFoo(123));
		assertSame(context.getDefaultListableBeanFactory(), fooService.beanFactory);
		assertEquals(2, fooService.listableBeanFactory.size());
		assertSame(context.getDefaultListableBeanFactory(), fooService.listableBeanFactory.get(0));
		assertSame(myBf, fooService.listableBeanFactory.get(1));
		assertSame(context, fooService.resourceLoader);
		assertSame(context, fooService.resourcePatternResolver);
		assertSame(context, fooService.eventPublisher);
		assertSame(ms, fooService.messageSource);
		assertSame(context, fooService.context);
		assertEquals(1, fooService.configurableContext.length);
		assertSame(context, fooService.configurableContext[0]);
		assertSame(context, fooService.genericContext);
	}

	@Test
	public void testBeanNotAutowiredWithAnnotationConfigDisabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(7, beanCount);
		context.refresh();

		try {
			context.getBean("fooService");
		}
		catch (BeanCreationException expected) {
			assertThat(expected.contains(BeanInstantiationException.class)).isTrue();
			// @Lookup method not substituted
		}
	}

	@Test
	public void testAutowireCandidatePatternMatches() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(true);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		scanner.setAutowireCandidatePatterns("*FooDao");
		scanner.scan(BASE_PACKAGE);
		context.refresh();

		FooServiceImpl fooService = (FooServiceImpl) context.getBean("fooService");
		assertEquals("bar", fooService.foo(123));
		assertEquals("bar", fooService.lookupFoo(123));
	}

	@Test
	public void testAutowireCandidatePatternDoesNotMatch() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(true);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		scanner.setAutowireCandidatePatterns("*NoSuchDao");
		scanner.scan(BASE_PACKAGE);
		context.refresh();
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				context.getBean("fooService"))
			.satisfies(ex -> assertThat(ex.getMostSpecificCause()).isInstanceOf(NoSuchBeanDefinitionException.class));
	}


	private static class TestBeanNameGenerator extends AnnotationBeanNameGenerator {

		@Override
		public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
			String beanName = super.generateBeanName(definition, registry);
			return beanName.replace("Impl", "");
		}
	}


	@Component("toBeIgnored")
	public class NonStaticInnerClass {
	}

}
