package anytabletop.unitests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jeasy.rules.api.Facts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import anytabletop.unittests.AllowedTransitions;
import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.IPlateauCoordinate;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.state.GameState;

public class HelpersForTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(HelpersForTests.class);

	final GameModel model;

	public HelpersForTests(GameModel model) {
		this.model = model;
	}

	public void runGenericTests(final AllowedTransitions allowedTransitions) {
		checkAllowedTransactions(allowedTransitions);

		checkForbiddenTransitions(allowedTransitions);
	}

	private Map<IPlateauCoordinate, Map<String, Object>> computeDiffCoordinates(GameState fromState,
			GameState toState) {
		Map<IPlateauCoordinate, Map<String, Object>> concerned = new HashMap<>();
		GameMapInterpreter fromInterpreter = new GameMapInterpreter(fromState);
		GameMapInterpreter toInterpreter = new GameMapInterpreter(toState);
		model.getBoard().forEach(coordinate -> {
			int x = PepperMapHelper.getRequiredNumber(coordinate.asMap(), "x").intValue();
			int y = PepperMapHelper.getRequiredNumber(coordinate.asMap(), "y").intValue();

			char from = fromInterpreter.charAt(x, y);
			char to = toInterpreter.charAt(x, y);

			if (from != to) {
				concerned.put(coordinate, Map.of("from", from, "to", to));
			}
		});
		return concerned;
	}

	private void checkAllowedTransactions(final AllowedTransitions allowedTransitions) {
		allowedTransitions.getAllowedTransitions().forEach(allowed -> {
			GameState fromState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "from"));
			GameState toState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "to"));

			// We implicitly consider the next player given the (optional) player metadata
			String playerId = PepperMapHelper.getRequiredString(fromState.getCustomMetadata(), "playing_player");
			List<IAgentChoice> allPossibleActions = model.nextPossibleActions(fromState, playerId);

			List<GameState> possibleToGameState = allPossibleActions.stream()
					.map(a -> model.applyActions(fromState, Map.of(playerId, a)))
					.collect(Collectors.toList());

			if (possibleToGameState.stream().filter(gs -> gs.containsState(toState)).findAny().isEmpty()) {
				LOGGER.warn("Issue transitting from {}{}{}to {}{}",
						System.lineSeparator(),
						fromState,
						System.lineSeparator(),
						System.lineSeparator(),
						toState);

				// Restrict relevant coordinates to make debug easier
				Map<IPlateauCoordinate, Map<String, Object>> concernedCoordinates =
						computeDiffCoordinates(fromState, toState);

				// TODO order so that we consider first a not-empty 'from'
				List<GameState> specializedToGameStates = concernedCoordinates.entrySet().stream().flatMap(e -> {
					IPlateauCoordinate coordinate = e.getKey();
					Facts facts = model.makeFacts(fromState);
					List<IAgentChoice> actions = model.nextPossibleActions(fromState, playerId, facts, coordinate);

					return actions.stream().map(a -> model.applyActions(fromState, Map.of("somePlayer", a)));
				}).collect(Collectors.toList());

				if (specializedToGameStates.stream().filter(gs -> gs.containsState(toState)).findAny().isPresent()) {
					// WARNING something is wrong not in the game, but in the engine
					throw new IllegalStateException(
							"Inconsistency within the engine to transit from " + fromState + " to " + toState);
				}

				// TODO Differentiate between board constrain and rules constrains: while we are not much interested in
				// move rejected due to board constrain, game constrain are much more interesting in most cases
				model.getGameInfo().getPlayers().stream().map(p -> p.getId()).forEach(p -> {
					LOGGER.info("Considering player: {}", p);
					concernedCoordinates.forEach((k, v) -> {
						LOGGER.info("Debug coordinate: {}", k);
						model.getGameInfo().getAllowedMoves().forEach(potentialMove -> {
							List<? extends IAgentChoice> allowedChoices =
									model.debugWhyNotPossible(fromState, p, k, potentialMove);

							allowedChoices.stream().forEach(allowedChoice -> {
								GameState gs = model.applyActions(fromState, Map.of(p, allowedChoice));
								if (gs.containsState(toState)) {
									throw new IllegalStateException(
											"Inconsistency within the engine to transit from " + fromState
													+ " to "
													+ toState);
								}
							});
						});
					});
				});

				if (specializedToGameStates.isEmpty()) {
					throw new IllegalStateException("Not a single action from " + fromState);
				} else {
					specializedToGameStates.forEach(allowedTo -> {
						LOGGER.info("Allowed to: {}{}", System.lineSeparator(), allowedTo);
					});

					throw new IllegalStateException("We were not able to transit from " + fromState + " to " + toState);
				}
			}
		});
	}

	private void checkForbiddenTransitions(final AllowedTransitions allowedTransitions) {
		allowedTransitions.getForbiddenTransitions().forEach(allowed -> {
			GameState fromState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "from"));
			GameState toState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "to"));

			// We implicitly consider the next player given the (optional) player metadata
			String playerId = PepperMapHelper.getRequiredString(fromState.getCustomMetadata(), "playing_player");
			List<IAgentChoice> allPossibleActions = model.nextPossibleActions(fromState, playerId);

			List<GameState> possibleGameState = allPossibleActions.stream()
					.map(a -> model.applyActions(fromState, Map.of()))
					.collect(Collectors.toList());

			if (possibleGameState.stream().filter(gs -> gs.equals(toState)).findAny().isPresent()) {
				model.nextPossibleActions(fromState, playerId);
				throw new IllegalStateException("It is forbidden to transit from " + fromState + " to " + toState);
			}
		});
	}

}
