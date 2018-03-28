package com.redhat.it.sso;


import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Takes a base-64 encoded JWT, and turns it into a prett-printed decoded string
 *
 * @throws RuntimeException if the string cannot be pretty-printed
 */
public class TokenPrinter implements Function<String, String> {

	@Override
	public String apply(final String jwtString) {
		try {
			final String[] jwtParts = jwtString.split("\\.");
			if (jwtParts.length < 2) {
				throw new RuntimeException("recieved a JWT string without a message header + body");
			}
			final String tokenPayloadString = jwtParts[1];

			final String decoded = new String(Base64.getDecoder().decode(tokenPayloadString));
			JsonReader reader = Json.createReader(new StringReader(decoded));
			JsonObject jsonObject = reader.readObject();

			final StringWriter prettyJsonWriter = new StringWriter();
			Map<String, Object> properties = new HashMap<>(1);
			properties.put(JsonGenerator.PRETTY_PRINTING, true);
			JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
			JsonWriter jsonWriter = writerFactory.createWriter(prettyJsonWriter);

			jsonWriter.writeObject(jsonObject);
			jsonWriter.close();

			return prettyJsonWriter.toString();
		} catch (Exception e) {
			throw new RuntimeException("Unable to pretty-print JSON string: " + jwtString);
		}
	}
}
