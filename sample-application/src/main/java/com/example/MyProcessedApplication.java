package com.example;

import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;

public class MyProcessedApplication {

	public static void main(String[] args) {
		GenericApplicationContext context = new GenericApplicationContext();
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
		beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
		List<DefaultListableBeanFactoryInitializer> beanFactoryInitializers = SpringFactoriesLoader
				.forNamedItem(BeanFactory.class, "default").load(DefaultListableBeanFactoryInitializer.class);
		beanFactoryInitializers.forEach(beanFactoryInitializer -> beanFactoryInitializer.initialize(beanFactory));
		context.refresh();
		context.getBean(MyApplication.class).run();
	}

}
