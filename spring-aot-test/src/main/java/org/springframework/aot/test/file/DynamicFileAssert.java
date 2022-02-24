package org.springframework.aot.test.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

/**
 * Assertion methods for {@code DynamicFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DynamicFileAssert<A extends DynamicFileAssert<A, F>, F extends DynamicFile> extends AbstractAssert<A, F> {

	DynamicFileAssert(F actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public A contains(CharSequence... values) {
		assertThat(this.actual.getContent()).contains(values);
		return this.myself;
	}

	public A isEqualTo(Object expected) {
		assertThat(this.actual.getContent()).isEqualTo(expected != null ? expected.toString() : null);
		return this.myself;
	}

}
