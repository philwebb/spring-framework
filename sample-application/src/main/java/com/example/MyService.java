package com.example;

import org.springframework.stereotype.Service;

@Service
public class MyService {

	private final MyRepository repository;

	private final MyMetrics metrics;

	public MyService(MyRepository repository, MyMetrics metrics) {
		this.repository = repository;
		this.metrics = metrics;
	}

	@Override
	public String toString() {
		return "MyService using " + this.repository + " and " + this.metrics;
	}

}
