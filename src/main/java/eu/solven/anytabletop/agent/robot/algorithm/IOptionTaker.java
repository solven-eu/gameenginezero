package eu.solven.anytabletop.agent.robot.algorithm;

import java.util.List;
import java.util.Optional;

import eu.solven.anytabletop.agent.robot.evaluation.IGameStateEvaluator;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

public interface IOptionTaker {

	Optional<IAgentChoice> pickBestOption(IGameStateEvaluator gameStateEvaluator,
			GameState currentState,
			String player,
			List<IAgentChoice> possibleActions);
}