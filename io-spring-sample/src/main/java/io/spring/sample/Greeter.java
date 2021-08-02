
package io.spring.sample;

import org.springframework.scheduling.annotation.Scheduled;

class Greeter {

	private final Printer printer;

	Greeter(Printer printer) {
		this.printer = printer;
	}

	@Scheduled(initialDelay = 500, fixedRate = 200)
	void greet() {
		this.printer.print("Hello World!");
	}

}
