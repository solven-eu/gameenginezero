package eu.solven.anytabletop.agent;

import java.util.List;
import java.util.Optional;

import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.choice.IAgentChoice;

public interface IGameAgent {

	Optional<IAgentChoice> pickAction(GameState currentState, List<IAgentChoice> possibleActions);

}
