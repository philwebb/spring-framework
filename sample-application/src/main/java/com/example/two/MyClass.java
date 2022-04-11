package com.example.two;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

public class MyClass {

	public static void main(String[] args) {
		System.out.println(new AnnotationConfigApplicationContext(MyConfig.class).getBean(MyBean.class));
	}

	@Configuration
	@Import(MyBean.class)
	static class MyConfig {

	}

	static class MyBean {

		private MyBean() {
		}
	}

}
