package com.redhat.it.sso;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.keycloak.KeycloakPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

@Path("/")
public class AuthenticatedEndpoint {
	private final Logger logger = LoggerFactory.getLogger(AuthenticatedEndpoint.class);

	@Inject
	private Configuration configuration;

	public AuthenticatedEndpoint() {
	}

	public AuthenticatedEndpoint(final Configuration configuration) {
		this.configuration = configuration;
	}

	@GET
	public Response getAuthenticatedInformation(@Context SecurityContext securityContext) {

		final Template indexTemplate;
		try {
			indexTemplate = configuration.getTemplate("authenticated.ftl");
		} catch (IOException e) {
			logger.error("Unable to retrieve authenticated Freemarker template for render", e);
			throw new WebApplicationException("An error occurred attempting to render the authenticated page");
		}
		final StringWriter writer = new StringWriter();
		final Map<String, String> userInfo = new TreeMap<>();
		final Principal userPrincipal = securityContext.getUserPrincipal();

		if (userPrincipal == null || !(userPrincipal instanceof KeycloakPrincipal)) {
			logger.error("hit AuthenticatedEndpoint with unauth'd user or non-KeycloakPrincipal user.  Validate configuration, as this is an illegal state.");
			throw new WebApplicationException("User athentication was detected to be in an illegal state.  Please close your browser and retry.");
		}

		final KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) userPrincipal;
		final String accessTokenString = keycloakPrincipal.getKeycloakSecurityContext().getTokenString();
		try {
			final String prettyString = new TokenPrinter().apply(accessTokenString);
			userInfo.put("jwt", prettyString);
		} catch (Exception e) {
			logger.error("error attempting to pretty-print JSON String: ", e);
			userInfo.put("jwt", "Error attempting to decode token.  Base 64'd string: " + accessTokenString);
		}

		try {
			indexTemplate.process(userInfo, writer);
		} catch (TemplateException | IOException e) {
			logger.error("Unable to process the Freemarker template", e);
			throw new WebApplicationException("An error occurred attempting to render the authenticated page");
		}

		return Response.ok().entity(writer.toString()).build();
	}
}
