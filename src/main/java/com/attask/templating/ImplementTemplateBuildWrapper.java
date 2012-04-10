package com.attask.templating;

import com.attask.utils.CollectionUtils;
import com.attask.utils.UnixUtils;
import com.google.common.collect.ImmutableMap;
import hudson.Extension;
import hudson.Launcher;
import hudson.XmlFile;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.*;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This is the build wrapper that needs to be enabled on a TemplateImplementationProject for the implementation effects to take place.
 * The BuildWrapper doesn't actually do anything when executed, but rather acts as a flag signifying which jobs need to be updated.
 * Also stores which template an implementation is implementing and what the values for the variables should be.
 * User: joeljohnson
 * Date: 3/14/12
 * Time: 4:13 PM
 */
public class ImplementTemplateBuildWrapper extends BuildWrapper {
	private String templateName;
	private String parameters; //TODO: change to a map and use a repeater in the jelly file for defining variables

	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	@DataBoundConstructor
	public ImplementTemplateBuildWrapper(String templateName, String parameters) {
		this.templateName = templateName;
		this.parameters = parameters;
	}

	/**
	 * Updates the the given implementation to implement the given template
	 * with the parameter values specified in this BuildWrapper.
	 * @param implementation The implementation Project to sync.
	 * @param template The Template to sync from.
	 * @throws IOException
	 * If the config XML file cannot be found, read, or written for either the template or implementation,
	 * an IOException will be thrown.
	 */
	public void updateImplementationWithTemplate(TemplateImplementationProject implementation, TemplateProject template) throws IOException {
		if(implementation == null) {
			return;
		}

		if(template == null) {
			return;
		}

		ImmutableMap.Builder<Pattern, String> patternPairsBuilder = ImmutableMap.builder();
		patternPairsBuilder.put(Pattern.compile(TemplateProject.class.getCanonicalName() + ">"), TemplateImplementationProject.class.getCanonicalName() + ">");
		for (Map.Entry<String, String> parameters : CollectionUtils.expandToMap(this.parameters).entrySet()) {
			patternPairsBuilder.put(Pattern.compile("\\$\\$" + parameters.getKey()), parameters.getValue());
		}

		XmlFile implementationXmlFile = implementation.getConfigFile();
		File implementationFile = implementationXmlFile.getFile();

		assert template.getConfigFile() != null : "template config file shouldn't be null";

		InputStream templateFileStream = new FileInputStream(template.getConfigFile().getFile());
		try {
			OutputStream outputStream = new FileOutputStream(implementationFile);
			try {
				UnixUtils.sed(templateFileStream, outputStream, patternPairsBuilder.build());
			} finally {
				outputStream.flush();
				outputStream.close();
			}
		} finally {
			templateFileStream.close();
		}

		implementation.getDescriptor().load();
		implementation = (TemplateImplementationProject) implementationXmlFile.unmarshal(implementation);
		implementation.getBuildWrappersList().add(this);
		implementation.saveNoUpdate();
	}

	/**
	 * Updates all the TemplateImplementationProject objects tracked by the
	 * given ItemGroup (which is typically the Hudson.getInstance() object)
	 * to be in sync with the given template.
	 * Will only sync the implementation projects, any other type of Project in the ItemGroup will be skipped.
	 * @param hudson ItemGroup that will contain all the projects that might be updated, typically Hudson.getInstance()
	 * @param template The template to sync all the implementations.
	 * @throws IOException
	 * If the config XML file cannot be found, read, or written for either the template or implementation,
	 * an IOException will be thrown.
	 */
	public static void updateImplementationsOfTemplate(ItemGroup hudson, TemplateProject template) throws IOException {
		for (Object o : hudson.getItems()) {
			if(o instanceof TopLevelItem) {
				TopLevelItem implementationProject = (TopLevelItem)o;
				if (implementationProject instanceof TemplateImplementationProject) {
					ImplementTemplateBuildWrapper implementer = null;
					for (BuildWrapper buildWrapper : ((TemplateImplementationProject) implementationProject).getBuildWrappers().values()) {
						if (buildWrapper != null && buildWrapper instanceof ImplementTemplateBuildWrapper) {
							ImplementTemplateBuildWrapper temp = (ImplementTemplateBuildWrapper) buildWrapper;
							if (template.getName().equals(temp.getTemplateName())) {
								implementer = temp;
								break;
							}
						}
					}

					if (implementer != null) {
						implementer.updateImplementationWithTemplate((TemplateImplementationProject) implementationProject, template);
					}
				}
			}
		}
	}

	/**
	 * Essentially a no-op.
	 */
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				return super.tearDown(build, listener);
			}
		};
	}

	/**
	 * @return The name of the template that is being implemented
	 */
	public String getTemplateName() {
		return templateName;
	}

	/**
	 * @param templateName The name of the template to be implemented
	 */
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	/**
	 * @return The parameters specified to be implemented. Formatted in a list of key/value pairs (the same as ANT property files).
	 */
	public String getParameters() {
		return parameters;
	}

	/**
	 * @param parameters The parameters specified to be implemented. Formatted in a list of key/value pairs (the same as ANT property files).
	 */
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		private final ItemGroup<TopLevelItem> hudson;

		public DescriptorImpl() {
			this.hudson = null;
		}

		/**
		 * Constructor for testing
		 * @param hudson
		 */
		public DescriptorImpl(ItemGroup<TopLevelItem> hudson) {
			this.hudson = hudson;
		}

		/**
		 * Verifies that the template name both exists and is a TemplateProject
		 * @param value The value to validate
		 * @return FormValidation.ok() if everything checks out.
		 * FormValidation.error(...) if the given value is neither a project or a template.
		 */
		public FormValidation doCheckTemplateName(@QueryParameter String value) {
			if (value == null || value.trim().isEmpty()) {
				return FormValidation.error("Template is a required field.");
			}

			ItemGroup<TopLevelItem> instance = getHudson();
			TopLevelItem topLevelItem = instance.getItem(value);
			if (topLevelItem == null) {
				return FormValidation.error("Project " + value + " does not exist.");
			}
			if (!(topLevelItem instanceof TemplateProject)) {
				return FormValidation.error("Project " + value + " is not a template.");
			}
			return FormValidation.ok();
		}
		
		private ItemGroup<TopLevelItem> getHudson() {
			if(hudson == null) {
				return Hudson.getInstance();
			}
			return hudson;
		}

		/**
		 * Only allow this BuildWrapper to be implemented on TemplateImplementationProject objects.
		 * @param item
		 * @return
		 */
		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return item instanceof TemplateImplementationProject;
		}

		@Override
		public String getDisplayName() {
			return "Implement Template";
		}
	}
}
