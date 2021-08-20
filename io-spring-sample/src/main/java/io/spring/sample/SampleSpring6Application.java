
package io.spring.sample;

import org.springframework.context.function.DefaultFunctionalApplicationContext;

public class SampleSpring6Application {

	public static void main(String[] args) throws InterruptedException {
		DefaultFunctionalApplicationContext applicationContext = new DefaultFunctionalApplicationContext(
				new ApplicationConfigurationRegistrar());
		applicationContext.getBean(Greeter.class).greet();
	}

}
