package com.attask.templating;

import com.google.common.collect.ImmutableMap;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Project;
import hudson.tasks.BuildWrapper;
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
public class ImplementTemplateBuildWrapperTest {
	@Mock private ItemGroup hudson;
	@Mock private TemplateProject template;
	@Mock private TemplateImplementationProject implementation_1;
	@Mock private TemplateImplementationProject implementation_2;
	@Mock private Project someProject;
	@Mock private ImplementTemplateBuildWrapper buildWrapper;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(hudson.getItems()).thenReturn(Arrays.asList(
				implementation_1,
				implementation_2,
				someProject
		));

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
}
