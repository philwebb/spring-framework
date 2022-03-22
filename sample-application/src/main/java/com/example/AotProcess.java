package com.example;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.aot.context.AotContribution;
import org.springframework.aot.context.AotProcessors;
import org.springframework.aot.context.DefaultAotContext;
import org.springframework.aot.context.TrackedAotProcessors;
import org.springframework.aot.context.TrackedAotProcessors.FileTracker;
import org.springframework.aot.generate.DefaultGeneratedSpringFactories;
import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.beans.factory.aot.AotBeanFactoryProcessor;
import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.aot.DefinedBeanExcludeFilter;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.aot.BeanRegistrationsAotBeanFactoryProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class AotProcess {

	private void run() throws Exception {
		DefaultListableBeanFactory beanFactory = getSourceBeanFactory();
		GeneratedFiles generatedFiles = getGeneratedFiles();
		FileTracker tracker = new FileTracker();
		Deque<AotContribution> contributions = new ArrayDeque<>();
		AotProcessors aotProcessors = new TrackedAotProcessors(contributions::add, tracker);
		DefaultAotContext aotContext = new DefaultAotContext(generatedFiles, aotProcessors);
		aotContext.getProcessors().add(new BeanRegistrationsAotBeanFactoryProcessor());
		aotContext.getProcessors().allOfType(AotBeanFactoryProcessor.class).processAndApplyContributions(new UniqueBeanFactoryName("default"), beanFactory);
		while(!contributions.isEmpty()) {
			AotContribution contribution = contributions.removeFirst();
			contribution.applyTo(aotContext);
		}
		DefaultGeneratedSpringFactories springFactories = (DefaultGeneratedSpringFactories) aotContext.getGeneratedSpringFactories();
		springFactories.writeTo(generatedFiles);
		tracker.save(generatedFiles);
	}

	private GeneratedFiles getGeneratedFiles() {
		Path root = new File(".").toPath().resolve("gen").normalize().toAbsolutePath();
		GeneratedFiles generatedFiles = new FileSystemGeneratedFiles(root);
		return generatedFiles;
	}

	@SuppressWarnings("resource")
	private DefaultListableBeanFactory getSourceBeanFactory() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(MyConfiguration.class);
		context.getBeanFactory().registerSingleton("excluder", new DefinedBeanExcludeFilter() {

			@Override
			public boolean isExcluded(DefinedBean definedBean) {
				return definedBean.getBeanName().equals("excluder") || definedBean.getBeanName().contains("internal");
			}

		});
		context.refreshForAotProcessing();
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
		return beanFactory;
	}

	public static void main(String[] args) throws Exception {
		new AotProcess().run();
	}

}
