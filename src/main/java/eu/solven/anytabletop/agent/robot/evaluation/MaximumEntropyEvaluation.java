package eu.solven.anytabletop.agent.robot.evaluation;

import java.util.List;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

/**
 * We prefer the move generating the maximum number of consecutive moves. In many games, having many possible moves
 * means having many units/pieces/freedom, which is good.
 * 
 * @author Benoit Lacelle
 *
 */
public class MaximumEntropyEvaluation implements IGameStateEvaluator {

	@Override
	public double evaluate(GameModel gameModel, GameState state, String playerId) {
		List<IAgentChoice> allPossibleActions = gameModel.nextPossibleActions(state, playerId);

		return allPossibleActions.size();
	}
}