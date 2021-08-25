
package com.example.functional;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SampleSpring5Application {

	public static void main(String[] args) throws InterruptedException {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ApplicationConfiguration.class);
		applicationContext.getBean(Greeter.class).greet();
		applicationContext.close();
	}

}
