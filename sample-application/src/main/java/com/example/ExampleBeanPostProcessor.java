package com.example;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;

public class ExampleBeanPostProcessor extends AutowiredAnnotationBeanPostProcessor {

	public ExampleBeanPostProcessor() {
		super(MyNaughtyComponent.class, (preComputedAutowiredElements) -> {
			preComputedAutowiredElements.field("myRepository");
		});
	}

}
