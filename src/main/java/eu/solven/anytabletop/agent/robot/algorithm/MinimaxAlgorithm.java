package eu.solven.anytabletop.agent.robot.algorithm;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder;
import com.google.common.collect.Multimaps;

import eu.solven.anytabletop.GameExecutor;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.agent.robot.evaluation.IGameStateEvaluator;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.choice.IAgentRangedChoice;
import eu.solven.anytabletop.state.GameState;

/**
 * Will consider any possible 1-move, and keep the one maximizing the {@link IGameStateEvaluator}
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/ykaragol/checkersmaster/blob/master/CheckersMaster/src/checkers/algorithm/MinimaxAlgorithm.java
public class MinimaxAlgorithm implements IOptionTaker {
	private static final Logger LOGGER = LoggerFactory.getLogger(MinimaxAlgorithm.class);

	final GameModel gameModel;

	public MinimaxAlgorithm(GameModel gameModel) {
		this.gameModel = gameModel;

		if (gameModel.getGameInfo().getPlayers().size() != 2) {
			throw new IllegalArgumentException("This is OK only for game with 2 players");
		}
	}

	@Override
	public Optional<IAgentChoice> pickBestOption(IGameStateEvaluator gameStateEvaluator,
			GameState currentState,
			String playerToWin,
			List<IAgentChoice> possibleActions) {
		if (possibleActions.isEmpty()) {
			return Optional.empty();
		}

		// TODO Verify next player is maximizingPlayer
		PrincipalVariation emptyPrincipalVariation = new PrincipalVariation(Double.MIN_VALUE, Collections.emptyList());

		PrincipalVariation minimax =
				minimax(emptyPrincipalVariation, 5, gameStateEvaluator, currentState, playerToWin, possibleActions);
		// Return the first choice of the maximizing PrincipalVariation (as it is the first action to take leading to
		// the optimal decision)
		return minimax.getChoices().stream().findFirst();
	}

	protected PrincipalVariation minimax(PrincipalVariation parent,
			int maxDepth,
			IGameStateEvaluator gameStateEvaluator,
			GameState currentState,
			String playerToWin,
			List<IAgentChoice> possibleActions) {
		if (maxDepth == parent.getChoices().size()) {
			// We are maximum depth to consider
			return parent;
		}

		AtomicReference<PrincipalVariation> selectedMove = new AtomicReference<>();
		for (IAgentChoice move : possibleActions) {
			GameState newState = gameModel.applyActions(currentState, Map.of(move.getPlayerId(), move));

			double newScore = gameStateEvaluator.evaluate(gameModel, currentState, playerToWin);

			PrincipalVariation newParent = new PrincipalVariation(newScore,
					ImmutableList.<IAgentChoice>builder().addAll(parent.getChoices()).add(move).build());

			ListMultimap<String, IAgentChoice> agentToActions =
					GameExecutor.computeEachAgentChoices(gameModel, newState);

			Multimaps.asMap(agentToActions).forEach((player, choices) -> {
				// recursion:
				PrincipalVariation minimax =
						minimax(newParent, maxDepth, gameStateEvaluator, currentState, player, choices);

				if (selectedMove.get() == null) {
					// This is the first possible action
					selectedMove.set(selectedMove.get());
				} else {
					PrincipalVariation currentBest = selectedMove.get();

					// ScoredAction minimax = optMinimax.get();
					if (
					// max value is desired for opponents
					playerToWin.equals(player) && minimax.getScore() > currentBest.getScore()
							// min value is desired for any opponent
							|| !playerToWin.equals(player) && minimax.getScore() < currentBest.getScore()) {

						selectedMove.set(minimax);
					}
				}

			});
		}

		if (selectedMove.get() == null) {
			LOGGER.warn("Is this gameOver?");
			return parent;
		} else {
			return selectedMove.get();
		}
	}
}