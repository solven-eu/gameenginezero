package anytabletop.checkers;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.util.concurrent.AtomicLongMap;

import anytabletop.unitests.HelpersForTests;
import anytabletop.unittests.AllowedTransitions;
import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.GameExecutor;
import eu.solven.anytabletop.GameInfo;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.agent.robot.RobotRandomOption;
import eu.solven.anytabletop.rules.GameRulesLoader;
import eu.solven.anytabletop.state.GameState;

public class TestCheckers {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCheckers.class);

	final ClassPathResource rulesResource = new ClassPathResource("checkers.yml");
	final GameInfo gameInfo = GameRulesLoader.loadRules(rulesResource);

	final GameModel model = new GameModel(gameInfo);

	protected Map<String, IGameAgent> generateRandomRobots(GameModel model) {
		Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();

		model.getGameInfo().getPlayers().forEach(p -> {
			playerIdToAgent.put(p.getId(), new RobotRandomOption(123));
		});
		return playerIdToAgent;
	}

	@Test
	public void testConfiguredTests() throws JsonParseException, JsonMappingException, IOException {
		final Resource resource = new ClassPathResource("checkers-tests.yml");
		final AllowedTransitions allowedTransitions = GameRulesLoader.loadAllowedTransitions(resource);

		new HelpersForTests(model).runGenericTests(allowedTransitions);
	}

	@Test
	public void testInitialState() throws JsonParseException, JsonMappingException, IOException {
		GameState initialState = model.generateInitialState();

		Assertions.assertThat(model.isGameOver("any", initialState)).isFalse();

		// First is black
		Assertions.assertThat(model.nextPossibleActions(initialState, "b")).isNotEmpty();
		Assertions.assertThat(model.nextPossibleActions(initialState, "w")).isEmpty();
	}

	@Test
	public void testAfter1RandomTurns() throws JsonParseException, JsonMappingException, IOException {
		Map<String, IGameAgent> robots = generateRandomRobots(model);

		GameExecutor gameExecutor = new GameExecutor();
		GameState initialState = model.generateInitialState();

		GameState stateAfterTurns = gameExecutor.playTheGame(model, initialState, robots, 1);

		Assertions.assertThat(model.isGameOver(stateAfterTurns)).isFalse();
		Assertions.assertThat(model.isGameOver("any", stateAfterTurns)).isFalse();

		// Second is white
		Assertions.assertThat(model.nextPossibleActions(stateAfterTurns, "b")).isEmpty();
		Assertions.assertThat(model.nextPossibleActions(stateAfterTurns, "w")).isNotEmpty();
	}

	@Test
	public void testAfter2RandomTurns() throws JsonParseException, JsonMappingException, IOException {
		Map<String, IGameAgent> robots = generateRandomRobots(model);

		GameExecutor gameExecutor = new GameExecutor();
		GameState initialState = model.generateInitialState();

		GameState stateAfterTurns = gameExecutor.playTheGame(model, initialState, robots, 2);

		Assertions.assertThat(model.isGameOver("any", stateAfterTurns)).isFalse();

		// Third is black
		Assertions.assertThat(model.nextPossibleActions(stateAfterTurns, "b")).isNotEmpty();
		Assertions.assertThat(model.nextPossibleActions(stateAfterTurns, "w")).isEmpty();
	}

	@Test
	public void testAfter3RandomTurns() throws JsonParseException, JsonMappingException, IOException {
		Map<String, IGameAgent> robots = generateRandomRobots(model);

		GameExecutor gameExecutor = new GameExecutor();
		GameState initialState = model.generateInitialState();

		GameState stateAfterTurns = gameExecutor.playTheGame(model, initialState, robots, 3);

		Assertions.assertThat(model.isGameOver("any", stateAfterTurns)).isFalse();

		// Fourth is white
		Assertions.assertThat(model.nextPossibleActions(stateAfterTurns, "b")).isEmpty();
		Assertions.assertThat(model.nextPossibleActions(stateAfterTurns, "w")).isNotEmpty();
	}

	@Test
	public void testAlgorithmVersusRandom() throws JsonParseException, JsonMappingException, IOException {
		Map<String, IGameAgent> robots = generateRandomRobots(model);

		AtomicLongMap<String> playerToWin = AtomicLongMap.create();
		AtomicLongMap<String> playerToTie = AtomicLongMap.create();

		GameExecutor gameExecutor = new GameExecutor();
		for (int i = 0; i < 10; i++) {
			// Some game may generate a different state for each game
			GameState initialState = model.generateInitialState();

			// Let's play with a limited number of rounds (to prevent infinite games)
			GameState gameOverState = gameExecutor.playTheGame(model, initialState, robots, 2 * 3);

			if (model.isGameOver(gameOverState)) {
				robots.keySet().forEach(player -> {
					Optional<Boolean> won =
							PepperMapHelper.getOptionalAs(gameOverState.getPlayersMetadata().get(player), "won");

					if (won.isPresent() && won.get()) {
						playerToWin.incrementAndGet(player);
					}
				});
			} else {
				// Not GameOver: Tie for every player
				robots.keySet().forEach(playerToTie::getAndIncrement);
			}
		}

		playerToWin.asMap().forEach((player, wins) -> {
			LOGGER.info("Player={} won {} times", player, wins);
		});
	}
}
