package com.example;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AutowiredInstantiationArgumentsResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class SampleRegistration implements DefaultListableBeanFactoryInitializer {

	@Override
	public void initialize(DefaultListableBeanFactory beanFactory) {
		beanFactory.registerBeanDefinition("myBean", getMyBeanDefinition());
	}

	private BeanDefinition getMyBeanDefinition() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MyBean.class);
		InstanceSupplier<MyBean> instanceSupplier = InstanceSupplier.of(this::getMyBeanInstance);
		instanceSupplier = instanceSupplier.withPostProcessor(this::autowireMyBean);
		beanDefinition.setInstanceSupplier(instanceSupplier);
		beanDefinition.getPropertyValues().addPropertyValue("name", getMyOtherInnerBeanDefinition());
		return beanDefinition;
	}

	private BeanDefinition getMyOtherInnerBeanDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	private MyBean getMyBeanInstance(RegisteredBean registeredBean) {
		return AutowiredInstantiationArgumentsResolver.forConstructor(String.class)
				.resolveAndInstantiate(registeredBean);
	}

	private MyBean getMyBeanInstance2(RegisteredBean registeredBean) {
		return AutowiredInstantiationArgumentsResolver.forConstructor(String.class).resolve(registeredBean,
				(args) -> new MyBean(/* (String) args[0]*/));
	}

	private MyBean getMyBeanInstance3(RegisteredBean registeredBean) {
		return AutowiredInstantiationArgumentsResolver.forFactoryMethod(MyConfiguration.class, "myBean")
				.resolveAndInstantiate(registeredBean);
	}


	private MyBean autowireMyBean(RegisteredBean registeredBean, MyBean instance) {
		return instance;
	}

}
