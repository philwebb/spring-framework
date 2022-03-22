package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MyService {

	private final MyRepository repository;

	private final MyMetrics metrics;

	private final String value;

	public MyService(MyRepository repository, MyMetrics metrics, @Value("{test}") String value) {
		this.repository = repository;
		this.metrics = metrics;
		this.value = value;
	}

	@Override
	public String toString() {
		return "MyService using " + this.repository + " and " + this.metrics + " with " + this.value;
	}

}
