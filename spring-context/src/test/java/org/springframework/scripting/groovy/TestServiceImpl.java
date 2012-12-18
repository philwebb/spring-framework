package org.springframework.scripting.groovy;

@Log
@Log
public class TestServiceImpl implements TestService {

	public String sayHello() {
		throw new TestException("TestServiceImpl");
	}
}
