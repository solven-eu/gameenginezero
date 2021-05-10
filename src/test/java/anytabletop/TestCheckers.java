package anytabletop;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jeasy.rules.api.Facts;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.util.concurrent.AtomicLongMap;

import anytabletop.unittests.AllowedTransitions;
import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.GameExecutor;
import eu.solven.anytabletop.GameInfo;
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.IPlateauCoordinate;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.agent.robot.RobotRandomOption;
import eu.solven.anytabletop.agent.robot.RobotWithAlgorithm;
import eu.solven.anytabletop.agent.robot.algorithm.GreedyAlgorithm;
import eu.solven.anytabletop.agent.robot.evaluation.MaximumEntropyEvaluation;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.rules.GameRulesLoader;

public class TestCheckers {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCheckers.class);

	final ClassPathResource rulesResource = new ClassPathResource("checkers.yml");
	final GameInfo gameInfo = GameRulesLoader.loadRules(rulesResource);

	final GameModel model = new GameModel(gameInfo);

	// protected Map<String, IGameAgent> generateAgents(GameModel model, Function<String, IGameAgent> playerIdToAgent) {
	// Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();
	//
	// model.getGameInfo().getPlayers().forEach(p -> {
	// IGameAgent robot = playerIdToAgent.get(p);
	// if (playerIdToAgent.isEmpty()) {
	// robot = new RobotWithAlgorithm(p.getId(),
	// model,
	// new MaximumEntropyEvaluation(),
	// new GreedyAlgorithm(model));
	// } else {
	// robot = new RobotRandomOption(123);
	// }
	// playerIdToAgent.put(p.getId(), robot);
	// });
	// return playerIdToAgent;
	// }

	protected Map<String, IGameAgent> generateRandomRobots(GameModel model) {
		Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();

		model.getGameInfo().getPlayers().forEach(p -> {
			playerIdToAgent.put(p.getId(), new RobotRandomOption(123));
		});
		return playerIdToAgent;
	}

	@Test
	public void testConfiguredTests() throws JsonParseException, JsonMappingException, IOException {
		final Resource resource = new ClassPathResource("checkers-tests.yml");
		final AllowedTransitions allowedTransitions = GameRulesLoader.loadAllowedTransitions(resource);

		allowedTransitions.getAllowedTransitions().forEach(allowed -> {
			GameState fromState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "from"));
			GameState toState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "to"));

			// We implicitly consider the next player given the (optional) player metadata
			String playerId = PepperMapHelper.getRequiredString(fromState.getMetadata(), "player");
			List<IAgentChoice> allPossibleActions = model.nextPossibleActions(fromState, playerId);

			List<GameState> possibleToGameState = allPossibleActions.stream()
					.map(a -> model.applyActions(fromState, Map.of("somePlayer", a)))
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

		allowedTransitions.getForbiddenTransitions().forEach(allowed -> {
			GameState fromState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "from"));
			GameState toState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "to"));

			// We implicitly consider the next player given the (optional) player metadata
			String playerId = PepperMapHelper.getRequiredString(fromState.getMetadata(), "player");
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

	@Test
	public void testAlgorithmVersusRandom() throws JsonParseException, JsonMappingException, IOException {
		Map<String, IGameAgent> robots = generateRandomRobots(model);

		AtomicLongMap<String> playerToWin = AtomicLongMap.create();

		GameExecutor gameExecutor = new GameExecutor();
		for (int i = 0; i < 1; i++) {
			GameState initialState = model.generateInitialState();

			GameState gameOverState = gameExecutor.playTheGame(model, initialState, robots);
		}

		playerToWin.asMap().forEach((player, wins) -> {
			LOGGER.info("Player={} won {} times", player, wins);
		});
	}
}
