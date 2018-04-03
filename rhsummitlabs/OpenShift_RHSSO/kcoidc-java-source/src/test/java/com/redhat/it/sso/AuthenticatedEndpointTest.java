package com.redhat.it.sso;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.mockito.Mock;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.Writer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthenticatedEndpointTest {

	@Mock
	Configuration configuration;
	AuthenticatedEndpoint authenticatedEndpoint;

	@Before
	public void setUp() {
		initMocks(this);
		authenticatedEndpoint = new AuthenticatedEndpoint(configuration);
	}

	@Test
	public void should200_whenRenderSuccess() throws Exception {
		final Template template = mock(Template.class);
		when(configuration.getTemplate(anyString())).thenReturn(template);

		final SecurityContext securityContext = mock(SecurityContext.class);
		final KeycloakPrincipal keycloakPrincipal = mock(KeycloakPrincipal.class);
		when(securityContext.getUserPrincipal()).thenReturn(keycloakPrincipal);
		when(keycloakPrincipal.getKeycloakSecurityContext()).thenReturn(mock(KeycloakSecurityContext.class));
		final Response response = authenticatedEndpoint.getAuthenticatedInformation(securityContext);

		assertThat(response.getStatus(), equalTo(200));
	}

	@Test(expected = WebApplicationException.class)
	public void shouldWebApplicationException_whenTemplateDoesNotExist() throws Exception {
		when(configuration.getTemplate(anyString())).thenThrow(new IOException("Template not found"));
		authenticatedEndpoint.getAuthenticatedInformation(mock(SecurityContext.class));
		fail("WebApplicationException not thrown on failure to find template");
	}

	@Test(expected = WebApplicationException.class)
	public void shouldWebApplicationException_whenUserPrincipalDoesNotExist() throws Exception {
		when(configuration.getTemplate(anyString())).thenReturn(mock(Template.class));
		authenticatedEndpoint.getAuthenticatedInformation(mock(SecurityContext.class));
		fail("WebApplicationException not thrown on null user principal");
	}

	@Test(expected = WebApplicationException.class)
	public void shouldWebApplicationException_whenUserPrincipalWrongKind() throws Exception {
		when(configuration.getTemplate(anyString())).thenReturn(mock(Template.class));
		final SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getUserPrincipal()).thenReturn(mock(SimplePrincipal.class));
		authenticatedEndpoint.getAuthenticatedInformation(securityContext);
		fail("WebApplicationException not thrown on invalid user principal type");
	}

	@Test(expected = WebApplicationException.class)
	public void shouldWebApplicationException_whenTemplateRenderFails() throws Exception {
		final Template template = mock(Template.class);
		when(configuration.getTemplate(anyString())).thenReturn(template);
		doThrow(new TemplateException("Unable to process template", null)).when(template).process(any(), any(Writer.class));

		final SecurityContext securityContext = mock(SecurityContext.class);
		final KeycloakPrincipal keycloakPrincipal = mock(KeycloakPrincipal.class);
		when(securityContext.getUserPrincipal()).thenReturn(keycloakPrincipal);
		when(keycloakPrincipal.getKeycloakSecurityContext()).thenReturn(mock(KeycloakSecurityContext.class));
		authenticatedEndpoint.getAuthenticatedInformation(securityContext);
		fail("WebApplicationException not thrown on invalid user principal type");
	}
}
