
package io.spring.sample;

import java.util.function.BiConsumer;

import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import io.spring.bean.config.BeanRepository;
import io.spring.bean.config.BeanSelector;

/**
 * Alt for {@link ScheduledAnnotationBeanPostProcessor}.
 */
public class ScheduledAnnotationBeanPostProcessorAlt {

	private ScheduledTaskRegistrar registrar;

	private void finishRegistration(BeanRepository beanRepository) {
		beanRepository.select(SchedulingConfigurer.class).ordered().forEach(
				(configurer) -> configurer.configureTasks(this.registrar));

	}

}
