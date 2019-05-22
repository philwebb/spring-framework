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

package org.springframework.beans.factory.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.xml.sax.InputSource;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MethodReplacer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.tests.sample.beans.DependenciesBean;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.IndexedTestBean;
import org.springframework.tests.sample.beans.ResourceTestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.factory.DummyFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SerializationTestUtils;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static org.assertj.core.api.Assertions.assertThat;

import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertSame;

/**
 * Miscellaneous tests for XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 * @author Sam Brannen
 */
public class XmlBeanFactoryTests {

	private static final Class<?> CLASS = XmlBeanFactoryTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final ClassPathResource AUTOWIRE_CONTEXT = classPathResource("-autowire.xml");
	private static final ClassPathResource CHILD_CONTEXT = classPathResource("-child.xml");
	private static final ClassPathResource CLASS_NOT_FOUND_CONTEXT = classPathResource("-classNotFound.xml");
	private static final ClassPathResource COMPLEX_FACTORY_CIRCLE_CONTEXT = classPathResource("-complexFactoryCircle.xml");
	private static final ClassPathResource CONSTRUCTOR_ARG_CONTEXT = classPathResource("-constructorArg.xml");
	private static final ClassPathResource CONSTRUCTOR_OVERRIDES_CONTEXT = classPathResource("-constructorOverrides.xml");
	private static final ClassPathResource DELEGATION_OVERRIDES_CONTEXT = classPathResource("-delegationOverrides.xml");
	private static final ClassPathResource DEP_CARG_AUTOWIRE_CONTEXT = classPathResource("-depCargAutowire.xml");
	private static final ClassPathResource DEP_CARG_INNER_CONTEXT = classPathResource("-depCargInner.xml");
	private static final ClassPathResource DEP_CARG_CONTEXT = classPathResource("-depCarg.xml");
	private static final ClassPathResource DEP_DEPENDSON_INNER_CONTEXT = classPathResource("-depDependsOnInner.xml");
	private static final ClassPathResource DEP_DEPENDSON_CONTEXT = classPathResource("-depDependsOn.xml");
	private static final ClassPathResource DEP_PROP = classPathResource("-depProp.xml");
	private static final ClassPathResource DEP_PROP_ABN_CONTEXT = classPathResource("-depPropAutowireByName.xml");
	private static final ClassPathResource DEP_PROP_ABT_CONTEXT = classPathResource("-depPropAutowireByType.xml");
	private static final ClassPathResource DEP_PROP_MIDDLE_CONTEXT = classPathResource("-depPropInTheMiddle.xml");
	private static final ClassPathResource DEP_PROP_INNER_CONTEXT = classPathResource("-depPropInner.xml");
	private static final ClassPathResource DEP_MATERIALIZE_CONTEXT = classPathResource("-depMaterializeThis.xml");
	private static final ClassPathResource FACTORY_CIRCLE_CONTEXT = classPathResource("-factoryCircle.xml");
	private static final ClassPathResource INITIALIZERS_CONTEXT = classPathResource("-initializers.xml");
	private static final ClassPathResource INVALID_CONTEXT = classPathResource("-invalid.xml");
	private static final ClassPathResource INVALID_NO_SUCH_METHOD_CONTEXT = classPathResource("-invalidOverridesNoSuchMethod.xml");
	private static final ClassPathResource COLLECTIONS_XSD_CONTEXT = classPathResource("-localCollectionsUsingXsd.xml");
	private static final ClassPathResource MISSING_CONTEXT = classPathResource("-missing.xml");
	private static final ClassPathResource OVERRIDES_CONTEXT = classPathResource("-overrides.xml");
	private static final ClassPathResource PARENT_CONTEXT = classPathResource("-parent.xml");
	private static final ClassPathResource NO_SUCH_FACTORY_METHOD_CONTEXT = classPathResource("-noSuchFactoryMethod.xml");
	private static final ClassPathResource RECURSIVE_IMPORT_CONTEXT = classPathResource("-recursiveImport.xml");
	private static final ClassPathResource RESOURCE_CONTEXT = classPathResource("-resource.xml");
	private static final ClassPathResource TEST_WITH_DUP_NAMES_CONTEXT = classPathResource("-testWithDuplicateNames.xml");
	private static final ClassPathResource TEST_WITH_DUP_NAME_IN_ALIAS_CONTEXT = classPathResource("-testWithDuplicateNameInAlias.xml");
	private static final ClassPathResource REFTYPES_CONTEXT = classPathResource("-reftypes.xml");
	private static final ClassPathResource DEFAULT_LAZY_CONTEXT = classPathResource("-defaultLazyInit.xml");
	private static final ClassPathResource DEFAULT_AUTOWIRE_CONTEXT = classPathResource("-defaultAutowire.xml");

	private static ClassPathResource classPathResource(String suffix) {
		return new ClassPathResource(CLASSNAME + suffix, CLASS);
	}


