package anytabletop.checkers;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import anytabletop.unitests.HelpersForTests;
import anytabletop.unittests.AllowedTransitions;
import eu.solven.anytabletop.GameInfo;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.rules.GameRulesLoader;

public class TestDraughts {
	final ClassPathResource rulesResource = new ClassPathResource("draughts.yml");
	final GameInfo gameInfo = GameRulesLoader.loadRules(rulesResource);

	final GameModel model = new GameModel(gameInfo);

	@Test
	public void testConfiguredTests() throws JsonParseException, JsonMappingException, IOException {
		final Resource resource = new ClassPathResource("draughts-tests.yml");
		final AllowedTransitions allowedTransitions = GameRulesLoader.loadAllowedTransitions(resource);

		new HelpersForTests(model).runGenericTests(allowedTransitions);
	}
}
