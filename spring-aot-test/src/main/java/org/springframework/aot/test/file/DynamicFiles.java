package org.springframework.aot.test.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * Internal class used by {@link SourceFiles} and {@link ResourceFiles} to manage
 * {@link DynamicFile} instances.
 *
 * @author Phillip Webb
 * @param <F> the {@link DynamicFile} type
 * @since 6.0
 */
class DynamicFiles<F extends DynamicFile> {

	private static final DynamicFiles<?> NONE = new DynamicFiles<>(Collections.emptyMap());

	private final Map<String, F> files;

	private DynamicFiles(Map<String, F> files) {
		this.files = files;
	}

	@SuppressWarnings("unchecked")
	static <F extends DynamicFile> DynamicFiles<F> none() {
		return (DynamicFiles<F>) NONE;
	}

	DynamicFiles<F> and(F[] files) {
		Map<String, F> merged = new LinkedHashMap<>(this.files);
		Arrays.stream(files).forEach(file -> merged.put(file.getPath(), file));
		return new DynamicFiles<>(Collections.unmodifiableMap(merged));
	}

	DynamicFiles<F> and(DynamicFiles<F> files) {
		Map<String, F> merged = new LinkedHashMap<>(this.files);
		merged.putAll(files.files);
		return new DynamicFiles<>(Collections.unmodifiableMap(merged));
	}

	@Nullable
	F get(String path) {
		return this.files.get(path);
	}

	Stream<F> stream() {
		return this.files.values().stream();
	}

}
