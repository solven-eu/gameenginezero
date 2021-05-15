package eu.solven.anytabletop.agent.human;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.jeasy.rules.api.Facts;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.IPlateauCoordinate;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

public class HumanPlayerSystemIn extends HumanPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayerSystemIn.class);

	public HumanPlayerSystemIn(GameModel gameModel) {
		super(gameModel);
	}

	@Override
	protected int selectAction(GameState currentState, List<IAgentChoice> possibleActions) {
		int actionIndex;

		try (Scanner in = new Scanner(System.in)) {
			do {
				LOGGER.info("Possible actions:");

				for (int i = 0; i < possibleActions.size(); i++) {
					IAgentChoice actions = possibleActions.get(i);

					GameState state = gameModel.applyChoice(currentState, actions);

					// states.add(state);
					currentState.diffTo(state);

					LOGGER.info("{}: {}", i, actions);
				}

				actionIndex = in.nextInt();

				if (actionIndex < 0 || actionIndex > 0) {
					LOGGER.warn("'{}'is not a valid option", actionIndex);
				} else {
					LOGGER.debug("'{}'is a valid option", actionIndex);
					break;
				}
			} while (true);
		}

		return actionIndex;
	}

	@Override
	public void notifyGameOver(GameState currentState) {
		LOGGER.info("The game is over for you (and possibly for all players)");
	}
}
