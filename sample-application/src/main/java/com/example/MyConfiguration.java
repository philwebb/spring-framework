package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ MyApplication.class, MyService.class, MyRepository.class, MyMetrics.class, MyNaughtyComponent.class })
public class MyConfiguration {

	@Bean
	public MyBean myBean() {
		return new MyBean();
	}

}
