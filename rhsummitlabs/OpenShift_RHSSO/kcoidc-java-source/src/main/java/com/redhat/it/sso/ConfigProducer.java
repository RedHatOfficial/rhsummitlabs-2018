package com.redhat.it.sso;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public final class ConfigProducer {

	@Produces
	@Singleton
	@Default
	public Configuration FreemarkerConfig() {
		final Configuration configuration = new Configuration(Configuration.VERSION_2_3_25);
		configuration.setClassForTemplateLoading(ConfigProducer.class, "/templates");
		configuration.setDefaultEncoding("UTF-8");
		configuration.setWhitespaceStripping(false);
		configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

		return configuration;
	}

}
