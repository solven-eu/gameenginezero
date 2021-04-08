package anytabletop;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.solven.anytabletop.GameExecutor;

public class TestCheckers {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCheckers.class);

	@Test
	public void testCheckers() throws JsonParseException, JsonMappingException, IOException {

		{
			Yaml yaml = new Yaml();
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("checkers.yml");

			Map<String, ?> gameModel = yaml.load(inputStream);
			LOGGER.info("Details: {}", gameModel);
		}

		new GameExecutor().main(new String[0]);
	}
}
