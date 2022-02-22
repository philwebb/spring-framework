package org.springframework.build.shadow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.util.PatternSet;

import com.github.jengelman.gradle.plugins.shadow.ShadowStats;
import com.github.jengelman.gradle.plugins.shadow.internal.DefaultDependencyFilter;
import com.github.jengelman.gradle.plugins.shadow.internal.DefaultZipCompressor;
import com.github.jengelman.gradle.plugins.shadow.internal.DependencyFilter;
import com.github.jengelman.gradle.plugins.shadow.internal.GradleVersionUtil;
import com.github.jengelman.gradle.plugins.shadow.internal.ZipCompressor;
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator;
import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator;
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowCopyAction;

import shadow.org.apache.tools.zip.ZipOutputStream;

public class ShadowSource extends Zip {

	private final List<Relocator> relocators = new ArrayList<>();

	private final ShadowStats shadowStats = new ShadowStats();

	private final GradleVersionUtil versionUtil;

	private transient List<Configuration> configurations;

	private transient DependencyFilter dependencyFilter;

	private final ConfigurableFileCollection includedDependencies = getProject()
			.files((Callable<FileCollection>) () -> this.dependencyFilter.resolve(this.configurations));

	public ShadowSource() {
		this.versionUtil = new GradleVersionUtil(getProject().getGradle().getGradleVersion());
		this.dependencyFilter = new DefaultDependencyFilter(getProject());
	}

	public void relocate(String pattern, String destination) {
		SimpleRelocator relocator = new SimpleRelocator(pattern, destination, new ArrayList<String>(),
				new ArrayList<String>());
		this.relocators.add(relocator);
	}

	@TaskAction
	@Override
	protected void copy() {
		from(getIncludedDependencies());
		super.copy();
		getLogger().info(this.shadowStats.toString());
	}

	@Classpath
	public FileCollection getIncludedDependencies() {
		return this.includedDependencies;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected CopyAction createCopyAction() {
		File zipFile = getArchiveFile().get().getAsFile();
		ZipCompressor compressor = new DefaultZipCompressor(isZip64(), ZipOutputStream.DEFLATED);
		DocumentationRegistry documentationRegistry = getServices().get(DocumentationRegistry.class);
		PatternSet patternSet = this.versionUtil.getRootPatternSet(getMainSpec());
		return new ShadowCopyAction(zipFile, compressor, documentationRegistry, getMetadataCharset(),
				Collections.emptyList(), this.relocators, patternSet, this.shadowStats, this.versionUtil,
				isPreserveFileTimestamps(), false, null);
	}

	@Classpath
	@Optional
	public List<Configuration> getConfigurations() {
		return this.configurations;
	}

	public void setConfigurations(List<Configuration> configurations) {
		this.configurations = configurations;
	}

}
