package anytabletop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jeasy.rules.api.Facts;
import org.junit.jupiter.api.Test;
import org.mvel2.ParserContext;
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
import eu.solven.anytabletop.rules.GameRulesLoader;

public class TestCheckers {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCheckers.class);

	final ClassPathResource rulesResource = new ClassPathResource("checkers.yml");
	final GameInfo gameInfo = GameRulesLoader.loadRules(rulesResource);

	final GameModel model = new GameModel(gameInfo);

	protected Map<String, IGameAgent> generateRandomRobots(GameModel model) {
		Map<String, IGameAgent> playerIdToAgent = new LinkedHashMap<>();

		model.getGameInfo().getPlayers().forEach(p -> {
			IGameAgent robot;
			if (playerIdToAgent.isEmpty()) {
				robot = new RobotWithAlgorithm(p.getId(), model, new MaximumEntropyEvaluation(), new GreedyAlgorithm());
			} else {
				robot = new RobotRandomOption(123);
			}
			playerIdToAgent.put(p.getId(), robot);
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

			List<Map<String, ?>> allPossibleActions = model.nextPossibleActions(fromState);

			List<GameState> possibleGameState = allPossibleActions.stream()
					.map(a -> model.applyActions(fromState, Map.of("somePlayer", a)))
					.collect(Collectors.toList());

			if (possibleGameState.stream().filter(gs -> gs.containsAll(toState)).findAny().isEmpty()) {
				Map<IPlateauCoordinate, Map<String, Object>> concernedCoordinates =
						computeDiffCoordinates(fromState, toState);
				ParserContext parserContext = new ParserContext();

				List<GameState> gameStates = concernedCoordinates.entrySet().stream().flatMap(e -> {
					IPlateauCoordinate coordinate = e.getKey();
					Facts facts = model.makeFacts(fromState);
					List<Map<String, ?>> actions =
							model.nextPossibleActions(fromState, facts, parserContext, coordinate);

					System.out.println(actions);

					return actions.stream().map(a -> model.applyActions(fromState, Map.of("somePlayer", a)));
				}).collect(Collectors.toList());

				System.out.println(gameStates);
				throw new IllegalStateException("We were not able to transit from " + fromState + " to " + toState);
			}
		});

		allowedTransitions.getForbiddenTransitions().forEach(allowed -> {
			GameState fromState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "from"));
			GameState toState = model.loadState(PepperMapHelper.getRequiredMap(allowed, "to"));

			List<Map<String, ?>> allPossibleActions = model.nextPossibleActions(fromState);

			List<GameState> possibleGameState = allPossibleActions.stream()
					.map(a -> model.applyActions(fromState, Map.of()))
					.collect(Collectors.toList());

			if (possibleGameState.stream().filter(gs -> gs.equals(toState)).findAny().isPresent()) {
				model.nextPossibleActions(fromState);
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

		for (int i = 0; i < 1000; i++) {
			GameState initialState = model.generateInitialState();

			GameState gameOverState = new GameExecutor().playTheGame(model, initialState, robots);
		}

		playerToWin.asMap().forEach((player, wins) -> {
			LOGGER.info("Player={} won {} times", player, wins);
		});
	}
}
