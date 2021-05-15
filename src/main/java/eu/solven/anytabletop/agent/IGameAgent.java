package eu.solven.anytabletop.agent;

import java.util.List;
import java.util.Optional;

import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

public interface IGameAgent {

	Optional<IAgentChoice> pickAction(GameState currentState, List<IAgentChoice> possibleActions);

	void notifyGameOver(GameState currentState);

}
