package eu.solven.anytabletop.agent.robot.algorithm;

import java.util.List;
import java.util.Optional;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.agent.robot.evaluation.IGameStateEvaluator;
import eu.solven.anytabletop.choice.IAgentChoice;

public interface IOptionTaker {

	Optional<IAgentChoice> pickBestOption(IGameStateEvaluator gameStateEvaluator,
			GameModel gameModel,
			GameState currentState,
			String player,
			List<IAgentChoice> possibleActions);
}