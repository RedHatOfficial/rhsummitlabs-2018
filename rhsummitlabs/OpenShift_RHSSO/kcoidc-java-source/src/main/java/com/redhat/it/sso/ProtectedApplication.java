package com.redhat.it.sso;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.Set;

@ApplicationPath("/authenticated")
public class ProtectedApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		return Collections.singleton(AuthenticatedEndpoint.class);
	}
}
