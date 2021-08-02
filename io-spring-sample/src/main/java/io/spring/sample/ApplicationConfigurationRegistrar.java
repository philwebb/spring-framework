
package io.spring.sample;

import io.spring.bean.config.BeanInstanceSupplier;
import io.spring.bean.config.BeanRegistrar;
import io.spring.bean.config.BeanRegistration;
import io.spring.bean.config.BeanRegistry;
import io.spring.bean.config.BeanRepository;
import io.spring.scheduling.ScheduleRegistrar;
import io.spring.scheduling.ScheduleRegistration;

/**
 * {@link BeanRegistrar} to adapt {@link ApplicationConfiguration}.
 */
class ApplicationConfigurationRegistrar implements BeanRegistrar {

	@Override
	public void apply(BeanRegistry registry) {
		registry.registerFrom(SchedulingConfigurationRegistrar::new);
		registry.register(this::applicationConfigurationRegistration);
		registry.register(this::printerRegistration);
		registry.register(this::greeterRegistration);
	}

	private void applicationConfigurationRegistration(
			BeanRegistration.Builder<ApplicationConfiguration> registration) {
		registration.setType(ApplicationConfiguration.class);
		registration.setInstanceSupplier(
				BeanInstanceSupplier.via(ApplicationConfiguration::new));
	}

	private void printerRegistration(BeanRegistration.Builder<Printer> registration) {
		registration.setType(Printer.class);
		registration.setInstanceSupplier(BeanInstanceSupplier.via(
				ApplicationConfiguration.class, ApplicationConfiguration::printer));
	}

	private void greeterRegistration(BeanRegistration.Builder<Greeter> registration) {
		registration.setType(Greeter.class);
		registration.setInstanceSupplier(this::greeter);
		registration.onCreate(this::dunno);
	}

	private Greeter greeter(BeanRepository repository) {
		ApplicationConfiguration applicationConfiguration = repository.get(
				ApplicationConfiguration.class);
		Printer printer = repository.get(Printer.class);
		return applicationConfiguration.greeter(printer);
	}
	
	private void dunno(Greeter greeter) {
		
	}

	private ScheduleRegistrar scheduleRegistrar(Greeter greeter) {
		return (registry) -> {
			registry.register(ScheduleRegistration.of(greeter::greet));
		};
	}

}
