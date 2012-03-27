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
 * User: joeljohnson
 * Date: 3/14/12
 * Time: 4:13 PM
 */
public class ImplementTemplateBuildWrapper extends BuildWrapper {
	public String templateName;
	public String parameters;

	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	@DataBoundConstructor
	public ImplementTemplateBuildWrapper(String templateName, String parameters) {
		this.templateName = templateName;
		this.parameters = parameters;
	}

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

	public static void updateImplementationsOfTemplate(TemplateProject template) throws IOException {
		for (String topLevelItemName : Hudson.getInstance().getTopLevelItemNames()) {
			TopLevelItem implementationProject = Hudson.getInstance().getItem(topLevelItemName);
			if (implementationProject instanceof TemplateImplementationProject) {
				ImplementTemplateBuildWrapper implementer = null;
				for (BuildWrapper buildWrapper : ((TemplateImplementationProject) implementationProject).getBuildWrappers().values()) {
					if (buildWrapper != null && buildWrapper instanceof ImplementTemplateBuildWrapper) {
						ImplementTemplateBuildWrapper temp = (ImplementTemplateBuildWrapper) buildWrapper;
						if (template.getName().equals(temp.templateName)) {
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

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				return super.tearDown(build, listener);
			}
		};
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		public FormValidation doCheckTemplateName(@QueryParameter String value) {
			if (value == null || value.trim().isEmpty()) {
				return FormValidation.error("Template is a required field.");
			}

			Hudson instance = Hudson.getInstance();
			TopLevelItem topLevelItem = instance.getItem(value);
			if (topLevelItem == null) {
				return FormValidation.error("Project " + value + " does not exist.");
			}
			if (!(topLevelItem instanceof TemplateProject)) {
				return FormValidation.error("Project " + value + " is not a template.");
			}
			return FormValidation.ok();
		}

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
