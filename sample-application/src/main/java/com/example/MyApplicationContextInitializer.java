package com.example;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

public class MyApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext applicationContext) {
		DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
		new MyRegistrations().registerBeanDefintions(beanFactory);

		// TODO Auto-generated method stub

	}

}
