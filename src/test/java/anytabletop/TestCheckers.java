package anytabletop;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.util.concurrent.AtomicLongMap;

import eu.solven.anytabletop.GameExecutor;
import eu.solven.anytabletop.GameInfo;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.agent.robot.RobotRandomOption;
import eu.solven.anytabletop.agent.robot.RobotWithAlgorithm;
import eu.solven.anytabletop.agent.robot.algorithm.GreedyAlgorithm;
import eu.solven.anytabletop.agent.robot.evaluation.MaximumEntropyEvaluation;
import eu.solven.anytabletop.rules.GameRulesLoader;

public class TestCheckers {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCheckers.class);

	final ClassPathResource rulesResource = new ClassPathResource("checkers.yml");
	final GameInfo gameInfo = GameRulesLoader.loadRules(rulesResource);

	final GameModel model = new GameModel(gameInfo);

	@Test
	public void testCheckers() throws JsonParseException, JsonMappingException, IOException {
		new GameExecutor().main(new String[0]);
	}

	protected Map<String, IGameAgent> generateRandomRobots(GameModel model) {
		Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();

		model.getGameInfo().getPlayers().forEach(p -> {
			IGameAgent robot;
			if (playerIdToAgent.isEmpty()) {
				robot = new RobotWithAlgorithm(p.getId(), model, new MaximumEntropyEvaluation(), new GreedyAlgorithm());
			} else {
				robot = new RobotRandomOption(123);
			}
			playerIdToAgent.put(p.getId(), robot);
		});
		return playerIdToAgent;
	}

	@Test
	public void testAlgorithmVersusRandom() throws JsonParseException, JsonMappingException, IOException {
		Map<String, IGameAgent> robots = generateRandomRobots(model);

		AtomicLongMap<String> playerToWin = AtomicLongMap.create();

		for (int i = 0; i < 1000; i++) {
			GameState initialState = model.generateInitialState();

			GameState gameOverState = new GameExecutor().playTheGame(model, initialState, robots);
		}

		playerToWin.asMap().forEach((player, wins) -> {
			LOGGER.info("Player={} won {} times", player, wins);
		});
	}
}
