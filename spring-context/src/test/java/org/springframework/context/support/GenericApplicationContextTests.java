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

package org.springframework.context.support;

import org.junit.Test;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertArrayEquals;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertSame;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class GenericApplicationContextTests {

	@Test
	public void getBeanForClass() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		ac.refresh();

		assertThat(ac.getBean("testBean")).isEqualTo("");
		assertSame(ac.getBean("testBean"), ac.getBean(String.class));
		assertSame(ac.getBean("testBean"), ac.getBean(CharSequence.class));

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				ac.getBean(Object.class));
	}

	@Test
	public void withSingletonSupplier() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class, ac::toString));
		ac.refresh();

		assertSame(ac.getBean("testBean"), ac.getBean("testBean"));
		assertSame(ac.getBean("testBean"), ac.getBean(String.class));
		assertSame(ac.getBean("testBean"), ac.getBean(CharSequence.class));
		assertThat(ac.getBean("testBean")).isEqualTo(ac.toString());
	}

	@Test
	public void withScopedSupplier() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean",
				new RootBeanDefinition(String.class, RootBeanDefinition.SCOPE_PROTOTYPE, ac::toString));
		ac.refresh();

		assertNotSame(ac.getBean("testBean"), ac.getBean("testBean"));
		assertThat(ac.getBean(String.class)).isEqualTo(ac.getBean("testBean"));
		assertThat(ac.getBean(CharSequence.class)).isEqualTo(ac.getBean("testBean"));
		assertThat(ac.getBean("testBean")).isEqualTo(ac.toString());
	}

	@Test
	public void accessAfterClosing() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		ac.refresh();

		assertSame(ac.getBean("testBean"), ac.getBean(String.class));
		assertSame(ac.getAutowireCapableBeanFactory().getBean("testBean"),
				ac.getAutowireCapableBeanFactory().getBean(String.class));

		ac.close();

		assertThatIllegalStateException().isThrownBy(() ->
				ac.getBean(String.class));

		assertThatIllegalStateException().isThrownBy(() -> {
				ac.getAutowireCapableBeanFactory().getBean("testBean");
				ac.getAutowireCapableBeanFactory().getBean(String.class);
		});
	}

	@Test
	public void individualBeans() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(BeanA.class);
		context.registerBean(BeanB.class);
		context.registerBean(BeanC.class);
		context.refresh();

		assertSame(context.getBean(BeanB.class), context.getBean(BeanA.class).b);
		assertSame(context.getBean(BeanC.class), context.getBean(BeanA.class).c);
		assertSame(context, context.getBean(BeanB.class).applicationContext);
	}

	@Test
	public void individualNamedBeans() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("a", BeanA.class);
		context.registerBean("b", BeanB.class);
		context.registerBean("c", BeanC.class);
		context.refresh();

		assertSame(context.getBean("b"), context.getBean("a", BeanA.class).b);
		assertSame(context.getBean("c"), context.getBean("a", BeanA.class).c);
		assertSame(context, context.getBean("b", BeanB.class).applicationContext);
	}

	@Test
	public void individualBeanWithSupplier() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isTrue();
		assertSame(context.getBean(BeanB.class), context.getBean(BeanA.class).b);
		assertSame(context.getBean(BeanC.class), context.getBean(BeanA.class).c);
		assertSame(context, context.getBean(BeanB.class).applicationContext);

		assertThat((Object[]) context.getDefaultListableBeanFactory().getDependentBeans(BeanB.class.getName())).isEqualTo(new String[] {BeanA.class.getName()});
		assertThat((Object[]) context.getDefaultListableBeanFactory().getDependentBeans(BeanC.class.getName())).isEqualTo(new String[] {BeanA.class.getName()});
	}

	@Test
	public void individualBeanWithSupplierAndCustomizer() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
				bd -> bd.setLazyInit(true));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isFalse();
		assertSame(context.getBean(BeanB.class), context.getBean(BeanA.class).b);
		assertSame(context.getBean(BeanC.class), context.getBean(BeanA.class).c);
		assertSame(context, context.getBean(BeanB.class).applicationContext);
	}

	@Test
	public void individualNamedBeanWithSupplier() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("a", BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("a")).isTrue();
		assertSame(context.getBean("b", BeanB.class), context.getBean(BeanA.class).b);
		assertSame(context.getBean("c"), context.getBean("a", BeanA.class).c);
		assertSame(context, context.getBean("b", BeanB.class).applicationContext);
	}

	@Test
	public void individualNamedBeanWithSupplierAndCustomizer() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("a", BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
				bd -> bd.setLazyInit(true));
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("a")).isFalse();
		assertSame(context.getBean("b", BeanB.class), context.getBean(BeanA.class).b);
		assertSame(context.getBean("c"), context.getBean("a", BeanA.class).c);
		assertSame(context, context.getBean("b", BeanB.class).applicationContext);
	}

	@Test
	public void individualBeanWithNullReturningSupplier() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("a", BeanA.class, () -> null);
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanA.class), "a")).isTrue();
		assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanB.class), "b")).isTrue();
		assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanC.class), "c")).isTrue();
		assertThat(context.getBeansOfType(BeanA.class).isEmpty()).isTrue();
		assertSame(context.getBean(BeanB.class), context.getBeansOfType(BeanB.class).values().iterator().next());
		assertSame(context.getBean(BeanC.class), context.getBeansOfType(BeanC.class).values().iterator().next());
	}


	static class BeanA {

		BeanB b;
		BeanC c;

		public BeanA(BeanB b, BeanC c) {
			this.b = b;
			this.c = c;
		}
	}

	static class BeanB implements ApplicationContextAware  {

		ApplicationContext applicationContext;

		public BeanB() {
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}
	}

	static class BeanC {}

}
