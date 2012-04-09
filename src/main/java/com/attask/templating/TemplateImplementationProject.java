package com.attask.templating;

import com.google.common.collect.ImmutableList;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;

/**
 * User: joeljohnson
 * Date: 3/13/12
 * Time: 12:12 PM
 */
public class TemplateImplementationProject extends Project<TemplateImplementationProject, TemplateImplementationBuild> implements TopLevelItem {
	@Extension(ordinal = 1000)
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	private String customWorkspace;

	public TemplateImplementationProject(ItemGroup parent, String name) {
		super(parent, name);
	}

	@Override
	protected Class<TemplateImplementationBuild> getBuildClass() {
		return TemplateImplementationBuild.class;
	}

	@Override
	public void save() throws IOException {
		super.save();
		ImplementTemplateBuildWrapper implementer = null;
		for (BuildWrapper buildWrapper : this.getBuildWrappers().values()) {
			if(buildWrapper != null && buildWrapper instanceof ImplementTemplateBuildWrapper) {
				implementer = (ImplementTemplateBuildWrapper) buildWrapper;
			}
		}

		if(implementer != null && implementer.getTemplateName() != null) {
			TemplateProject template = (TemplateProject) Hudson.getInstance().getItem(implementer.getTemplateName());
			implementer.updateImplementationWithTemplate(this, template);
		}
	}

	public void saveNoUpdate() throws IOException {
		super.save();
	}

	@Override
	protected void submit(StaplerRequest request, StaplerResponse response) throws IOException, ServletException, Descriptor.FormException {
		customWorkspace = request.hasParameter("customWorkspace") ? request.getParameter("customWorkspace.directory") : null;
		super.submit(request, response);
	}

	public String getCustomWorkspace() {
		return customWorkspace;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setCustomWorkspace(String customWorkspace) throws IOException {
		this.customWorkspace = customWorkspace;
		save();
	}

	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	public static final class DescriptorImpl extends AbstractProjectDescriptor {
		public String getDisplayName() {
			return "Template Implementation";
		}

		public TemplateImplementationProject newInstance(ItemGroup parent, String name) {
			return new TemplateImplementationProject(parent,name);
		}
	}

	public Collection<TemplateProject> getTemplates() {
		ImmutableList.Builder<TemplateProject> builder = ImmutableList.builder();

		Hudson instance = Hudson.getInstance();
		Collection<String> topLevelItemNames = instance.getTopLevelItemNames();
		for (String topLevelItemName : topLevelItemNames) {
			TopLevelItem topLevelItem = instance.getItem(topLevelItemName);
			if(topLevelItem instanceof TemplateProject) {
				builder.add((TemplateProject) topLevelItem);
			}
		}

		return builder.build();
	}
}
