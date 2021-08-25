
package com.example.functional;

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
		registry.register(this::applicationConfigurationDefinition);
		registry.register(this::printerDefinition);
		registry.register(this::greeterDefinition);
	}

	private void applicationConfigurationDefinition(
			FunctionalBeanDefinition.Builder<ApplicationConfiguration> registration) {
		registration.setName("applicationConfiguration");
		registration.setType(ApplicationConfiguration.class);
		registration.setInstanceSupplier(InstanceSupplier.of(ApplicationConfiguration::new));
	}

	private void printerDefinition(FunctionalBeanDefinition.Builder<Printer> builder) {
		builder.setName("printer");
		builder.setType(Printer.class);
		builder.setInstanceSupplier(this::printer);
	}

	private Printer printer(InjectionContext injectionContext) {
		return injectionContext.getBean(ApplicationConfiguration.class).printer();
	}

	private void greeterDefinition(FunctionalBeanDefinition.Builder<Greeter> builder) {
		builder.setName("greeter");
		builder.setType(Greeter.class);
		builder.setInstanceSupplier(this::greeter);
	}

	private Greeter greeter(InjectionContext injectionContext) {
		Printer printer = injectionContext.getBean(Printer.class);
		return injectionContext.getBean(ApplicationConfiguration.class).greeter(printer);
	}

}
