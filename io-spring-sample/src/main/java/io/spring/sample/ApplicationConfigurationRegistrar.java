
package io.spring.sample;

import io.spring.bean.config.BeanInstanceSupplier;
import io.spring.bean.config.BeanRegistrar;
import io.spring.bean.config.BeanRegistration;
import io.spring.bean.config.BeanRegistry;
import io.spring.bean.config.BeanRepository;

/**
 * {@link BeanRegistrar} to adapt {@link ApplicationConfiguration}.
 */
class ApplicationConfigurationRegistrar implements BeanRegistrar {

	@Override
	public void apply(BeanRegistry registry) {
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
	}

	private Greeter greeter(BeanRepository repository) {
		ApplicationConfiguration applicationConfiguration = repository.get(ApplicationConfiguration.class);
		Printer printer = repository.get(Printer.class);
		return applicationConfiguration.greeter(printer);
	}

}
