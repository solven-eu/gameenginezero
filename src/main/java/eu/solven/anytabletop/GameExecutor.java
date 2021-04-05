package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder;
import com.google.common.collect.Multimaps;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.agent.GamePlayers;
import eu.solven.anytabletop.agent.IGameAgent;

public class GameExecutor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameExecutor.class);

	public static void main(String[] args) {
		GameModel model = new GameModel();

		GameState initialState = model.generateInitialState();
		LOGGER.info("Initial state:");
		LOGGER.info("{}{}", System.lineSeparator(), initialState);

		GamePlayers players = model.generatePlayers(2);

		Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();

		playerIdToAgent.put("w", players.getAgent(0));
		playerIdToAgent.put("b", players.getAgent(1));

		GameState mutatingState = initialState;

		while (true) {
			GameState currentState = mutatingState;

			if (model.isGameOver(currentState)) {
				LOGGER.info("GameOver (explicit)");
				break;
			}

			List<Map<String, ?>> allPossibleActions = model.nextPossibleActions(currentState);

			if (allPossibleActions.isEmpty()) {
				LOGGER.info("GameOver (no action)");
				break;
			}

			ListMultimap<String, Map<String, ?>> agentToActions =
					ListMultimapBuilder.hashKeys().arrayListValues().build();

			allPossibleActions
					.forEach(action -> agentToActions.put(PepperMapHelper.getRequiredString(action, "player"), action));

			Map<String, Map<String, ?>> playerToSelectedActions = new LinkedHashMap<>();

			Multimaps.asMap(agentToActions).forEach((gameAgent, possibleActions) -> {
				Optional<Map<String, ?>> optAction =
						playerIdToAgent.get(gameAgent).pickAction(currentState, possibleActions);

				if (optAction.isPresent()) {
					playerToSelectedActions.put(gameAgent, optAction.get());
				} else {
					// TODO Enable a tweak for noop. One may add manually noop as an option in its gameModel
					throw new IllegalArgumentException("We require a move to be selected");
				}
			});

			GameState newState = model.applyActions(currentState, playerToSelectedActions);
			LOGGER.info("New state:{}{}", System.lineSeparator(), newState);

			mutatingState = newState;
		}
	}
}
