package eu.solven.anytabletop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.mvel.MVELAction;
import org.jeasy.rules.mvel.MVELCondition;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.agent.GamePlayers;
import eu.solven.anytabletop.agent.HumanPlayerAwt;
import eu.solven.anytabletop.agent.PlayerPojo;
import eu.solven.anytabletop.agent.RobotAlwaysFirstOption;
import eu.solven.anytabletop.map.BoardFromMap;
import eu.solven.anytabletop.map.IBoard;
import eu.solven.anytabletop.rules.FactMutator;

public class GameModel {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameModel.class);

	final int minNbPlayers = 2;
	final int maxNbPlayers = 2;

	final IBoard board;

	final List<Map<String, ?>> parameters = Arrays.asList(Map.of("name", "maxX", "condition", List.of("maxX == 10")),
			Map.of("name", "maxY", "condition", List.of("maxY == 10")));

	final IStateProvider initialStateProvider;

	final List<Map<String, ?>> rendering = new ArrayList<>();

	final Map<String, Object> constants = new LinkedHashMap<>();

	final List<Map<String, ?>> allowedMoves = new ArrayList<>();

	// final List<Object> nextPlayers = new ArrayList<>();

	final GameInfo gameInfo;

	public GameModel(GameInfo gameInfo) {
		this.gameInfo = gameInfo;

		LOGGER.info("About to play a game of: {}", gameInfo.getName());

		rendering.addAll(gameInfo.getRenderings());

		constants.putAll(gameInfo.getConstants());

		// nextPlayers.addAll(gameInfo.getNextPlayers());

		board = new BoardFromMap(gameInfo.getBoard());
		initialStateProvider = new StateProviderFromMap(gameInfo);

		allowedMoves.addAll(gameInfo.getAllowedMoves());
	}

	public GameState generateInitialState() {
		return initialStateProvider.generateInitialState();
	}

	public GamePlayers generatePlayers(int nbPlayers) {
		return new GamePlayers(Stream
				.concat(Stream.of(new HumanPlayerAwt(this)),
						IntStream.range(1, nbPlayers).mapToObj(i -> new RobotAlwaysFirstOption(i)))
				.collect(Collectors.toList()));
	}

	public boolean isGameOver(GameState currentState) {
		return false;
	}

	public GameState applyActions(GameState currentState, Map<String, Map<String, ?>> actions) {
		if (actions.isEmpty()) {
			return currentState;
		}

		ParserContext parserContext = new ParserContext();

		GameState newState = currentState;
		for (Map<String, ?> action : actions.values()) {
			Facts playerFacts = this.makeFacts(currentState);
			// playerFacts.put("player", "?");
			PepperMapHelper.<IPlateauCoordinate>getRequiredAs(action, "coordinates").asMap().forEach(playerFacts::put);

			List<String> intermediates = PepperMapHelper.getRequiredAs(action, "intermediate");

			// Mutate with intermediate/hidden variables
			Facts enrichedFacts = this.applyMutators(playerFacts, parserContext, intermediates);

			this.applyMutators(enrichedFacts, parserContext, PepperMapHelper.getRequiredAs(action, "mutation"));

			newState = PepperMapHelper.<GameMapInterpreter>getRequiredAs(playerFacts.asMap(), "map").getLatestState();
		}

		return newState;
	}

	public List<Map<String, ?>> nextPossibleActions(GameState currentState) {
		// final String nextPlayer = PepperMapHelper.getRequiredString(currentState.getMetadata(), "player");

		Facts facts = makeFacts(currentState);

		List<Map<String, ?>> availableActions = new ArrayList<>();

		ParserContext parserContext = new ParserContext();

		board.forEach(coordinate -> {
			coordinate.asMap().forEach(facts::put);

			allowedMoves.forEach(move -> {
				List<String> intermediates = PepperMapHelper.getRequiredAs(move, "intermediate");

				// Mutate with intermediate/hidden variables
				Facts enrichedFacts = applyMutators(facts, parserContext, intermediates);

				List<String> conditions = PepperMapHelper.getRequiredAs(move, "conditions");
				List<String> mutations = PepperMapHelper.getRequiredAs(move, "mutations");
				// List<String> validations = PepperMapHelper.getRequiredAs(move, "validation");

				gameInfo.getPlayers().stream().map(PlayerPojo::getId).forEach(player -> {
					Optional<String> allowedPlayers =
							PepperMapHelper.getOptionalString(currentState.getMetadata(), "player");
					if (allowedPlayers.isPresent() && !allowedPlayers.get().equals(player)) {
						return;
					}

					Facts playerFacts = cloneFacts(enrichedFacts);
					playerFacts.put("player", player);

					boolean conditionIsOk = logicalAnd(playerFacts, conditions, parserContext);

					if (!conditionIsOk) {
						return;
					}

					{
						availableActions.add(ImmutableMap.<String, Object>builder()
								.put("player", player)
								.put("coordinates", coordinate)
								.put("mutation", mutations)
								.put("intermediate", intermediates)
								.build());
					}
				});
			});
		});

		// May be empty when waiting for an external events: e.g. during an animation
		return availableActions;
	}

	public Facts makeFacts(GameState state) {
		Facts facts = new Facts();

		constants.forEach(facts::put);

		facts.put("map", new GameMapInterpreter(state));

		return facts;
	}

	public Facts applyMutators(Facts facts, ParserContext parserContext, List<String> mutations) {
		Facts enrichedFacts = cloneFacts(facts);

		// Add a mutator, enabling mutation
		enrichedFacts.put("mutator", new FactMutator(enrichedFacts));
		for (String mutation : mutations) {
			MVELAction action = new MVELAction(mutation, parserContext);

			action.execute(enrichedFacts);
		}
		return enrichedFacts;
	}

	private Facts cloneFacts(Facts facts) {
		Facts mutatedFacts = new Facts();

		// Copy current state before mutations
		facts.asMap().forEach(mutatedFacts::put);
		return mutatedFacts;
	}

	private boolean logicalOr(Facts facts, List<String> conditions, ParserContext parserContext) {
		boolean conditionIsOk = false;

		for (String condition : conditions) {
			MVELCondition c = new MVELCondition(condition, parserContext);

			if (c.evaluate(facts)) {
				conditionIsOk = true;
				break;
			}
		}
		return conditionIsOk;
	}

	private boolean logicalAnd(Facts facts, List<String> conditions, ParserContext parserContext) {
		boolean conditionIsOk = true;

		for (String condition : conditions) {
			MVELCondition c = new MVELCondition(condition, parserContext);

			if (!c.evaluate(facts)) {
				conditionIsOk = false;
				break;
			}
		}
		return conditionIsOk;
	}

}
