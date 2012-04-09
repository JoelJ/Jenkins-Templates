package com.attask.templating;

import com.google.common.collect.ImmutableMap;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.FormValidation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * User: joeljohnson
 * Date: 4/9/12
 * Time: 9:26 AM
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ImplementTemplateBuildWrapperTest {
	@Mock private ItemGroup<TopLevelItem> hudson;
	@Mock private TemplateProject template;
	@Mock private TemplateImplementationProject implementation_1;
	@Mock private TemplateImplementationProject implementation_2;
	@Mock private FreeStyleProject someProject;
	@Mock private ImplementTemplateBuildWrapper buildWrapper;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(hudson.getItems()).thenReturn(Arrays.<TopLevelItem>asList(
				implementation_1,
				implementation_2,
				someProject
		));

		when(hudson.getItem("Template Name")).thenReturn(template);
		when(hudson.getItem("implementation_1")).thenReturn(implementation_1);
		when(hudson.getItem("implementation_2")).thenReturn(implementation_2);
		when(hudson.getItem("someProject")).thenReturn(someProject);

		ImmutableMap.Builder<Descriptor<BuildWrapper>, BuildWrapper> builder = ImmutableMap.builder();
		builder.put(ImplementTemplateBuildWrapper.DESCRIPTOR, buildWrapper);
		Map<Descriptor<BuildWrapper>, BuildWrapper> buildWrappers = builder.build();
		when(implementation_1.getBuildWrappers()).thenReturn(buildWrappers);
		when(implementation_2.getBuildWrappers()).thenReturn(buildWrappers);

		when(template.getName()).thenReturn("Template Name");
		when(buildWrapper.getTemplateName()).thenReturn("Template Name");
	}

	@Test
	public void test_updateImplementationsOfTemplate_callsOnImplementation() throws IOException {
		ImplementTemplateBuildWrapper.updateImplementationsOfTemplate(hudson, template);
		verify(buildWrapper).updateImplementationWithTemplate(implementation_1, template);
		verify(buildWrapper).updateImplementationWithTemplate(implementation_2, template);
		verifyZeroInteractions(someProject);
	}

	@Test
	public void test_descriptorTemplateNameValidation_withTemplate() {
		ImplementTemplateBuildWrapper.DescriptorImpl descriptor = new ImplementTemplateBuildWrapper.DescriptorImpl(hudson);
		Assert.assertEquals(FormValidation.ok(), descriptor.doCheckTemplateName("Template Name"));
	}

	@Test
	public void test_descriptorTemplateNameValidation_withImplementation() {
		ImplementTemplateBuildWrapper.DescriptorImpl descriptor = new ImplementTemplateBuildWrapper.DescriptorImpl(hudson);
		FormValidation someProjectValidation = descriptor.doCheckTemplateName("implementation_1");
		Assert.assertEquals(FormValidation.error("").getClass(), someProjectValidation.getClass());
		Assert.assertEquals("Project implementation_1 is not a template.", someProjectValidation.getMessage());
	}

	@Test
	public void test_descriptorTemplateNameValidation_withOtherProject() {
		ImplementTemplateBuildWrapper.DescriptorImpl descriptor = new ImplementTemplateBuildWrapper.DescriptorImpl(hudson);
		FormValidation someProjectValidation = descriptor.doCheckTemplateName("someProject");
		Assert.assertEquals(FormValidation.error("").getClass(), someProjectValidation.getClass());
		Assert.assertEquals("Project someProject is not a template.", someProjectValidation.getMessage());
	}

	@Test
	public void test_descriptorTemplateNameValidation_nullName() {
		ImplementTemplateBuildWrapper.DescriptorImpl descriptor = new ImplementTemplateBuildWrapper.DescriptorImpl(hudson);
		FormValidation someProjectValidation = descriptor.doCheckTemplateName(null);
		Assert.assertEquals(FormValidation.error("").getClass(), someProjectValidation.getClass());
		Assert.assertEquals("Template is a required field.", someProjectValidation.getMessage());
	}

	@Test
	public void test_descriptorTemplateNameValidation_emptyName() {
		ImplementTemplateBuildWrapper.DescriptorImpl descriptor = new ImplementTemplateBuildWrapper.DescriptorImpl(hudson);
		FormValidation someProjectValidation = descriptor.doCheckTemplateName("");
		Assert.assertEquals(FormValidation.error("").getClass(), someProjectValidation.getClass());
		Assert.assertEquals("Template is a required field.", someProjectValidation.getMessage());
	}

	@Test
	public void test_descriptorTemplateNameValidation_whiteSpaceName() {
		ImplementTemplateBuildWrapper.DescriptorImpl descriptor = new ImplementTemplateBuildWrapper.DescriptorImpl(hudson);
		FormValidation someProjectValidation = descriptor.doCheckTemplateName(" \t\n");
		Assert.assertEquals(FormValidation.error("").getClass(), someProjectValidation.getClass());
		Assert.assertEquals("Template is a required field.", someProjectValidation.getMessage());
	}
}
