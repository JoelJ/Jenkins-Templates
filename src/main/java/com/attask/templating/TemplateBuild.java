package com.attask.templating;

import hudson.model.Build;
import hudson.model.Node;
import hudson.slaves.WorkspaceList;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * User: joeljohnson
 * Date: 3/13/12
 * Time: 12:13 PM
 */
public class TemplateBuild extends Build<TemplateProject,TemplateBuild> {
	public TemplateBuild(TemplateProject project) throws IOException {
		super(project);
	}

	public TemplateBuild(TemplateProject project, File buildDir) throws IOException {
		super(project, buildDir);
	}

	@Override
	public void run() {
		//noinspection unchecked
		run(new RunnerImpl());
	}

	protected class RunnerImpl extends Build<TemplateProject,TemplateBuild>.RunnerImpl {
		@Override
		protected WorkspaceList.Lease decideWorkspace(Node n, WorkspaceList wsl) throws IOException, InterruptedException {
			String customWorkspace = getProject().getCustomWorkspace();
			if (customWorkspace != null) {
				// we allow custom workspaces to be concurrently used between jobs.
				return WorkspaceList.Lease.createDummyLease(n.getRootPath().child(getEnvironment(listener).expand(customWorkspace)));
			}
			return super.decideWorkspace(n,wsl);
		}
	}
}
