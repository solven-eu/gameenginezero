package eu.solven.anytabletop.checkers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import eu.solven.anytabletop.GameExecutor;
import eu.solven.anytabletop.GameInfo;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.agent.human.HumanPlayerAwt;
import eu.solven.anytabletop.agent.robot.RobotRandomOption;
import eu.solven.anytabletop.rules.GameRulesLoader;
import eu.solven.anytabletop.state.GameState;

public class RunCheckersHumanVersusRobot {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunCheckersHumanVersusRobot.class);

	public static void playGame(final Resource rulesResource) {
		final GameInfo gameInfo = GameRulesLoader.loadRules(rulesResource);

		final GameModel model = new GameModel(gameInfo);
		GameState initialState = model.generateInitialState();
		LOGGER.info("Initial state:");
		LOGGER.info("{}{}", System.lineSeparator(), initialState);

		Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();

		playerIdToAgent.put("w", new HumanPlayerAwt("w", model));
		playerIdToAgent.put("b", new RobotRandomOption(123456789));

		new GameExecutor().playTheGame(model, initialState, playerIdToAgent, 100);
	}
}
