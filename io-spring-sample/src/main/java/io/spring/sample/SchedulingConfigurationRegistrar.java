package io.spring.sample;

import org.springframework.scheduling.annotation.SchedulingConfiguration;

import io.spring.bean.config.BeanInstanceSupplier;
import io.spring.bean.config.BeanRegistrar;
import io.spring.bean.config.BeanRegistration;
import io.spring.bean.config.BeanRegistry;

/**
 * {@link BeanRegistrar} to adapt {@link SchedulingConfiguration}.
 */
public class SchedulingConfigurationRegistrar implements BeanRegistrar {

	@Override
	public void apply(BeanRegistry registry) {
		// @Configuration
		registry.register(this::schedulingConfigurationRegistration);
		// @Bean methods
		// ScheduledAnnotationBeanPostProcessor is not really suitable
	}

	private void schedulingConfigurationRegistration(
			BeanRegistration.Builder<SchedulingConfiguration> registration) {
		registration.setType(SchedulingConfiguration.class);
		registration.setInstanceSupplier(
				BeanInstanceSupplier.via(SchedulingConfiguration::new));
	}

}
