package eu.solven.anytabletop.agent.robot.evaluation;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.state.GameState;

/**
 * Used to give a score to a gameState.
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGameStateEvaluator {

	double evaluate(GameModel gameModel, GameState state, String player);

}
