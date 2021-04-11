package eu.solven.anytabletop.agent.robot.algorithm;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.agent.robot.evaluation.IGameStateEvaluator;

public interface IOptionTaker {

	Optional<Map<String, ?>> pickBestOption(IGameStateEvaluator gameStateEvaluator,
			GameModel gameModel,
			GameState currentState,
			String player,
			List<Map<String, ?>> possibleActions);
}