package org.springframework.aot.test.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
class DynamicFiles<F extends DynamicFile> implements Iterable<F> {

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

	@Override
	public Iterator<F> iterator() {
		return this.files.values().iterator();
	}

	Stream<F> stream() {
		return this.files.values().stream();
	}

	F getSingle() {
		if (this.files.size() != 1) {
			throw new IllegalStateException("No single file available");
		}
		return this.files.values().iterator().next();
	}

}
