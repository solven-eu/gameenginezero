package eu.solven.anytabletop.agent.robot.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.agent.robot.evaluation.IGameStateEvaluator;

/**
 * Will consider any possible 1-move, and keep the one maximizing the {@link IGameStateEvaluator}
 * 
 * @author Benoit Lacelle
 *
 */
// TODO We may prefer a Greedy algorithm which has no way to compute a score, but only to compare 2 states (i.e. it may
// be easier to compare positions, than computing a score for a position).
// This may be even more relevant to choose the position which minimizes the position of the opponent, especially if
// multiple of our position looks equivalent. (is this equivalent to alpha-beta pruning?)
public class GreedyAlgorithm implements IOptionTaker {

	@Override
	public Optional<Map<String, ?>> pickBestOption(IGameStateEvaluator gateStateEvaluator,
			GameModel gameModel,
			GameState currentState,
			String player,
			List<Map<String, ?>> possibleActions) {
		if (possibleActions.isEmpty()) {
			return Optional.empty();
		}

		double maxValue = Double.MAX_VALUE;
		List<Map<String, ?>> selectedMove = new ArrayList<>();

		// Consider each move
		for (int i = 0; i < possibleActions.size(); i++) {
			Map<String, ?> currentMove = possibleActions.get(i);

			// Compute the consecutive state
			GameState newState = gameModel.applyActions(currentState, Map.of(player, currentMove));

			// Compute the score for given state
			double currentValue = gateStateEvaluator.evaluate(gameModel, newState, player);

			if (maxValue < currentValue) {
				selectedMove.clear();

				maxValue = currentValue;
				selectedMove.add(currentMove);
			}
		}

		return selectedMove.stream().findFirst();
	}
}