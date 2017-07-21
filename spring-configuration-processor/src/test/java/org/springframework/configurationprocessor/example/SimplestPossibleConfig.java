package org.springframework.configurationprocessor.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimplestPossibleConfig {

	@Bean
	public String stringBean() {
		return "foo";
	}
}
