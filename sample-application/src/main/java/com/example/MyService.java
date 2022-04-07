/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	public void example(String thing) {
	}

	@Override
	public String toString() {
		return "MyService using " + this.repository + " and " + this.metrics + " with " + this.value;
	}

}