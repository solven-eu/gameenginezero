package eu.solven.anytabletop.agent.human;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.choice.IAgentChoice;

public abstract class HumanPlayer implements IGameAgent {
	private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayer.class);
	final GameModel gameModel;

	public HumanPlayer(GameModel gameModel) {
		this.gameModel = gameModel;
	}

	@Override
	public Optional<IAgentChoice> pickAction(GameState currentState, List<IAgentChoice> possibleActions) {
		int actionIndex;
		do {
			actionIndex = selectAction(currentState, possibleActions);

			if (actionIndex < 0 || actionIndex >= possibleActions.size()) {
				LOGGER.warn("'{}'is not a valid option", actionIndex);
			} else {
				LOGGER.debug("'{}'is a valid option", actionIndex);
				break;
			}
		} while (true);

		return Optional.of(possibleActions.get(actionIndex));
	}

	protected abstract int selectAction(GameState currentState, List<IAgentChoice> possibleActions);
}
