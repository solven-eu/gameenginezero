package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder;
import com.google.common.collect.Multimaps;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.agent.human.HumanPlayerAwt;
import eu.solven.anytabletop.agent.robot.RobotRandomOption;
import eu.solven.anytabletop.rules.GameRulesLoader;

public class GameExecutor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameExecutor.class);

	public static void playGame(final Resource rulesResource) {
		final GameInfo gameInfo = GameRulesLoader.loadRules(rulesResource);

		final GameModel model = new GameModel(gameInfo);
		GameState initialState = model.generateInitialState();
		LOGGER.info("Initial state:");
		LOGGER.info("{}{}", System.lineSeparator(), initialState);

		Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();

		playerIdToAgent.put("w", new HumanPlayerAwt(model));
		playerIdToAgent.put("b", new RobotRandomOption(123456789));

		new GameExecutor().playTheGame(model, initialState, playerIdToAgent);
	}

	public GameState playTheGame(final GameModel model,
			GameState initialState,
			Map<String, IGameAgent> playerIdToAgent) {
		GameState mutatingState = initialState;

		while (true) {
			GameState currentState = mutatingState;

			if (model.isGameOver(currentState)) {
				LOGGER.info("GameOver (explicit)");
				break;
			}

			List<Map<String, ?>> allPossibleActions = model.nextPossibleActions(currentState);

			if (allPossibleActions.isEmpty()) {
				LOGGER.info("GameOver (no action available)");
				break;
			}

			ListMultimap<String, Map<String, ?>> agentToActions =
					ListMultimapBuilder.hashKeys().arrayListValues().build();

			allPossibleActions
					.forEach(action -> agentToActions.put(PepperMapHelper.getRequiredString(action, "player"), action));

			Map<String, Map<String, ?>> playerToSelectedActions = new LinkedHashMap<>();

			Multimaps.asMap(agentToActions).forEach((playerId, possibleActions) -> {
				Optional<Map<String, ?>> optAction =
						playerIdToAgent.get(playerId).pickAction(currentState, possibleActions);

				if (optAction.isPresent()) {
					playerToSelectedActions.put(playerId, optAction.get());
				} else {
					// TODO Enable a tweak for noop. One may add manually noop as an option in its gameModel
					// LOGGER.info("GameOver (no action picked by " + playerId + ")");

					model.nextPossibleActions(currentState);

					// break;
					throw new IllegalArgumentException("We require a move to be selected");
				}
			});

			GameState newState = model.applyActions(currentState, playerToSelectedActions);
			LOGGER.info("New state:{}{}", System.lineSeparator(), newState);

			mutatingState = newState;
		}

		return mutatingState;
	}
}
