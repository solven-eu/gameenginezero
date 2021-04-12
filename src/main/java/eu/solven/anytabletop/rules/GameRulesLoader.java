package eu.solven.anytabletop.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import eu.solven.anytabletop.GameInfo;

public class GameRulesLoader {

	public static GameInfo loadRules(Resource rules) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		// https://manosnikolaidis.wordpress.com/2015/08/25/jackson-without-annotations/
		mapper.registerModule(new ParameterNamesModule());
		mapper.registerModule(new ParameterNamesModule());
		// make private fields of Person visible to Jackson
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		try (InputStream inputStream = rules.getInputStream()) {
			GameInfo gameInfo;
			try {
				gameInfo = mapper.readValue(inputStream, GameInfo.class);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return gameInfo;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
