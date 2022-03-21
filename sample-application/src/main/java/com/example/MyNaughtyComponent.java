package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyNaughtyComponent {

	@Autowired
	private MyRepository myRepository;

	@Override
	public String toString() {
		return "MyNaughtyComponent using " + myRepository;
	}

}
