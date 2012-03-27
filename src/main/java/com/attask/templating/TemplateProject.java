package com.attask.templating;

import hudson.Extension;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.*;

/**
 * User: joeljohnson
 * Date: 3/13/12
 * Time: 12:12 PM
 */
public class TemplateProject extends Project<TemplateProject, TemplateBuild> implements TopLevelItem {
	@Extension(ordinal = 1000)
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	private String customWorkspace;

	public TemplateProject(ItemGroup parent, String name) {
		super(parent, name);
	}

	@Override
	protected Class<TemplateBuild> getBuildClass() {
		return TemplateBuild.class;
	}

	@Override
	public void save() throws IOException {
		super.save();

		ImplementTemplateBuildWrapper.updateImplementationsOfTemplate(this);
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
			return "Template Project";
		}

		public TemplateProject newInstance(ItemGroup parent, String name) {
			return new TemplateProject(parent,name);
		}
	}
}
