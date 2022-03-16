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

package org.springframework.beans.factory.generator.config2;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 *
 * @author pwebb
 * @since 6.0
 */
public class BeanDefinitionRegistrar {

	/*
	 *

BeanDefinitionRegistrar.of(Foo.class).withConstructor(A.class).instanceSupplier((p)->new Foo(p.get(0));

	dunno(A.class, (params) -> new A(params.get(0));


	private MyBean getMyBean(ResolvedDependencies deps) {
		return new MyBean(deps.get(0), deps.get(1));
	}

	private MyBean instantiateMyBean(Object[] args) {
		return new MyBean((Foo) args[0]);
	}

	private MyBean invokeMyFactory(Object[] args) {
		return myFactory((Foo) args[0]);
	}



// setInstanceSupplier

	ListableBeanFactory, BeanDefinitionRegistry


	registry.register("foo", getFooDefinition(beanFactory));


	new Dunno(BeanFactory).forMethod(A.class, "foo", Foo.class).wiredBy(this::wireMyBean)

	RootBeanDefinin definition = new RootBeanDefinition();
	definition.setInstanceSupplier(InstanceSupplier.of(Foo.class).

	new InstanceSupplier(beanFactory).constructor().on(Foo.class).suppliedBy(this::getFooBean);


	new InstanceSupplier(beanFactory).usingConstructor(Foo.class).suppliedBy(this::getFooBean);

	new WiredInstance(beanFactory, this::getFooBean).forConstructor(Foo.class, String.class);

	beanFactory.injectionSupplier(this::getFooBean).



	bd.setInstanceSupplier(

	RootBeanDefinition.

	Dunn.forMethod(A.class, "foo", Foo.class).suppliedBy(this::invokeFoo);

RootBeanDefinition


	InjectedSupplier.usingFactoryMethod(Foo.class, String.class).resolvedBy(beanFactory, this::getFooBean);
	InjectedSupplier.usingConstructor(Foo.class, String.class).resolvedBy(beanFactory, this::getFooBean);

 	RootBeanDefinition bd = RootBeanDefinition
		.supply(MyBean.class)
		.usingConstructor(Foo.class, String.class)
		.resolvedBy(beanFactory, this::getFooBean);

 	RootBeanDefinition bd = RootBeanDefinition
		.supply("MyBean", MyBean.class)
		.usingConstructor(Foo.class, String.class)
		.resolvedBy(beanFactory, this::getFooBean);

	initialize(BDR reg, BF fac) {
		registerFoo(registry, beanFactory);
	}

	void registerFoo(registry, beanFactory) {
		String name = "foo";
		RootBeanDefinition definition = RootBeanDefinition.supply(name, Foo.class)
			.usingContructor(Foo.class)
			.resolvedBy(beanFactory, this::instantiateFoo);
		bd.setScope(...);
		registry.registerBeanDefinition(name, definition);
	}

	Foo instantiateFoo(Object[] parameters) {
		return new Foo((String) parameters[0]);
	}


	 *
	 *
	 *
	 */

}
