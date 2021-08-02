
package io.spring.sample;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SampleSpring5Application {

	public static void main(String[] args) throws InterruptedException {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ApplicationConfiguration.class);
		applicationContext.getBean(Greeter.class).greet();
		Thread.sleep(1500);
		applicationContext.close();
	}

}
