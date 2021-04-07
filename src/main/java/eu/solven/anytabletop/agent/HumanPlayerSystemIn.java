package eu.solven.anytabletop.agent;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.jeasy.rules.api.Facts;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.IPlateauCoordinate;

public class HumanPlayerSystemIn extends HumanPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayerSystemIn.class);

	public HumanPlayerSystemIn(GameModel gameModel) {
		super(gameModel);
	}

	@Override
	protected int selectAction(GameState currentState, List<Map<String, ?>> possibleActions) {
		int actionIndex;

		try (Scanner in = new Scanner(System.in)) {
			do {
				LOGGER.info("Possible actions:");

				ParserContext parserContext = new ParserContext();
				for (int i = 0; i < possibleActions.size(); i++) {
					Map<String, ?> actions = possibleActions.get(i);

					Facts playerFacts = gameModel.makeFacts(currentState);
					playerFacts.put("player", "?");
					PepperMapHelper.<IPlateauCoordinate>getRequiredAs(actions, "coordinates")
							.asMap()
							.forEach(playerFacts::put);

					List<String> intermediates = PepperMapHelper.getRequiredAs(actions, "intermediate");

					// Mutate with intermediate/hidden variables
					Facts enrichedFacts = gameModel.applyMutators(playerFacts, parserContext, intermediates);

					gameModel.applyMutators(enrichedFacts,
							parserContext,
							PepperMapHelper.getRequiredAs(actions, "mutation"));

					GameState state = PepperMapHelper.<GameMapInterpreter>getRequiredAs(playerFacts.asMap(), "map")
							.getLatestState();

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
}
