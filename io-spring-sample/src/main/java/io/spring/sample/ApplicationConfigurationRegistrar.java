
package io.spring.sample;

import org.springframework.beans.factory.function.FunctionalBeanDefinition;
import org.springframework.beans.factory.function.FunctionalBeanRegistrar;
import org.springframework.beans.factory.function.FunctionalBeanRegistry;
import org.springframework.beans.factory.function.InjectionContext;
import org.springframework.util.function.InstanceSupplier;

/**
 * {@link BeanRegistrar} to adapt {@link ApplicationConfiguration}.
 */
class ApplicationConfigurationRegistrar implements FunctionalBeanRegistrar {

	@Override
	public void apply(FunctionalBeanRegistry registry) {
		registry.register(this::applicationConfigurationRegistration);
		registry.register(this::printerRegistration);
		registry.register(this::greeterRegistration);
	}

	private void applicationConfigurationRegistration(
			FunctionalBeanDefinition.Builder<ApplicationConfiguration> registration) {
		registration.setType(ApplicationConfiguration.class);
		registration.setInstanceSupplier(InstanceSupplier.of(ApplicationConfiguration::new));
	}

	private void printerRegistration(FunctionalBeanDefinition.Builder<Printer> registration) {
		registration.setType(Printer.class);
		registration.setInstanceSupplier(this::printer);
	}

	private Printer printer(InjectionContext injectionContext) {
		return injectionContext.getBean(ApplicationConfiguration.class).printer();
	}

	private void greeterRegistration(FunctionalBeanDefinition.Builder<Greeter> registration) {
		registration.setType(Greeter.class);
		registration.setInstanceSupplier(this::greeter);
	}

	private Greeter greeter(InjectionContext injectionContext) {
		Printer printer = injectionContext.getBean(Printer.class);
		return injectionContext.getBean(ApplicationConfiguration.class).greeter(printer);
	}

}
