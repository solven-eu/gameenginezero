package eu.solven.anytabletop.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import anytabletop.unittests.AllowedTransitions;
import eu.solven.anytabletop.GameInfo;

public class GameRulesLoader {

	public static GameInfo loadRules(Resource rules) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		// https://manosnikolaidis.wordpress.com/2015/08/25/jackson-without-annotations/
		mapper.registerModule(new ParameterNamesModule());
		// make private fields of Person visible to Jackson
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		GameInfo gameInfo;
		try (InputStream inputStream = rules.getInputStream()) {
			try {
				gameInfo = mapper.readValue(inputStream, GameInfo.class);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (gameInfo.getDerivesFrom() != null) {
			ResourceLoader resourceLoader = new DefaultResourceLoader();
			GameInfo derivedFrom = loadRules(resourceLoader.getResource(gameInfo.getDerivesFrom()));

			Map<String, ?> derivedFromAsMap = mapper.convertValue(derivedFrom, Map.class);
			Map<String, ?> overridingsAsMap = mapper.convertValue(gameInfo, Map.class);

			Map<String, Object> merged = new LinkedHashMap<>();

			// TODO Enable merging of deeper values
			merged.putAll(derivedFromAsMap);

			overridingsAsMap.forEach((k, v) -> {
				if (v == null) {
					// Not interesting
				} else if (v instanceof Collection<?> && ((Collection<?>) v).isEmpty()) {
					// Not interesting
				} else if (v instanceof Map<?, ?> && ((Map<?, ?>) v).isEmpty()) {
					// Not interesting
				} else {
					merged.put(k, v);
				}
			});

			gameInfo = mapper.convertValue(merged, GameInfo.class);
		}

		return gameInfo;
	}

	public static AllowedTransitions loadAllowedTransitions(Resource tests) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		// https://manosnikolaidis.wordpress.com/2015/08/25/jackson-without-annotations/
		mapper.registerModule(new ParameterNamesModule());
		mapper.registerModule(new ParameterNamesModule());
		// make private fields of Person visible to Jackson
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		try (InputStream inputStream = tests.getInputStream()) {
			AllowedTransitions allowedTransitions;
			try {
				allowedTransitions = mapper.readValue(inputStream, AllowedTransitions.class);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return allowedTransitions;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
