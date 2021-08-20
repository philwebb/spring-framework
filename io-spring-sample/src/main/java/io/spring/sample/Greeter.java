
package io.spring.sample;

class Greeter {

	private final Printer printer;

	Greeter(Printer printer) {
		this.printer = printer;
	}

	void greet() {
		this.printer.print("Hello World!");
	}

}
