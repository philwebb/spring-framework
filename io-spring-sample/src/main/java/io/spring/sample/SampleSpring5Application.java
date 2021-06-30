
package io.spring.sample;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SampleSpring5Application {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ApplicationConfiguration.class);
		applicationContext.getBean(Greeter.class).greet();
	}

}
