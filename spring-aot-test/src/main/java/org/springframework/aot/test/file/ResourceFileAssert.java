package org.springframework.aot.test.file;

/**
 * Assertion methods for {@code ResourceFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class ResourceFileAssert extends DynamicFileAssert<ResourceFileAssert, ResourceFile> {

	ResourceFileAssert(ResourceFile actual) {
		super(actual, ResourceFileAssert.class);
	}

}
