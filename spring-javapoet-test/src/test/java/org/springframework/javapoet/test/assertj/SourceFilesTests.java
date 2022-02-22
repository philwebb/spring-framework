package org.springframework.javapoet.test.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

/**
 * Tests for {@link SourceFiles}.
 *
 * @author Phillip Webb
 */
class SourceFilesTests {

	@Test
	void testName() throws IOException {
		MethodSpec main = MethodSpec.methodBuilder("main").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(void.class).addParameter(String[].class, "args")
				.addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!").build();
		TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld").addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addMethod(main).build();
		JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld).build();
		System.err.println(javaFile.packageName);
		System.err.println(javaFile.toJavaFileObject().toUri().getPath());
		javaFile.writeTo(System.out);
	}

	@Test
	void testName2() {
		SourceFiles f = null;
		assertThat(f);
	}

}
