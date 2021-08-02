package io.spring.sample;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class ApplicationConfiguration {

	@Bean
	Printer printer() {
		return System.out::println;
	}

	@Bean
	Greeter greeter(Printer printer) {
		return new Greeter(printer);
	}

}
