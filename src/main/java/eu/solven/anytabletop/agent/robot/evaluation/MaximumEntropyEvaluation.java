package eu.solven.anytabletop.agent.robot.evaluation;

import java.util.List;
import java.util.Map;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;

/**
 * We prefer the move generating the maximum number of consecutive moves. In many games, having many possible moves
 * means having many units/pieces/freedom, which is good.
 * 
 * @author Benoit Lacelle
 *
 */
public class MaximumEntropyEvaluation implements IGameStateEvaluator {

	@Override
	public double evaluate(GameModel gameModel, GameState state, String player) {
		List<Map<String, ?>> allPossibleActions = gameModel.nextPossibleActions(state);

		return allPossibleActions.size();
	}
}