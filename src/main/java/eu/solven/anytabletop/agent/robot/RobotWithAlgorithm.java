package eu.solven.anytabletop.agent.robot;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.agent.robot.algorithm.IOptionTaker;
import eu.solven.anytabletop.agent.robot.evaluation.IGameStateEvaluator;

/**
 * A robot selecting an action random. Its behavior can be determinist by configuring with a constant seed.
 * 
 * @author Benoit Lacelle
 *
 */
public class RobotWithAlgorithm implements IGameAgent {
	final String player;

	final GameModel gameModel;
	final IGameStateEvaluator gameStateEvaluator;
	final IOptionTaker algorithm;

	public RobotWithAlgorithm(String player,
			GameModel gameModel,
			IGameStateEvaluator gameStateEvaluator,
			IOptionTaker algorithm) {
		this.player = player;

		this.gameModel = gameModel;
		this.gameStateEvaluator = gameStateEvaluator;
		this.algorithm = algorithm;
	}

	@Override
	public Optional<Map<String, ?>> pickAction(GameState currentState, List<Map<String, ?>> possibleActions) {
		return algorithm.pickBestOption(gameStateEvaluator, gameModel, currentState, player, possibleActions);
	}

}
