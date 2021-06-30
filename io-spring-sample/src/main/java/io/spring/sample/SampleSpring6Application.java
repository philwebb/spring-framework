
package io.spring.sample;

import io.spring.context.DefaultApplicationContext;

public class SampleSpring6Application {

	public static void main(String[] args) {
		DefaultApplicationContext applicationContext = new DefaultApplicationContext(
				new ApplicationConfigurationRegistrar());
		applicationContext.get(Greeter.class).greet();
	}

}
