
package com.example.functional;

class Greeter {

	private final Printer printer;

	Greeter(Printer printer) {
		this.printer = printer;
	}

	void greet() {
		this.printer.print("Hello World!");
	}

}
