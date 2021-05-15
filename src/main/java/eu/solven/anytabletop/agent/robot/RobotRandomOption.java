package eu.solven.anytabletop.agent.robot;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

/**
 * A robot selecting an action random. Its behavior can be determinist by configuring with a constant seed.
 * 
 * @author Benoit Lacelle
 *
 */
public class RobotRandomOption implements IGameAgent {
	final Random random;
	final AtomicBoolean gameIsOver = new AtomicBoolean();

	public RobotRandomOption(int seed) {
		this.random = new Random(seed);
	}

	@Override
	public Optional<IAgentChoice> pickAction(GameState currentState, List<IAgentChoice> possibleActions) {
		if (possibleActions.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(possibleActions.get(random.nextInt(possibleActions.size())));
		}
	}

	@Override
	public void notifyGameOver(GameState currentState) {
		gameIsOver.set(true);
	}

}
