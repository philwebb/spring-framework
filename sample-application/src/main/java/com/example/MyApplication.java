package com.example;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyApplication implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void run() {
		System.out.println("Hello World!");
		System.out.println(this.applicationContext.getBean(MyBean.class));
		System.out.println(this.applicationContext.getBean(MyService.class));
		System.out.println(this.applicationContext.getBean(MyNaughtyComponent.class));
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new AnnotationConfigApplicationContext(MyConfiguration.class).getBean(MyApplication.class).run();
	}

}
