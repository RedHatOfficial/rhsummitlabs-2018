package com.redhat.it.sso;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class TokenPrinterTest {
	private static final Logger logger = LoggerFactory.getLogger(TokenPrinterTest.class);

	@Test
	public void shouldPrintValidToken() throws Exception {
		final String prettyPrintedToken = new TokenPrinter().apply(tokenString);
		logger.info(prettyPrintedToken);
		assertThat(prettyPrintedToken, equalTo(prettyString));
	}

	@Test(expected = RuntimeException.class)
	public void shouldFailFast_whenInvalidToken() throws Exception {
		new TokenPrinter().apply("ASDF@#$SDFF.@#SDFASDFS" + tokenString);
	}

	/**
	 * this string was problematic for Keycloak's Base64 Decoder, not sure why.
	 * // TODO file bug + PR
	 */
	@Test
	public void shouldPrintValidToken_whenProblematicString() throws Exception {
		final String prettyPrintedToken = new TokenPrinter().apply(problematicTokenString);
		logger.info(prettyPrintedToken);
	}

	static final String tokenString = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ0Z1hyZXhZcHIwa1BZUHBkR3dsdWlFWmN2MmJRNlBKUW5zckhaSHB2Sk5VIn0.eyJqdGkiOiIzMmE5M2Y2Zi01NTkwLTQ2M2YtYjdkNy0yNGJkYjIxZGNmNWYiLCJleHAiOjE1MjIxODUxNjQsIm5iZiI6MCwiaWF0IjoxNTIyMTg1MTA0LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoib2lkYy10ZXN0Iiwic3ViIjoiZDhkYjVmOGMtZDQxMi00ZmVlLTg4ZDEtYzE3Zjc3MGQzODA0IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoib2lkYy10ZXN0IiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZTI4MzdhMjEtZTYwZi00ZGU4LTk1NWUtMTYxNTkwNmJmOGQ0IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhdXRoZW50aWNhdGVkIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJuYW1lIjoidXNlciAxIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlcjEiLCJnaXZlbl9uYW1lIjoidXNlciIsImZhbWlseV9uYW1lIjoiMSIsImVtYWlsIjoidXNlcjFAcmVkaGF0LmNvbSJ9.iR53p0mZw8Ri7W9h_xn56gF0HFpdP5pQwngja3DwpzSoR28xVHAGPACRGgNUL3xByM_dtO0_qBOiEt4Dt_nFsRsH6Dp6f6FqRFzzgQgGLbBNtka5kgw9qEaAR0AMCVWEQM_cQ6lCXs296jL8-K8VpyolUbQ8Itqitq07kwq-qu9p1dixuKfI4ipDXfxnLiOcRSQJzSVSnhJl41jVmAIxmVF2pABt9mKqsWtHVZBnhWIkOEXa3gdvY_AsQeD3hskNgFpVL9PyrgCiuU2HlxfI-QPbUTdCg4vX4_n2TbYJPJSxiPQE4wBUPGWCCZVeoo0mnigRPPufWLGBd-HT6rtGVQ";
	static final String problematicTokenString = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJZWXQzN1N1eXF3UFlsdWpuS2NIR3MwdDVwUUNyRE5zUW8xd0pqY2p3NnV3In0.eyJqdGkiOiJiYjE2NTI3Zi0yNWNhLTQzNmMtYThiOS04ZDZmNjgzODE2YjciLCJleHAiOjE1MjIyNjMxMDAsIm5iZiI6MCwiaWF0IjoxNTIyMjYzMDQwLCJpc3MiOiJodHRwczovL3NlY3VyZS10bXBzc28tZGVtby5wYWFzLmxvY2FsL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6Im9pZGMtdGVzdCIsInN1YiI6IjE4ZjUwMjk0LTQyNWItNDY3My05NDNkLTFiMzVkMWE3M2I2YiIsInR5cCI6IkJlYXJlciIsImF6cCI6Im9pZGMtdGVzdCIsImF1dGhfdGltZSI6MTUyMjI2MzA0MCwic2Vzc2lvbl9zdGF0ZSI6Ijk5NGIyODhmLTNmZWMtNGQ3MC05YzFlLTkxMTk2NTYxNDIzMyIsImFjciI6IjEiLCJjbGllbnRfc2Vzc2lvbiI6IjViNjZkZTkzLTA3NDktNGYxYS1iMDU5LTk5ZDk1ZmI1ZWM1NSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhdXRoZW50aWNhdGVkIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6InVzZXIgb25lIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlcjEiLCJnaXZlbl9uYW1lIjoidXNlciIsImZhbWlseV9uYW1lIjoib25lIiwiZW1haWwiOiJ1c2VyMUByZWRoYXQuY29tIn0.CyRvQYaqGvwn3M5RCX0ZbUt2tkcpaUsUL3t1CmCp7DKEhvtSAg2qnPMHfx8tQzvVLjGX1gvuY1soK0WNL3AiMrO0oXHXJCIZVk3DCgH_j-mAAuukt4M9Ec0EszvkVHUpGkwxPw2MGE85LZsWGdXUkVipkauBWgAe7GxRkQSu8KI7fOvpOGR5f46ibu7nL0ZSMiqy7B2aTCnHh760ioLJyUalbhKHjcdeIOeLynPzpH2ebizvB2iT9qOszHPblmd5fnrpFtnfXMZg6qXUM4FOKV3qmQsn5cKIxKFPnY82F82iLPmHg60ZgHJXEz7AndQXUR1bTH2fMYOn6d4e35LdZA";

	static final String prettyString = "\n" +
			"{\n" +
					"    \"jti\": \"32a93f6f-5590-463f-b7d7-24bdb21dcf5f\",\n" +
					"    \"exp\": 1522185164,\n" +
					"    \"nbf\": 0,\n" +
					"    \"iat\": 1522185104,\n" +
					"    \"iss\": \"http://localhost:8080/auth/realms/master\",\n" +
					"    \"aud\": \"oidc-test\",\n" +
					"    \"sub\": \"d8db5f8c-d412-4fee-88d1-c17f770d3804\",\n" +
					"    \"typ\": \"Bearer\",\n" +
					"    \"azp\": \"oidc-test\",\n" +
					"    \"auth_time\": 0,\n" +
					"    \"session_state\": \"e2837a21-e60f-4de8-955e-1615906bf8d4\",\n" +
					"    \"acr\": \"1\",\n" +
					"    \"allowed-origins\": [\n" +
					"    ],\n" +
					"    \"realm_access\": {\n" +
					"        \"roles\": [\n" +
					"            \"authenticated\",\n" +
					"            \"uma_authorization\"\n" +
					"        ]\n" +
					"    },\n" +
					"    \"resource_access\": {\n" +
					"        \"account\": {\n" +
					"            \"roles\": [\n" +
					"                \"manage-account\",\n" +
					"                \"manage-account-links\",\n" +
					"                \"view-profile\"\n" +
					"            ]\n" +
					"        }\n" +
					"    },\n" +
					"    \"name\": \"user 1\",\n" +
					"    \"preferred_username\": \"user1\",\n" +
					"    \"given_name\": \"user\",\n" +
					"    \"family_name\": \"1\",\n" +
					"    \"email\": \"user1@redhat.com\"\n" +
					"}";
}