	@Test  // SPR-2368
	public void testCollectionsReferredToAsRefLocals() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(COLLECTIONS_XSD_CONTEXT);
		factory.preInstantiateSingletons();
	}

	@Test
	public void testRefToSeparatePrototypeInstances() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);

		TestBean emma = (TestBean) xbf.getBean("emma");
		TestBean georgia = (TestBean) xbf.getBean("georgia");
		ITestBean emmasJenks = emma.getSpouse();
		ITestBean georgiasJenks = georgia.getSpouse();
		assertThat(emmasJenks != georgiasJenks).as("Emma and georgia think they have a different boyfriend").isTrue();
		assertThat(emmasJenks.getName().equals("Andrew")).as("Emmas jenks has right name").isTrue();
		assertThat(emmasJenks != xbf.getBean("jenks")).as("Emmas doesn't equal new ref").isTrue();
		assertThat(emmasJenks.getName().equals("Andrew")).as("Georgias jenks has right name").isTrue();
		assertThat(emmasJenks.equals(georgiasJenks)).as("They are object equal").isTrue();
		assertThat(emmasJenks.equals(xbf.getBean("jenks"))).as("They object equal direct ref").isTrue();
	}

	@Test
	public void testRefToSingleton() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(new EncodedResource(REFTYPES_CONTEXT, "ISO-8859-1"));

		TestBean jen = (TestBean) xbf.getBean("jenny");
		TestBean dave = (TestBean) xbf.getBean("david");
		TestBean jenks = (TestBean) xbf.getBean("jenks");
		ITestBean davesJen = dave.getSpouse();
		ITestBean jenksJen = jenks.getSpouse();
		assertThat(davesJen == jenksJen).as("1 jen instance").isTrue();
		assertThat(davesJen == jen).as("1 jen instance").isTrue();
	}

	@Test
	public void testInnerBeans() throws IOException {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);

		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		InputStream inputStream = getClass().getResourceAsStream(REFTYPES_CONTEXT.getPath());
		try {
			reader.loadBeanDefinitions(new InputSource(inputStream));
		}
		finally {
			inputStream.close();
		}

		// Let's create the outer bean named "innerBean",
		// to check whether it doesn't create any conflicts
		// with the actual inner beans named "innerBean".
		xbf.getBean("innerBean");

		TestBean hasInnerBeans = (TestBean) xbf.getBean("hasInnerBeans");
		assertEquals(5, hasInnerBeans.getAge());
		TestBean inner1 = (TestBean) hasInnerBeans.getSpouse();
		assertNotNull(inner1);
		assertThat(inner1.getBeanName()).isEqualTo("innerBean#1");
		assertThat(inner1.getName()).isEqualTo("inner1");
		assertEquals(6, inner1.getAge());

		assertNotNull(hasInnerBeans.getFriends());
		Object[] friends = hasInnerBeans.getFriends().toArray();
		assertEquals(3, friends.length);
		DerivedTestBean inner2 = (DerivedTestBean) friends[0];
		assertThat(inner2.getName()).isEqualTo("inner2");
		assertThat(inner2.getBeanName().startsWith(DerivedTestBean.class.getName())).isTrue();
		assertThat(xbf.containsBean("innerBean#1")).isFalse();
		assertNotNull(inner2);
		assertEquals(7, inner2.getAge());
		TestBean innerFactory = (TestBean) friends[1];
		assertThat(innerFactory.getName()).isEqualTo(DummyFactory.SINGLETON_NAME);
		TestBean inner5 = (TestBean) friends[2];
		assertThat(inner5.getBeanName()).isEqualTo("innerBean#2");

		assertNotNull(hasInnerBeans.getSomeMap());
		assertEquals(2, hasInnerBeans.getSomeMap().size());
		TestBean inner3 = (TestBean) hasInnerBeans.getSomeMap().get("someKey");
		assertThat(inner3.getName()).isEqualTo("Jenny");
		assertEquals(30, inner3.getAge());
		TestBean inner4 = (TestBean) hasInnerBeans.getSomeMap().get("someOtherKey");
		assertThat(inner4.getName()).isEqualTo("inner4");
		assertEquals(9, inner4.getAge());

		TestBean hasInnerBeansForConstructor = (TestBean) xbf.getBean("hasInnerBeansForConstructor");
		TestBean innerForConstructor = (TestBean) hasInnerBeansForConstructor.getSpouse();
		assertNotNull(innerForConstructor);
		assertThat(innerForConstructor.getBeanName()).isEqualTo("innerBean#3");
		assertThat(innerForConstructor.getName()).isEqualTo("inner1");
		assertEquals(6, innerForConstructor.getAge());

		hasInnerBeansForConstructor = (TestBean) xbf.getBean("hasInnerBeansAsPrototype");
		innerForConstructor = (TestBean) hasInnerBeansForConstructor.getSpouse();
		assertNotNull(innerForConstructor);
		assertThat(innerForConstructor.getBeanName()).isEqualTo("innerBean");
		assertThat(innerForConstructor.getName()).isEqualTo("inner1");
		assertEquals(6, innerForConstructor.getAge());

		hasInnerBeansForConstructor = (TestBean) xbf.getBean("hasInnerBeansAsPrototype");
		innerForConstructor = (TestBean) hasInnerBeansForConstructor.getSpouse();
		assertNotNull(innerForConstructor);
		assertThat(innerForConstructor.getBeanName()).isEqualTo("innerBean");
		assertThat(innerForConstructor.getName()).isEqualTo("inner1");
		assertEquals(6, innerForConstructor.getAge());

		xbf.destroySingletons();
		assertThat(inner1.wasDestroyed()).isTrue();
		assertThat(inner2.wasDestroyed()).isTrue();
		assertThat(innerFactory.getName() == null).isTrue();
		assertThat(inner5.wasDestroyed()).isTrue();
	}

	@Test
	public void testInnerBeansWithoutDestroy() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);

		// Let's create the outer bean named "innerBean",
		// to check whether it doesn't create any conflicts
		// with the actual inner beans named "innerBean".
		xbf.getBean("innerBean");

		TestBean hasInnerBeans = (TestBean) xbf.getBean("hasInnerBeansWithoutDestroy");
		assertEquals(5, hasInnerBeans.getAge());
		TestBean inner1 = (TestBean) hasInnerBeans.getSpouse();
		assertNotNull(inner1);
		assertThat(inner1.getBeanName().startsWith("innerBean")).isTrue();
		assertThat(inner1.getName()).isEqualTo("inner1");
		assertEquals(6, inner1.getAge());

		assertNotNull(hasInnerBeans.getFriends());
		Object[] friends = hasInnerBeans.getFriends().toArray();
		assertEquals(3, friends.length);
		DerivedTestBean inner2 = (DerivedTestBean) friends[0];
		assertThat(inner2.getName()).isEqualTo("inner2");
		assertThat(inner2.getBeanName().startsWith(DerivedTestBean.class.getName())).isTrue();
		assertNotNull(inner2);
		assertEquals(7, inner2.getAge());
		TestBean innerFactory = (TestBean) friends[1];
		assertThat(innerFactory.getName()).isEqualTo(DummyFactory.SINGLETON_NAME);
		TestBean inner5 = (TestBean) friends[2];
		assertThat(inner5.getBeanName().startsWith("innerBean")).isTrue();
	}

	@Test
	public void testFailsOnInnerBean() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);

		try {
			xbf.getBean("failsOnInnerBean");
		}
		catch (BeanCreationException ex) {
			// Check whether message contains outer bean name.
			ex.printStackTrace();
			assertThat(ex.getMessage().contains("failsOnInnerBean")).isTrue();
			assertThat(ex.getMessage().contains("someMap")).isTrue();
		}

		try {
			xbf.getBean("failsOnInnerBeanForConstructor");
		}
		catch (BeanCreationException ex) {
			// Check whether message contains outer bean name.
			ex.printStackTrace();
			assertThat(ex.getMessage().contains("failsOnInnerBeanForConstructor")).isTrue();
			assertThat(ex.getMessage().contains("constructor argument")).isTrue();
		}
	}

	@Test
	public void testInheritanceFromParentFactoryPrototype() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		assertThat(child.getType("inheritsFromParentFactory")).isEqualTo(TestBean.class);
		TestBean inherits = (TestBean) child.getBean("inheritsFromParentFactory");
		// Name property value is overridden
		assertThat(inherits.getName().equals("override")).isTrue();
		// Age property is inherited from bean in parent factory
		assertThat(inherits.getAge() == 1).isTrue();
		TestBean inherits2 = (TestBean) child.getBean("inheritsFromParentFactory");
		assertThat(inherits2 == inherits).isFalse();
	}

	@Test
	public void testInheritanceWithDifferentClass() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		assertThat(child.getType("inheritsWithClass")).isEqualTo(DerivedTestBean.class);
		DerivedTestBean inherits = (DerivedTestBean) child.getBean("inheritsWithDifferentClass");
		// Name property value is overridden
		assertThat(inherits.getName().equals("override")).isTrue();
		// Age property is inherited from bean in parent factory
		assertThat(inherits.getAge() == 1).isTrue();
		assertThat(inherits.wasInitialized()).isTrue();
	}

	@Test
	public void testInheritanceWithClass() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		assertThat(child.getType("inheritsWithClass")).isEqualTo(DerivedTestBean.class);
		DerivedTestBean inherits = (DerivedTestBean) child.getBean("inheritsWithClass");
		// Name property value is overridden
		assertThat(inherits.getName().equals("override")).isTrue();
		// Age property is inherited from bean in parent factory
		assertThat(inherits.getAge() == 1).isTrue();
		assertThat(inherits.wasInitialized()).isTrue();
	}

	@Test
	public void testPrototypeInheritanceFromParentFactoryPrototype() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		assertThat(child.getType("prototypeInheritsFromParentFactoryPrototype")).isEqualTo(TestBean.class);
		TestBean inherits = (TestBean) child.getBean("prototypeInheritsFromParentFactoryPrototype");
		// Name property value is overridden
		assertThat(inherits.getName().equals("prototype-override")).isTrue();
		// Age property is inherited from bean in parent factory
		assertThat(inherits.getAge() == 2).isTrue();
		TestBean inherits2 = (TestBean) child.getBean("prototypeInheritsFromParentFactoryPrototype");
		assertThat(inherits2 == inherits).isFalse();
		inherits2.setAge(13);
		assertThat(inherits2.getAge() == 13).isTrue();
		// Shouldn't have changed first instance
		assertThat(inherits.getAge() == 2).isTrue();
	}

	@Test
	public void testPrototypeInheritanceFromParentFactorySingleton() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		TestBean inherits = (TestBean) child.getBean("protoypeInheritsFromParentFactorySingleton");
		// Name property value is overridden
		assertThat(inherits.getName().equals("prototypeOverridesInheritedSingleton")).isTrue();
		// Age property is inherited from bean in parent factory
		assertThat(inherits.getAge() == 1).isTrue();
		TestBean inherits2 = (TestBean) child.getBean("protoypeInheritsFromParentFactorySingleton");
		assertThat(inherits2 == inherits).isFalse();
		inherits2.setAge(13);
		assertThat(inherits2.getAge() == 13).isTrue();
		// Shouldn't have changed first instance
		assertThat(inherits.getAge() == 1).isTrue();
	}

	@Test
	public void testAutowireModeNotInherited() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

		TestBean david = (TestBean) xbf.getBean("magicDavid");
		// the parent bean is autowiring
		assertNotNull(david.getSpouse());

		TestBean derivedDavid = (TestBean) xbf.getBean("magicDavidDerived");
		// this fails while it inherits from the child bean
		assertNull("autowiring not propagated along child relationships", derivedDavid.getSpouse());
	}

	@Test
	public void testAbstractParentBeans() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		parent.preInstantiateSingletons();
		assertThat(parent.isSingleton("inheritedTestBeanWithoutClass")).isTrue();

		// abstract beans should not match
		Map<?, ?> tbs = parent.getBeansOfType(TestBean.class);
		assertEquals(2, tbs.size());
		assertThat(tbs.containsKey("inheritedTestBeanPrototype")).isTrue();
		assertThat(tbs.containsKey("inheritedTestBeanSingleton")).isTrue();

		// abstract bean should throw exception on creation attempt
		assertThatExceptionOfType(BeanIsAbstractException.class).isThrownBy(() ->
				parent.getBean("inheritedTestBeanWithoutClass"));

		// non-abstract bean should work, even if it serves as parent
		boolean condition = parent.getBean("inheritedTestBeanPrototype") instanceof TestBean;
		assertThat(condition).isTrue();
	}

	@Test
	public void testDependenciesMaterializeThis() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(DEP_MATERIALIZE_CONTEXT);

		assertEquals(2, xbf.getBeansOfType(DummyBo.class, true, false).size());
		assertEquals(3, xbf.getBeansOfType(DummyBo.class, true, true).size());
		assertEquals(3, xbf.getBeansOfType(DummyBo.class, true, false).size());
		assertEquals(3, xbf.getBeansOfType(DummyBo.class).size());
		assertEquals(2, xbf.getBeansOfType(DummyBoImpl.class, true, true).size());
		assertEquals(1, xbf.getBeansOfType(DummyBoImpl.class, false, true).size());
		assertEquals(2, xbf.getBeansOfType(DummyBoImpl.class).size());

		DummyBoImpl bos = (DummyBoImpl) xbf.getBean("boSingleton");
		DummyBoImpl bop = (DummyBoImpl) xbf.getBean("boPrototype");
		assertNotSame(bos, bop);
		assertThat(bos.dao == bop.dao).isTrue();
	}

	@Test
	public void testChildOverridesParentBean() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		TestBean inherits = (TestBean) child.getBean("inheritedTestBean");
		// Name property value is overridden
		assertThat(inherits.getName().equals("overrideParentBean")).isTrue();
		// Age property is inherited from bean in parent factory
		assertThat(inherits.getAge() == 1).isTrue();
		TestBean inherits2 = (TestBean) child.getBean("inheritedTestBean");
		assertThat(inherits2 != inherits).isTrue();
	}

	/**
	 * Check that a prototype can't inherit from a bogus parent.
	 * If a singleton does this the factory will fail to load.
	 */
	@Test
	public void testBogusParentageFromParentFactory() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				child.getBean("bogusParent", TestBean.class))
			.withMessageContaining("bogusParent")
			.withCauseInstanceOf(NoSuchBeanDefinitionException.class);
	}

	/**
	 * Note that prototype/singleton distinction is <b>not</b> inherited.
	 * It's possible for a subclass singleton not to return independent
	 * instances even if derived from a prototype
	 */
	@Test
	public void testSingletonInheritsFromParentFactoryPrototype() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		TestBean inherits = (TestBean) child.getBean("singletonInheritsFromParentFactoryPrototype");
		// Name property value is overridden
		assertThat(inherits.getName().equals("prototype-override")).isTrue();
		// Age property is inherited from bean in parent factory
		assertThat(inherits.getAge() == 2).isTrue();
		TestBean inherits2 = (TestBean) child.getBean("singletonInheritsFromParentFactoryPrototype");
		assertThat(inherits2 == inherits).isTrue();
	}

	@Test
	public void testSingletonFromParent() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		TestBean beanFromParent = (TestBean) parent.getBean("inheritedTestBeanSingleton");
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		TestBean beanFromChild = (TestBean) child.getBean("inheritedTestBeanSingleton");
		assertThat(beanFromParent == beanFromChild).as("singleton from parent and child is the same").isTrue();
	}

	@Test
	public void testNestedPropertyValue() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(parent).loadBeanDefinitions(PARENT_CONTEXT);
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(child).loadBeanDefinitions(CHILD_CONTEXT);
		IndexedTestBean bean = (IndexedTestBean) child.getBean("indexedTestBean");
		assertThat(bean.getArray()[0].getName()).as("name applied correctly").isEqualTo("myname");
	}

	@Test
	public void testCircularReferences() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		TestBean jenny = (TestBean) xbf.getBean("jenny");
		TestBean david = (TestBean) xbf.getBean("david");
		TestBean ego = (TestBean) xbf.getBean("ego");
		TestBean complexInnerEgo = (TestBean) xbf.getBean("complexInnerEgo");
		TestBean complexEgo = (TestBean) xbf.getBean("complexEgo");
		assertThat(jenny.getSpouse() == david).as("Correct circular reference").isTrue();
		assertThat(david.getSpouse() == jenny).as("Correct circular reference").isTrue();
		assertThat(ego.getSpouse() == ego).as("Correct circular reference").isTrue();
		assertThat(complexInnerEgo.getSpouse().getSpouse() == complexInnerEgo).as("Correct circular reference").isTrue();
		assertThat(complexEgo.getSpouse().getSpouse() == complexEgo).as("Correct circular reference").isTrue();
	}

	@Test
	public void testCircularReferenceWithFactoryBeanFirst() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		xbf.getBean("egoBridge");
		TestBean complexEgo = (TestBean) xbf.getBean("complexEgo");
		assertThat(complexEgo.getSpouse().getSpouse() == complexEgo).as("Correct circular reference").isTrue();
	}

	@Test
	public void testCircularReferenceWithTwoFactoryBeans() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		TestBean ego1 = (TestBean) xbf.getBean("ego1");
		assertThat(ego1.getSpouse().getSpouse() == ego1).as("Correct circular reference").isTrue();
		TestBean ego3 = (TestBean) xbf.getBean("ego3");
		assertThat(ego3.getSpouse().getSpouse() == ego3).as("Correct circular reference").isTrue();
	}

	@Test
	public void testCircularReferencesWithNotAllowed() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		xbf.setAllowCircularReferences(false);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("jenny"))
			.matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
	}

	@Test
	public void testCircularReferencesWithWrapping() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		xbf.addBeanPostProcessor(new WrappingPostProcessor());
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("jenny"))
			.matches(ex -> ex.contains(BeanCurrentlyInCreationException.class));
	}

	@Test
	public void testCircularReferencesWithWrappingAndRawInjectionAllowed() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		xbf.setAllowRawInjectionDespiteWrapping(true);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		xbf.addBeanPostProcessor(new WrappingPostProcessor());

		ITestBean jenny = (ITestBean) xbf.getBean("jenny");
		ITestBean david = (ITestBean) xbf.getBean("david");
		assertThat(AopUtils.isAopProxy(jenny)).isTrue();
		assertThat(AopUtils.isAopProxy(david)).isTrue();
		assertSame(david, jenny.getSpouse());
		assertNotSame(jenny, david.getSpouse());
		assertThat(david.getSpouse().getName()).isEqualTo("Jenny");
		assertSame(david, david.getSpouse().getSpouse());
		assertThat(AopUtils.isAopProxy(jenny.getSpouse())).isTrue();
		boolean condition = !AopUtils.isAopProxy(david.getSpouse());
		assertThat(condition).isTrue();
	}

	@Test
	public void testFactoryReferenceCircle() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(FACTORY_CIRCLE_CONTEXT);
		TestBean tb = (TestBean) xbf.getBean("singletonFactory");
		DummyFactory db = (DummyFactory) xbf.getBean("&singletonFactory");
		assertThat(tb == db.getOtherTestBean()).isTrue();
	}

	@Test
	public void testFactoryReferenceWithDoublePrefix() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(FACTORY_CIRCLE_CONTEXT);
		assertThat(xbf.getBean("&&singletonFactory")).isInstanceOf(DummyFactory.class);
	}

	@Test
	public void testComplexFactoryReferenceCircle() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(COMPLEX_FACTORY_CIRCLE_CONTEXT);
		xbf.getBean("proxy1");
		// check that unused instances from autowiring got removed
		assertEquals(4, xbf.getSingletonCount());
		// properly create the remaining two instances
		xbf.getBean("proxy2");
		assertEquals(5, xbf.getSingletonCount());
	}

	@Test
	public void noSuchFactoryBeanMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(NO_SUCH_FACTORY_METHOD_CONTEXT);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("defaultTestBean"));
	}

	@Test
	public void testInitMethodIsInvoked() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
		DoubleInitializer in = (DoubleInitializer) xbf.getBean("init-method1");
		// Initializer should have doubled value
		assertEquals(14, in.getNum());
	}

	/**
	 * Test that if a custom initializer throws an exception, it's handled correctly
	 */
	@Test
	public void testInitMethodThrowsException() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("init-method2"))
			.withCauseInstanceOf(IOException.class)
			.satisfies(ex -> {
				assertThat(ex.getResourceDescription()).contains("initializers.xml");
				assertThat(ex.getBeanName()).isEqualTo("init-method2");
			});
	}

	@Test
	public void testNoSuchInitMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
		assertThatExceptionOfType(FatalBeanException.class).isThrownBy(() ->
				xbf.getBean("init-method3"))
			.withMessageContaining("initializers.xml")
			.withMessageContaining("init-method3")
			.withMessageContaining("init");
	}

	/**
	 * Check that InitializingBean method is called first.
	 */
	@Test
	public void testInitializingBeanAndInitMethod() {
		InitAndIB.constructed = false;
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
		assertThat(InitAndIB.constructed).isFalse();
		xbf.preInstantiateSingletons();
		assertThat(InitAndIB.constructed).isFalse();
		InitAndIB iib = (InitAndIB) xbf.getBean("init-and-ib");
		assertThat(InitAndIB.constructed).isTrue();
		assertThat(iib.afterPropertiesSetInvoked && iib.initMethodInvoked).isTrue();
		boolean condition = !iib.destroyed && !iib.customDestroyed;
		assertThat(condition).isTrue();
		xbf.destroySingletons();
		assertThat(iib.destroyed && iib.customDestroyed).isTrue();
		xbf.destroySingletons();
		assertThat(iib.destroyed && iib.customDestroyed).isTrue();
	}

	/**
	 * Check that InitializingBean method is not called twice.
	 */
	@Test
	public void testInitializingBeanAndSameInitMethod() {
		InitAndIB.constructed = false;
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INITIALIZERS_CONTEXT);
		assertThat(InitAndIB.constructed).isFalse();
		xbf.preInstantiateSingletons();
		assertThat(InitAndIB.constructed).isFalse();
		InitAndIB iib = (InitAndIB) xbf.getBean("ib-same-init");
		assertThat(InitAndIB.constructed).isTrue();
		boolean condition3 = iib.afterPropertiesSetInvoked && !iib.initMethodInvoked;
		assertThat(condition3).isTrue();
		boolean condition2 = !iib.destroyed && !iib.customDestroyed;
		assertThat(condition2).isTrue();
		xbf.destroySingletons();
		boolean condition1 = iib.destroyed && !iib.customDestroyed;
		assertThat(condition1).isTrue();
		xbf.destroySingletons();
		boolean condition = iib.destroyed && !iib.customDestroyed;
		assertThat(condition).isTrue();
	}

	@Test
	public void testDefaultLazyInit() {
		InitAndIB.constructed = false;
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(DEFAULT_LAZY_CONTEXT);
		assertThat(InitAndIB.constructed).isFalse();
		xbf.preInstantiateSingletons();
		assertThat(InitAndIB.constructed).isTrue();
		try {
			xbf.getBean("lazy-and-bad");
		}
		catch (BeanCreationException ex) {
			boolean condition = ex.getCause() instanceof IOException;
			assertThat(condition).isTrue();
		}
	}

	@Test
	public void noSuchXmlFile() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(MISSING_CONTEXT));
	}

	@Test
	public void invalidXmlFile() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(INVALID_CONTEXT));
	}

	@Test
	public void testAutowire() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(AUTOWIRE_CONTEXT);
		TestBean spouse = new TestBean("kerry", 0);
		xbf.registerSingleton("spouse", spouse);
		doTestAutowire(xbf);
	}

	@Test
	public void testAutowireWithParent() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(AUTOWIRE_CONTEXT);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "kerry");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("spouse", bd);
		xbf.setParentBeanFactory(lbf);
		doTestAutowire(xbf);
	}

	private void doTestAutowire(DefaultListableBeanFactory xbf) {
		DependenciesBean rod1 = (DependenciesBean) xbf.getBean("rod1");
		TestBean kerry = (TestBean) xbf.getBean("spouse");
		// should have been autowired
		assertThat(rod1.getSpouse()).isEqualTo(kerry);

		DependenciesBean rod1a = (DependenciesBean) xbf.getBean("rod1a");
		// should have been autowired
		assertThat(rod1a.getSpouse()).isEqualTo(kerry);

		DependenciesBean rod2 = (DependenciesBean) xbf.getBean("rod2");
		// should have been autowired
		assertThat(rod2.getSpouse()).isEqualTo(kerry);

		DependenciesBean rod2a = (DependenciesBean) xbf.getBean("rod2a");
		// should have been set explicitly
		assertThat(rod2a.getSpouse()).isEqualTo(kerry);

		ConstructorDependenciesBean rod3 = (ConstructorDependenciesBean) xbf.getBean("rod3");
		IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
		// should have been autowired
		assertThat(rod3.getSpouse1()).isEqualTo(kerry);
		assertThat(rod3.getSpouse2()).isEqualTo(kerry);
		assertThat(rod3.getOther()).isEqualTo(other);

		ConstructorDependenciesBean rod3a = (ConstructorDependenciesBean) xbf.getBean("rod3a");
		// should have been autowired
		assertThat(rod3a.getSpouse1()).isEqualTo(kerry);
		assertThat(rod3a.getSpouse2()).isEqualTo(kerry);
		assertThat(rod3a.getOther()).isEqualTo(other);

		assertThatExceptionOfType(FatalBeanException.class).isThrownBy(() ->
				xbf.getBean("rod4", ConstructorDependenciesBean.class));

		DependenciesBean rod5 = (DependenciesBean) xbf.getBean("rod5");
		// Should not have been autowired
		assertNull(rod5.getSpouse());

		BeanFactory appCtx = (BeanFactory) xbf.getBean("childAppCtx");
		assertThat(appCtx.containsBean("rod1")).isTrue();
		assertThat(appCtx.containsBean("jenny")).isTrue();
	}

	@Test
	public void testAutowireWithDefault() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(DEFAULT_AUTOWIRE_CONTEXT);

		DependenciesBean rod1 = (DependenciesBean) xbf.getBean("rod1");
		// should have been autowired
		assertNotNull(rod1.getSpouse());
		assertThat(rod1.getSpouse().getName().equals("Kerry")).isTrue();

		DependenciesBean rod2 = (DependenciesBean) xbf.getBean("rod2");
		// should have been autowired
		assertNotNull(rod2.getSpouse());
		assertThat(rod2.getSpouse().getName().equals("Kerry")).isTrue();
	}

	@Test
	public void testAutowireByConstructor() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorDependenciesBean rod1 = (ConstructorDependenciesBean) xbf.getBean("rod1");
		TestBean kerry = (TestBean) xbf.getBean("kerry2");
		// should have been autowired
		assertThat(rod1.getSpouse1()).isEqualTo(kerry);
		assertEquals(0, rod1.getAge());
		assertThat(rod1.getName()).isEqualTo(null);

		ConstructorDependenciesBean rod2 = (ConstructorDependenciesBean) xbf.getBean("rod2");
		TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
		TestBean kerry2 = (TestBean) xbf.getBean("kerry2");
		// should have been autowired
		assertThat(rod2.getSpouse1()).isEqualTo(kerry2);
		assertThat(rod2.getSpouse2()).isEqualTo(kerry1);
		assertEquals(0, rod2.getAge());
		assertThat(rod2.getName()).isEqualTo(null);

		ConstructorDependenciesBean rod = (ConstructorDependenciesBean) xbf.getBean("rod3");
		IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
		// should have been autowired
		assertThat(rod.getSpouse1()).isEqualTo(kerry);
		assertThat(rod.getSpouse2()).isEqualTo(kerry);
		assertThat(rod.getOther()).isEqualTo(other);
		assertEquals(0, rod.getAge());
		assertThat(rod.getName()).isEqualTo(null);

		xbf.getBean("rod4", ConstructorDependenciesBean.class);
		// should have been autowired
		assertThat(rod.getSpouse1()).isEqualTo(kerry);
		assertThat(rod.getSpouse2()).isEqualTo(kerry);
		assertThat(rod.getOther()).isEqualTo(other);
		assertEquals(0, rod.getAge());
		assertThat(rod.getName()).isEqualTo(null);
	}

	@Test
	public void testAutowireByConstructorWithSimpleValues() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

		ConstructorDependenciesBean rod5 = (ConstructorDependenciesBean) xbf.getBean("rod5");
		TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
		TestBean kerry2 = (TestBean) xbf.getBean("kerry2");
		IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
		// should have been autowired
		assertThat(rod5.getSpouse1()).isEqualTo(kerry2);
		assertThat(rod5.getSpouse2()).isEqualTo(kerry1);
		assertThat(rod5.getOther()).isEqualTo(other);
		assertEquals(99, rod5.getAge());
		assertThat(rod5.getName()).isEqualTo("myname");

		DerivedConstructorDependenciesBean rod6 = (DerivedConstructorDependenciesBean) xbf.getBean("rod6");
		// should have been autowired
		assertThat(rod6.initialized).isTrue();
		boolean condition = !rod6.destroyed;
		assertThat(condition).isTrue();
		assertThat(rod6.getSpouse1()).isEqualTo(kerry2);
		assertThat(rod6.getSpouse2()).isEqualTo(kerry1);
		assertThat(rod6.getOther()).isEqualTo(other);
		assertEquals(0, rod6.getAge());
		assertThat(rod6.getName()).isEqualTo(null);

		xbf.destroySingletons();
		assertThat(rod6.destroyed).isTrue();
	}

	@Test
	public void testRelatedCausesFromConstructorResolution() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

		try {
			xbf.getBean("rod2Accessor");
		}
		catch (BeanCreationException ex) {
			assertThat(ex.toString().contains("touchy")).isTrue();
			ex.printStackTrace();
			assertNull(ex.getRelatedCauses());
		}
	}

	@Test
	public void testConstructorArgResolution() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
		TestBean kerry2 = (TestBean) xbf.getBean("kerry2");

		ConstructorDependenciesBean rod9 = (ConstructorDependenciesBean) xbf.getBean("rod9");
		assertEquals(99, rod9.getAge());
		ConstructorDependenciesBean rod9a = (ConstructorDependenciesBean) xbf.getBean("rod9", 98);
		assertEquals(98, rod9a.getAge());
		ConstructorDependenciesBean rod9b = (ConstructorDependenciesBean) xbf.getBean("rod9", "myName");
		assertThat(rod9b.getName()).isEqualTo("myName");
		ConstructorDependenciesBean rod9c = (ConstructorDependenciesBean) xbf.getBean("rod9", 97);
		assertEquals(97, rod9c.getAge());

		ConstructorDependenciesBean rod10 = (ConstructorDependenciesBean) xbf.getBean("rod10");
		assertThat(rod10.getName()).isEqualTo(null);

		ConstructorDependenciesBean rod11 = (ConstructorDependenciesBean) xbf.getBean("rod11");
		assertThat(rod11.getSpouse1()).isEqualTo(kerry2);

		ConstructorDependenciesBean rod12 = (ConstructorDependenciesBean) xbf.getBean("rod12");
		assertThat(rod12.getSpouse1()).isEqualTo(kerry1);
		assertNull(rod12.getSpouse2());

		ConstructorDependenciesBean rod13 = (ConstructorDependenciesBean) xbf.getBean("rod13");
		assertThat(rod13.getSpouse1()).isEqualTo(kerry1);
		assertThat(rod13.getSpouse2()).isEqualTo(kerry2);

		ConstructorDependenciesBean rod14 = (ConstructorDependenciesBean) xbf.getBean("rod14");
		assertThat(rod14.getSpouse1()).isEqualTo(kerry1);
		assertThat(rod14.getSpouse2()).isEqualTo(kerry2);

		ConstructorDependenciesBean rod15 = (ConstructorDependenciesBean) xbf.getBean("rod15");
		assertThat(rod15.getSpouse1()).isEqualTo(kerry2);
		assertThat(rod15.getSpouse2()).isEqualTo(kerry1);

		ConstructorDependenciesBean rod16 = (ConstructorDependenciesBean) xbf.getBean("rod16");
		assertThat(rod16.getSpouse1()).isEqualTo(kerry2);
		assertThat(rod16.getSpouse2()).isEqualTo(kerry1);
		assertEquals(29, rod16.getAge());

		ConstructorDependenciesBean rod17 = (ConstructorDependenciesBean) xbf.getBean("rod17");
		assertThat(rod17.getSpouse1()).isEqualTo(kerry1);
		assertThat(rod17.getSpouse2()).isEqualTo(kerry2);
		assertEquals(29, rod17.getAge());
	}

	@Test
	public void testPrototypeWithExplicitArguments() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		SimpleConstructorArgBean cd1 = (SimpleConstructorArgBean) xbf.getBean("rod18");
		assertEquals(0, cd1.getAge());
		SimpleConstructorArgBean cd2 = (SimpleConstructorArgBean) xbf.getBean("rod18", 98);
		assertEquals(98, cd2.getAge());
		SimpleConstructorArgBean cd3 = (SimpleConstructorArgBean) xbf.getBean("rod18", "myName");
		assertThat(cd3.getName()).isEqualTo("myName");
		SimpleConstructorArgBean cd4 = (SimpleConstructorArgBean) xbf.getBean("rod18");
		assertEquals(0, cd4.getAge());
		SimpleConstructorArgBean cd5 = (SimpleConstructorArgBean) xbf.getBean("rod18", 97);
		assertEquals(97, cd5.getAge());
	}

	@Test
	public void testConstructorArgWithSingleMatch() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		File file = (File) xbf.getBean("file");
		assertThat(file.getPath()).isEqualTo((File.separator + "test"));
	}

	@Test
	public void throwsExceptionOnTooManyArguments() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("rod7", ConstructorDependenciesBean.class));
	}

	@Test
	public void throwsExceptionOnAmbiguousResolution() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				xbf.getBean("rod8", ConstructorDependenciesBean.class));
	}

	@Test
	public void testDependsOn() {
		doTestDependencies(DEP_DEPENDSON_CONTEXT, 1);
	}

	@Test
	public void testDependsOnInInnerBean() {
		doTestDependencies(DEP_DEPENDSON_INNER_CONTEXT, 4);
	}

	@Test
	public void testDependenciesThroughConstructorArguments() {
		doTestDependencies(DEP_CARG_CONTEXT, 1);
	}

	@Test
	public void testDependenciesThroughConstructorArgumentAutowiring() {
		doTestDependencies(DEP_CARG_AUTOWIRE_CONTEXT, 1);
	}

	@Test
	public void testDependenciesThroughConstructorArgumentsInInnerBean() {
		doTestDependencies(DEP_CARG_INNER_CONTEXT, 1);
	}

	@Test
	public void testDependenciesThroughProperties() {
		doTestDependencies(DEP_PROP, 1);
	}

	@Test
	public void testDependenciesThroughPropertiesWithInTheMiddle() {
		doTestDependencies(DEP_PROP_MIDDLE_CONTEXT, 1);
	}

	@Test
	public void testDependenciesThroughPropertyAutowiringByName() {
		doTestDependencies(DEP_PROP_ABN_CONTEXT, 1);
	}

	@Test
	public void testDependenciesThroughPropertyAutowiringByType() {
		doTestDependencies(DEP_PROP_ABT_CONTEXT, 1);
	}

	@Test
	public void testDependenciesThroughPropertiesInInnerBean() {
		doTestDependencies(DEP_PROP_INNER_CONTEXT, 1);
	}

	private void doTestDependencies(ClassPathResource resource, int nrOfHoldingBeans) {
		PreparingBean1.prepared = false;
		PreparingBean1.destroyed = false;
		PreparingBean2.prepared = false;
		PreparingBean2.destroyed = false;
		DependingBean.destroyCount = 0;
		HoldingBean.destroyCount = 0;
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(resource);
		xbf.preInstantiateSingletons();
		xbf.destroySingletons();
		assertThat(PreparingBean1.prepared).isTrue();
		assertThat(PreparingBean1.destroyed).isTrue();
		assertThat(PreparingBean2.prepared).isTrue();
		assertThat(PreparingBean2.destroyed).isTrue();
		assertEquals(nrOfHoldingBeans, DependingBean.destroyCount);
		if (!xbf.getBeansOfType(HoldingBean.class, false, false).isEmpty()) {
			assertEquals(nrOfHoldingBeans, HoldingBean.destroyCount);
		}
	}

	/**
	 * When using a BeanFactory. singletons are of course not pre-instantiated.
	 * So rubbish class names in bean defs must now not be 'resolved' when the
	 * bean def is being parsed, 'cos everything on a bean def is now lazy, but
	 * must rather only be picked up when the bean is instantiated.
	 */
	@Test
	public void testClassNotFoundWithDefaultBeanClassLoader() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(CLASS_NOT_FOUND_CONTEXT);
		// cool, no errors, so the rubbish class name in the bean def was not resolved
		// let's resolve the bean definition; must blow up
		assertThatExceptionOfType(CannotLoadBeanClassException.class).isThrownBy(() ->
				factory.getBean("classNotFound"))
			.withCauseInstanceOf(ClassNotFoundException.class)
			.satisfies(ex -> assertThat(ex.getResourceDescription()).contains("classNotFound.xml"));
	}

	@Test
	public void testClassNotFoundWithNoBeanClassLoader() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.setBeanClassLoader(null);
		reader.loadBeanDefinitions(CLASS_NOT_FOUND_CONTEXT);
		assertThat(bf.getBeanDefinition("classNotFound").getBeanClassName()).isEqualTo("WhatALotOfRubbish");
	}

	@Test
	public void testResourceAndInputStream() throws IOException {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(RESOURCE_CONTEXT);
		// comes from "resourceImport.xml"
		ResourceTestBean resource1 = (ResourceTestBean) xbf.getBean("resource1");
		// comes from "resource.xml"
		ResourceTestBean resource2 = (ResourceTestBean) xbf.getBean("resource2");

		boolean condition = resource1.getResource() instanceof ClassPathResource;
		assertThat(condition).isTrue();
		StringWriter writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getResource().getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("test");
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("test");
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getResource().getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("test");
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("test");
	}

	@Test
	public void testClassPathResourceWithImport() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(RESOURCE_CONTEXT);
		// comes from "resourceImport.xml"
		xbf.getBean("resource1", ResourceTestBean.class);
		// comes from "resource.xml"
		xbf.getBean("resource2", ResourceTestBean.class);
	}

	@Test
	public void testUrlResourceWithImport() {
		URL url = getClass().getResource(RESOURCE_CONTEXT.getPath());
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(new UrlResource(url));
		// comes from "resourceImport.xml"
		xbf.getBean("resource1", ResourceTestBean.class);
		// comes from "resource.xml"
		xbf.getBean("resource2", ResourceTestBean.class);
	}

	@Test
	public void testFileSystemResourceWithImport() throws URISyntaxException {
		String file = getClass().getResource(RESOURCE_CONTEXT.getPath()).toURI().getPath();
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(new FileSystemResource(file));
		// comes from "resourceImport.xml"
		xbf.getBean("resource1", ResourceTestBean.class);
		// comes from "resource.xml"
		xbf.getBean("resource2", ResourceTestBean.class);
	}

	@Test
	public void recursiveImport() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(RECURSIVE_IMPORT_CONTEXT));
	}

	/**
	 * @since 3.2.8 and 4.0.2
	 * @see <a href="https://jira.spring.io/browse/SPR-10785">SPR-10785</a> and <a
	 *      href="https://jira.spring.io/browse/SPR-11420">SPR-11420</a>
	 */
	@Test
	public void methodInjectedBeanMustBeOfSameEnhancedCglibSubclassTypeAcrossBeanFactories() {
		Class<?> firstClass = null;

		for (int i = 0; i < 10; i++) {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			new XmlBeanDefinitionReader(bf).loadBeanDefinitions(OVERRIDES_CONTEXT);

			final Class<?> currentClass = bf.getBean("overrideOneMethod").getClass();
			assertThat(ClassUtils.isCglibProxyClass(currentClass)).as("Method injected bean class [" + currentClass + "] must be a CGLIB enhanced subclass.").isTrue();

			if (firstClass == null) {
				firstClass = currentClass;
			}
			else {
				assertThat(currentClass).isEqualTo(firstClass);
			}
		}
	}

	@Test
	public void lookupOverrideMethodsWithSetterInjection() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

		lookupOverrideMethodsWithSetterInjection(xbf, "overrideOneMethod", true);
		// Should work identically on subclass definition, in which lookup
		// methods are inherited
		lookupOverrideMethodsWithSetterInjection(xbf, "overrideInheritedMethod", true);

		// Check cost of repeated construction of beans with method overrides
		// Will pick up misuse of CGLIB
		int howMany = 100;
		StopWatch sw = new StopWatch();
		sw.start("Look up " + howMany + " prototype bean instances with method overrides");
		for (int i = 0; i < howMany; i++) {
			lookupOverrideMethodsWithSetterInjection(xbf, "overrideOnPrototype", false);
		}
		sw.stop();
		// System.out.println(sw);
		if (!LogFactory.getLog(DefaultListableBeanFactory.class).isDebugEnabled()) {
			assertThat(sw.getTotalTimeMillis() < 2000).isTrue();
		}

		// Now test distinct bean with swapped value in factory, to ensure the two are independent
		OverrideOneMethod swappedOom = (OverrideOneMethod) xbf.getBean("overrideOneMethodSwappedReturnValues");

		TestBean tb = swappedOom.getPrototypeDependency();
		assertThat(tb.getName()).isEqualTo("David");
		tb = swappedOom.protectedOverrideSingleton();
		assertThat(tb.getName()).isEqualTo("Jenny");
	}

	private void lookupOverrideMethodsWithSetterInjection(BeanFactory xbf,
			String beanName, boolean singleton) {
		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean(beanName);

		if (singleton) {
			assertSame(oom, xbf.getBean(beanName));
		}
		else {
			assertNotSame(oom, xbf.getBean(beanName));
		}

		TestBean jenny1 = oom.getPrototypeDependency();
		assertThat(jenny1.getName()).isEqualTo("Jenny");
		TestBean jenny2 = oom.getPrototypeDependency();
		assertThat(jenny2.getName()).isEqualTo("Jenny");
		assertNotSame(jenny1, jenny2);

		// Check that the bean can invoke the overridden method on itself
		// This differs from Spring's AOP support, which has a distinct notion
		// of a "target" object, meaning that the target needs explicit knowledge
		// of AOP proxying to invoke an advised method on itself.
		TestBean jenny3 = oom.invokesOverriddenMethodOnSelf();
		assertThat(jenny3.getName()).isEqualTo("Jenny");
		assertNotSame(jenny1, jenny3);

		// Now try protected method, and singleton
		TestBean dave1 = oom.protectedOverrideSingleton();
		assertThat(dave1.getName()).isEqualTo("David");
		TestBean dave2 = oom.protectedOverrideSingleton();
		assertThat(dave2.getName()).isEqualTo("David");
		assertSame(dave1, dave2);
	}

	@Test
	public void testReplaceMethodOverrideWithSetterInjection() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);

		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethod");

		// Same contract as for overrides.xml
		TestBean jenny1 = oom.getPrototypeDependency();
		assertThat(jenny1.getName()).isEqualTo("Jenny");
		TestBean jenny2 = oom.getPrototypeDependency();
		assertThat(jenny2.getName()).isEqualTo("Jenny");
		assertNotSame(jenny1, jenny2);

		TestBean notJenny = oom.getPrototypeDependency("someParam");
		boolean condition = !"Jenny".equals(notJenny.getName());
		assertThat(condition).isTrue();

		// Now try protected method, and singleton
		TestBean dave1 = oom.protectedOverrideSingleton();
		assertThat(dave1.getName()).isEqualTo("David");
		TestBean dave2 = oom.protectedOverrideSingleton();
		assertThat(dave2.getName()).isEqualTo("David");
		assertSame(dave1, dave2);

		// Check unadvised behaviour
		String str = "woierowijeiowiej";
		assertThat(oom.echo(str)).isEqualTo(str);

		// Now test replace
		String s = "this is not a palindrome";
		String reverse = new StringBuffer(s).reverse().toString();
		assertThat(oom.replaceMe(s)).as("Should have overridden to reverse, not echo").isEqualTo(reverse);

		assertThat(oom.replaceMe()).as("Should have overridden no-arg overloaded replaceMe method to return fixed value").isEqualTo(FixedMethodReplacer.VALUE);

		OverrideOneMethodSubclass ooms = (OverrideOneMethodSubclass) xbf.getBean("replaceVoidMethod");
		DoSomethingReplacer dos = (DoSomethingReplacer) xbf.getBean("doSomethingReplacer");
		assertThat(dos.lastArg).isEqualTo(null);
		String s1 = "";
		String s2 = "foo bar black sheep";
		ooms.doSomething(s1);
		assertThat(dos.lastArg).isEqualTo(s1);
		ooms.doSomething(s2);
		assertThat(dos.lastArg).isEqualTo(s2);
	}

	@Test
	public void lookupOverrideOneMethodWithConstructorInjection() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(CONSTRUCTOR_OVERRIDES_CONTEXT);

		ConstructorInjectedOverrides cio = (ConstructorInjectedOverrides) xbf.getBean("constructorOverrides");

		// Check that the setter was invoked...
		// We should be able to combine Constructor and
		// Setter Injection
		assertThat(cio.getSetterString()).as("Setter string was set").isEqualTo("from property element");

		// Jenny is a singleton
		TestBean jenny = (TestBean) xbf.getBean("jenny");
		assertSame(jenny, cio.getTestBean());
		assertSame(jenny, cio.getTestBean());
		FactoryMethods fm1 = cio.createFactoryMethods();
		FactoryMethods fm2 = cio.createFactoryMethods();
		assertNotSame("FactoryMethods reference is to a prototype", fm1, fm2);
		assertSame("The two prototypes hold the same singleton reference",
				fm1.getTestBean(), fm2.getTestBean());
	}

	@Test
	public void testRejectsOverrideOfBogusMethodName() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(INVALID_NO_SUCH_METHOD_CONTEXT);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				xbf.getBean("constructorOverrides"))
			.withMessageContaining("bogusMethod");
	}

	@Test
	public void serializableMethodReplacerAndSuperclass() throws IOException {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
		SerializableMethodReplacerCandidate s = (SerializableMethodReplacerCandidate) xbf.getBean("serializableReplacer");
		String forwards = "this is forwards";
		String backwards = new StringBuffer(forwards).reverse().toString();
		assertThat(s.replaceMe(forwards)).isEqualTo(backwards);
		// SPR-356: lookup methods & method replacers are not serializable.
		assertThat(SerializationTestUtils.isSerializable(s)).as("Lookup methods and method replacers are not meant to be serializable.").isFalse();
	}

	@Test
	public void testInnerBeanInheritsScopeFromConcreteChildDefinition() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

		TestBean jenny1 = (TestBean) xbf.getBean("jennyChild");
		assertEquals(1, jenny1.getFriends().size());
		Object friend1 = jenny1.getFriends().iterator().next();
		boolean condition1 = friend1 instanceof TestBean;
		assertThat(condition1).isTrue();

		TestBean jenny2 = (TestBean) xbf.getBean("jennyChild");
		assertEquals(1, jenny2.getFriends().size());
		Object friend2 = jenny2.getFriends().iterator().next();
		boolean condition = friend2 instanceof TestBean;
		assertThat(condition).isTrue();

		assertNotSame(jenny1, jenny2);
		assertNotSame(friend1, friend2);
	}

	@Test
	public void testConstructorArgWithSingleSimpleTypeMatch() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

		SingleSimpleTypeConstructorBean bean = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBoolean");
		assertThat(bean.isSingleBoolean()).isTrue();

		SingleSimpleTypeConstructorBean bean2 = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBoolean2");
		assertThat(bean2.isSingleBoolean()).isTrue();
	}

	@Test
	public void testConstructorArgWithDoubleSimpleTypeMatch() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);

		SingleSimpleTypeConstructorBean bean = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBooleanAndString");
		assertThat(bean.isSecondBoolean()).isTrue();
		assertThat(bean.getTestString()).isEqualTo("A String");

		SingleSimpleTypeConstructorBean bean2 = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBooleanAndString2");
		assertThat(bean2.isSecondBoolean()).isTrue();
		assertThat(bean2.getTestString()).isEqualTo("A String");
	}

	@Test
	public void testDoubleBooleanAutowire() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		DoubleBooleanConstructorBean bean = (DoubleBooleanConstructorBean) xbf.getBean("beanWithDoubleBoolean");
		assertThat(bean.boolean1).isEqualTo(Boolean.TRUE);
		assertThat(bean.boolean2).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void testDoubleBooleanAutowireWithIndex() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		DoubleBooleanConstructorBean bean = (DoubleBooleanConstructorBean) xbf.getBean("beanWithDoubleBooleanAndIndex");
		assertThat(bean.boolean1).isEqualTo(Boolean.FALSE);
		assertThat(bean.boolean2).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void testLenientDependencyMatching() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		LenientDependencyTestBean bean = (LenientDependencyTestBean) xbf.getBean("lenientDependencyTestBean");
		boolean condition = bean.tb instanceof DerivedTestBean;
		assertThat(condition).isTrue();
	}

	@Test
	public void testLenientDependencyMatchingFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		LenientDependencyTestBean bean = (LenientDependencyTestBean) xbf.getBean("lenientDependencyTestBeanFactoryMethod");
		boolean condition = bean.tb instanceof DerivedTestBean;
		assertThat(condition).isTrue();
	}

	@Test
	public void testNonLenientDependencyMatching() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("lenientDependencyTestBean");
		bd.setLenientConstructorResolution(false);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("lenientDependencyTestBean"))
			.satisfies(ex -> assertThat(ex.getMostSpecificCause().getMessage()).contains("Ambiguous"));
	}

	@Test
	public void testNonLenientDependencyMatchingFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("lenientDependencyTestBeanFactoryMethod");
		bd.setLenientConstructorResolution(false);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("lenientDependencyTestBeanFactoryMethod"))
			.satisfies(ex -> assertThat(ex.getMostSpecificCause().getMessage()).contains("Ambiguous"));
	}

	@Test
	public void testJavaLangStringConstructor() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("string");
		bd.setLenientConstructorResolution(false);
		String str = (String) xbf.getBean("string");
		assertThat(str).isEqualTo("test");
	}

	@Test
	public void testCustomStringConstructor() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("stringConstructor");
		bd.setLenientConstructorResolution(false);
		StringConstructorTestBean tb = (StringConstructorTestBean) xbf.getBean("stringConstructor");
		assertThat(tb.name).isEqualTo("test");
	}

	@Test
	public void testPrimitiveConstructorArray() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArray");
		boolean condition = bean.array instanceof int[];
		assertThat(condition).isTrue();
		assertEquals(1, ((int[]) bean.array).length);
		assertEquals(1, ((int[]) bean.array)[0]);
	}

	@Test
	public void testIndexedPrimitiveConstructorArray() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("indexedConstructorArray");
		boolean condition = bean.array instanceof int[];
		assertThat(condition).isTrue();
		assertEquals(1, ((int[]) bean.array).length);
		assertEquals(1, ((int[]) bean.array)[0]);
	}

	@Test
	public void testStringConstructorArrayNoType() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArrayNoType");
		boolean condition = bean.array instanceof String[];
		assertThat(condition).isTrue();
		assertEquals(0, ((String[]) bean.array).length);
	}

	@Test
	public void testStringConstructorArrayNoTypeNonLenient() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("constructorArrayNoType");
		bd.setLenientConstructorResolution(false);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArrayNoType");
		boolean condition = bean.array instanceof String[];
		assertThat(condition).isTrue();
		assertEquals(0, ((String[]) bean.array).length);
	}

	@Test
	public void testConstructorWithUnresolvableParameterName() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONSTRUCTOR_ARG_CONTEXT);
		AtomicInteger bean = (AtomicInteger) xbf.getBean("constructorUnresolvableName");
		assertEquals(1, bean.get());
		bean = (AtomicInteger) xbf.getBean("constructorUnresolvableNameWithIndex");
		assertEquals(1, bean.get());
	}

	@Test
	public void testWithDuplicateName() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(TEST_WITH_DUP_NAMES_CONTEXT))
			.withMessageContaining("Bean name 'foo'");
	}

	@Test
	public void testWithDuplicateNameInAlias() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(TEST_WITH_DUP_NAME_IN_ALIAS_CONTEXT))
			.withMessageContaining("Bean name 'foo'");
	}

	@Test
	public void testOverrideMethodByArgTypeAttribute() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethodByAttribute");
		assertThat(oom.replaceMe(1)).as("should not replace").isEqualTo("replaceMe:1");
		assertThat(oom.replaceMe("abc")).as("should replace").isEqualTo("cba");
	}

	@Test
	public void testOverrideMethodByArgTypeElement() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethodByElement");
		assertThat(oom.replaceMe(1)).as("should not replace").isEqualTo("replaceMe:1");
		assertThat(oom.replaceMe("abc")).as("should replace").isEqualTo("cba");
	}

	public static class DoSomethingReplacer implements MethodReplacer {

		public Object lastArg;

		@Override
		public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
			assertEquals(1, args.length);
			assertThat(method.getName()).isEqualTo("doSomething");
			lastArg = args[0];
			return null;
		}
	}


	public static class BadInitializer {

		/** Init method */
		public void init2() throws IOException {
			throw new IOException();
		}
	}


	public static class DoubleInitializer {

		private int num;

		public int getNum() {
			return num;
		}

		public void setNum(int i) {
			num = i;
		}

		/** Init method */
		public void init() {
			this.num *= 2;
		}
	}


	public static class InitAndIB implements InitializingBean, DisposableBean {

		public static boolean constructed;

		public boolean afterPropertiesSetInvoked, initMethodInvoked, destroyed, customDestroyed;

		public InitAndIB() {
			constructed = true;
		}

		@Override
		public void afterPropertiesSet() {
			assertThat(this.initMethodInvoked).isFalse();
			if (this.afterPropertiesSetInvoked) {
				throw new IllegalStateException("Already initialized");
			}
			this.afterPropertiesSetInvoked = true;
		}

		/** Init method */
		public void customInit() throws IOException {
			assertThat(this.afterPropertiesSetInvoked).isTrue();
			if (this.initMethodInvoked) {
				throw new IllegalStateException("Already customInitialized");
			}
			this.initMethodInvoked = true;
		}

		@Override
		public void destroy() {
			assertThat(this.customDestroyed).isFalse();
			if (this.destroyed) {
				throw new IllegalStateException("Already destroyed");
			}
			this.destroyed = true;
		}

		public void customDestroy() {
			assertThat(this.destroyed).isTrue();
			if (this.customDestroyed) {
				throw new IllegalStateException("Already customDestroyed");
			}
			this.customDestroyed = true;
		}
	}


	public static class PreparingBean1 implements DisposableBean {

		public static boolean prepared = false;

		public static boolean destroyed = false;

		public PreparingBean1() {
			prepared = true;
		}

		@Override
		public void destroy() {
			destroyed = true;
		}
	}


	public static class PreparingBean2 implements DisposableBean {

		public static boolean prepared = false;

		public static boolean destroyed = false;

		public PreparingBean2() {
			prepared = true;
		}

		@Override
		public void destroy() {
			destroyed = true;
		}
	}


	public static class DependingBean implements InitializingBean, DisposableBean {

		public static int destroyCount = 0;

		public boolean destroyed = false;

		public DependingBean() {
		}

		public DependingBean(PreparingBean1 bean1, PreparingBean2 bean2) {
		}

		public void setBean1(PreparingBean1 bean1) {
		}

		public void setBean2(PreparingBean2 bean2) {
		}

		public void setInTheMiddleBean(InTheMiddleBean bean) {
		}

		@Override
		public void afterPropertiesSet() {
			if (!(PreparingBean1.prepared && PreparingBean2.prepared)) {
				throw new IllegalStateException("Need prepared PreparingBeans!");
			}
		}

		@Override
		public void destroy() {
			if (PreparingBean1.destroyed || PreparingBean2.destroyed) {
				throw new IllegalStateException("Should not be destroyed after PreparingBeans");
			}
			destroyed = true;
			destroyCount++;
		}
	}


	public static class InTheMiddleBean {

		public void setBean1(PreparingBean1 bean1) {
		}

		public void setBean2(PreparingBean2 bean2) {
		}
	}


	public static class HoldingBean implements DisposableBean {

		public static int destroyCount = 0;

		private DependingBean dependingBean;

		public boolean destroyed = false;

		public void setDependingBean(DependingBean dependingBean) {
			this.dependingBean = dependingBean;
		}

		@Override
		public void destroy() {
			if (this.dependingBean.destroyed) {
				throw new IllegalStateException("Should not be destroyed after DependingBean");
			}
			this.destroyed = true;
			destroyCount++;
		}
	}


	public static class DoubleBooleanConstructorBean {

		private Boolean boolean1;
		private Boolean boolean2;

		public DoubleBooleanConstructorBean(Boolean b1, Boolean b2) {
			this.boolean1 = b1;
			this.boolean2 = b2;
		}

		public DoubleBooleanConstructorBean(String s1, String s2) {
			throw new IllegalStateException("Don't pick this constructor");
		}

		public static DoubleBooleanConstructorBean create(Boolean b1, Boolean b2) {
			return new DoubleBooleanConstructorBean(b1, b2);
		}

		public static DoubleBooleanConstructorBean create(String s1, String s2) {
			return new DoubleBooleanConstructorBean(s1, s2);
		}
	}


	public static class LenientDependencyTestBean {

		public final ITestBean tb;

		public LenientDependencyTestBean(ITestBean tb) {
			this.tb = tb;
		}

		public LenientDependencyTestBean(TestBean tb) {
			this.tb = tb;
		}

		public LenientDependencyTestBean(DerivedTestBean tb) {
			this.tb = tb;
		}

		@SuppressWarnings("rawtypes")
		public LenientDependencyTestBean(Map[] m) {
			throw new IllegalStateException("Don't pick this constructor");
		}

		public static LenientDependencyTestBean create(ITestBean tb) {
			return new LenientDependencyTestBean(tb);
		}

		public static LenientDependencyTestBean create(TestBean tb) {
			return new LenientDependencyTestBean(tb);
		}

		public static LenientDependencyTestBean create(DerivedTestBean tb) {
			return new LenientDependencyTestBean(tb);
		}
	}


	public static class ConstructorArrayTestBean {

		public final Object array;

		public ConstructorArrayTestBean(int[] array) {
			this.array = array;
		}

		public ConstructorArrayTestBean(float[] array) {
			this.array = array;
		}

		public ConstructorArrayTestBean(short[] array) {
			this.array = array;
		}

		public ConstructorArrayTestBean(String[] array) {
			this.array = array;
		}
	}


	public static class StringConstructorTestBean {

		public final String name;

		public StringConstructorTestBean(String name) {
			this.name = name;
		}
	}


	public static class WrappingPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			ProxyFactory pf = new ProxyFactory(bean);
			return pf.getProxy();
		}
	}

}
