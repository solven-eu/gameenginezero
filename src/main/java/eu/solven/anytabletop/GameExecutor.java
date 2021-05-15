package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.jeasy.rules.api.Facts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder;
import com.google.common.collect.Multimaps;

import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

public class GameExecutor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameExecutor.class);

	public GameState playTheGame(final GameModel model,
			GameState initialState,
			Map<String, IGameAgent> playerIdToAgent,
			int nbTurn) {
		AtomicInteger nbTurnLeft = new AtomicInteger(0);

		return playTheGame(model, initialState, playerIdToAgent, () -> {
			int currentTurnIndex = nbTurnLeft.incrementAndGet();

			if (currentTurnIndex == nbTurn) {
				LOGGER.debug("This is the last turn");
				return false;
			} else if (currentTurnIndex > nbTurn) {
				return true;
			} else {
				return false;
			}
		});
	}

	public GameState playTheGame(final GameModel model,
			GameState initialState,
			Map<String, IGameAgent> playerIdToAgent,
			BooleanSupplier isInterrupted) {
		GameState mutatingState = initialState;

		while (!isInterrupted.getAsBoolean()) {
			GameState currentState = mutatingState;

			if (model.isGameOver("before_players_turn", currentState)) {
				LOGGER.info("GameOver (explicit)");

				playerIdToAgent.forEach((k, v) -> {
					v.notifyGameOver(currentState);
				});

				break;
			}

			ListMultimap<String, IAgentChoice> agentToActions = computeEachAgentChoices(model, currentState);

			// Some game rely on available moves to determine gameOver
			playerIdToAgent.forEach((player, agent) -> {
				List<IAgentChoice> moves = agentToActions.get(player);

				Facts facts = model.makeFacts(currentState);

				facts.put("moves", moves);
				// facts.put("player", player);

				if (model.isPlayerGameOver(player, "before_player_choose_move", facts)) {
					currentState.setPlayerGameOver(player);
				}
			});
			
			if (model.isGameOver(currentState)) {
				LOGGER.debug("The game is over");
				break;
			}

			if (agentToActions.isEmpty()) {
				// TODO Some game may require to wait for some time before an action is possible by any agent
				// TODO Introduce specific action
				throw new IllegalStateException("Not a single agent has a single available action");
			}

			mutatingState = queryAndApplyPlayersAction(model, playerIdToAgent, currentState, agentToActions);

			// after_player_action
		}

		return mutatingState;
	}

	private GameState queryAndApplyPlayersAction(final GameModel model,
			Map<String, IGameAgent> playerIdToAgent,
			GameState currentState,
			ListMultimap<String, IAgentChoice> agentToActions) {
		// Let each agent choose an action
		Map<String, IAgentChoice> playerToSelectedActions = new LinkedHashMap<>();

		Multimaps.asMap(agentToActions).forEach((playerId, possibleActions) -> {
			Optional<IAgentChoice> optAction = playerIdToAgent.get(playerId).pickAction(currentState, possibleActions);

			if (optAction.isPresent()) {
				playerToSelectedActions.put(playerId, optAction.get());
			} else {
				// TODO Enable a tweak for noop. One may add manually noop as an option in its gameModel
				// LOGGER.info("GameOver (no action picked by " + playerId + ")");

				model.nextPossibleActions(currentState, playerId);

				// break;
				throw new IllegalArgumentException("We require a move to be selected");
			}
		});

		GameState newState = model.applyActions(currentState, playerToSelectedActions);
		LOGGER.info("New state:{}{}", System.lineSeparator(), newState);

		return newState;
	}

	public static ListMultimap<String, IAgentChoice> computeEachAgentChoices(final GameModel model,
			GameState currentState) {
		// Each agent may have some actions
		ListMultimap<String, IAgentChoice> agentToActions = ListMultimapBuilder.hashKeys().arrayListValues().build();

		model.getGameInfo().getPlayers().forEach(player -> {
			String playerId = player.getId();
			agentToActions.putAll(playerId, model.nextPossibleActions(currentState, playerId));
		});
		return agentToActions;
	}
}
