package io.spring.sample;

import io.spring.scheduling.ScheduleRegistrar;
import io.spring.scheduling.ScheduleRegistration;
import io.spring.scheduling.ScheduleRegistry;

public class GreeterScheduleRegistrar implements ScheduleRegistrar {

	private final Greeter greeter;

	public GreeterScheduleRegistrar(Greeter greeter) {
		this.greeter = greeter;
	}

	@Override
	public void apply(ScheduleRegistry registry) {
		registry.register(ScheduleRegistration.of(this.greeter::greet));
	}

}
