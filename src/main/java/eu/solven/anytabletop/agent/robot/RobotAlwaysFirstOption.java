package eu.solven.anytabletop.agent.robot;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.agent.IGameAgent;

/**
 * A robot always selecting the first option (if available).
 * 
 * @author Benoit Lacelle
 *
 */
public class RobotAlwaysFirstOption implements IGameAgent {
	final int agentIndex;

	public RobotAlwaysFirstOption(int agentIndex) {
		this.agentIndex = agentIndex;
	}

	@Override
	public Optional<Map<String, ?>> pickAction(GameState currentState, List<Map<String, ?>> possibleActions) {
		if (possibleActions.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(possibleActions.get(0));
		}
	}

}
