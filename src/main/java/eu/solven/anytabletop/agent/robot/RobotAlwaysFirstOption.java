package eu.solven.anytabletop.agent.robot;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

/**
 * A robot always selecting the first option (if available).
 * 
 * @author Benoit Lacelle
 *
 */
public class RobotAlwaysFirstOption implements IGameAgent {
	final int agentIndex;
	final AtomicBoolean gameIsOver = new AtomicBoolean();

	public RobotAlwaysFirstOption(int agentIndex) {
		this.agentIndex = agentIndex;
	}

	@Override
	public Optional<IAgentChoice> pickAction(GameState currentState, List<IAgentChoice> possibleActions) {
		if (possibleActions.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(possibleActions.get(0));
		}
	}

	@Override
	public void notifyGameOver(GameState currentState) {
		gameIsOver.set(true);
	}
}
