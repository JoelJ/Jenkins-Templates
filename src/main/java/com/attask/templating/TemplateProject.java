package com.attask.templating;

import hudson.Extension;
import hudson.model.*;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.*;

/**
 * Simply the same as a FreeStyleProject,
 * except that whenever it gets updated it syncs all the TemplateImplementation objects with the changes.
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

	/**
	 * Saves as normal then syncs all the child implementations with the changes. Replacing all the $$VARIABLES
	 * @throws IOException
	 * If the config.xml file of the template or implementation cannot be read or written to
	 * an appropriate IOException will be thrown.
	 */
	@Override
	public void save() throws IOException {
		super.save();

		ImplementTemplateBuildWrapper.updateImplementationsOfTemplate(Hudson.getInstance(), this);
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
