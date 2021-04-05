package eu.solven.anytabletop.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.solven.anytabletop.GameState;

public interface IGameAgent {

	Optional<Map<String, ?>> pickAction(GameState currentState, List<Map<String, ?>> possibleActions);

}
