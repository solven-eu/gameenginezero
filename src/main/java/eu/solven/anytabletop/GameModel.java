package eu.solven.anytabletop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.mvel.MVELAction;
import org.jeasy.rules.mvel.MVELCondition;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.agent.PlayerPojo;
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

	final GameInfo gameInfo;

	public GameModel(GameInfo gameInfo) {
		this.gameInfo = gameInfo;

		LOGGER.info("About to play a game of: {}", gameInfo.getName());

		rendering.addAll(gameInfo.getRenderings());

		constants.putAll(gameInfo.getConstants());

		board = new BoardFromMap(gameInfo.getBoard());
		initialStateProvider = new StateProviderFromMap(gameInfo);

		allowedMoves.addAll(gameInfo.getAllowedMoves());
	}

	public GameInfo getGameInfo() {
		return gameInfo;
	}

	public GameState generateInitialState() {
		return initialStateProvider.generateInitialState();
	}

	public boolean isGameOver(GameState currentState) {
		Facts facts = this.makeFacts(currentState);

		facts.put("set", "before_turn");

		ParserContext parserContext = new ParserContext();
		gameInfo.getWinConditions().stream().filter(c -> {
			List<String> conditions = PepperMapHelper.getRequiredAs(c, "conditions");

			Set<String> variables = facts.asMap().keySet();

			List<String> winningPlayers = gameInfo.getPlayers().stream().map(PlayerPojo::getId).filter(player -> {
				Facts playerFacts = cloneFacts(facts);
				playerFacts.put("player", player);

				List<String> allConditions = ImmutableList.<String>builder()
						.addAll(constrainsToConditions(gameInfo.getConstrains(), variables))
						.addAll(conditions)
						.build();

				return logicalAnd(playerFacts, allConditions, parserContext);
			}).collect(Collectors.toList());

			// - "step.equals()"
			// - "player.equals('b')"
			// - "moves.isEmpty()"

			return !winningPlayers.isEmpty();
		});

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
			List<Facts> asList = this.applyMutators(playerFacts, parserContext, intermediates);

			// It would be an error to have multiple options as output of the selected option
			Facts enrichedFacts = Iterables.getOnlyElement(asList);

			this.applyMutators(enrichedFacts, parserContext, PepperMapHelper.getRequiredAs(action, "mutation"));

			newState = PepperMapHelper.<GameMapInterpreter>getRequiredAs(playerFacts.asMap(), "map").getLatestState();
		}

		return newState;
	}

	public List<Map<String, ?>> nextPossibleActions(GameState currentState) {
		Facts facts = makeFacts(currentState);

		List<Map<String, ?>> availableActions = new ArrayList<>();

		ParserContext parserContext = new ParserContext();

		board.forEach(coordinate -> {
			coordinate.asMap().forEach(facts::put);

			allowedMoves.forEach(move -> {
				List<String> intermediates = PepperMapHelper.getRequiredAs(move, "intermediate");

				// Mutate with intermediate/hidden variables
				applyMutators(facts, parserContext, intermediates).forEach(enrichedFacts -> {

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

						Set<String> variables = playerFacts.asMap().keySet();

						List<String> allConditions = ImmutableList.<String>builder()
								.addAll(constrainsToConditions(gameInfo.getConstrains(), variables))
								.addAll(conditions)
								.build();

						boolean conditionIsOk = logicalAnd(playerFacts, allConditions, parserContext);

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
		});

		// May be empty when waiting for an external events: e.g. during an animation
		return availableActions;
	}

	private List<? extends String> constrainsToConditions(List<Map<String, ?>> constrains, Set<String> usedVariables) {

		return constrains.stream().flatMap(constrain -> {
			List<String> variables = PepperMapHelper.getRequiredAs(constrain, "variables");

			Set<String> relevantVariables =
					Sets.intersection(usedVariables, variables.stream().collect(Collectors.toSet()));

			int min = PepperMapHelper.getRequiredNumber(constrain, "min").intValue();
			int max = PepperMapHelper.getRequiredNumber(constrain, "max").intValue();

			return relevantVariables.stream().flatMap(v -> Stream.of(v + ">=" + min, v + "<" + max));
		}).collect(Collectors.toList());
	}

	public Facts makeFacts(GameState state) {
		Facts facts = new Facts();

		constants.forEach(facts::put);

		facts.put("map", new GameMapInterpreter(state));

		return facts;
	}

	public List<Facts> applyMutators(Facts facts, ParserContext parserContext, List<String> mutations) {
		// Some mutators will generate a set of possible outputs
		List<Facts> outputfacts = new ArrayList<>();

		{
			Facts enrichedFacts = cloneFacts(facts);

			// Add a mutator, enabling mutation
			enrichedFacts.put("mutator", new FactMutator(enrichedFacts));

			outputfacts.add(enrichedFacts);
		}

		for (String mutation : mutations) {
			MVELAction action;
			try {
				action = new MVELAction(mutation, parserContext);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Issue with mutation: [[" + mutation + "]]", e);
			}

			List<Facts> outputfacts2 = new ArrayList<>();

			outputfacts.forEach(f -> {
				action.execute(f);

				List<Fact<?>> simpleFacts = new ArrayList<>();
				Map<String, List<?>> listFacts = new LinkedHashMap<>();
				f.forEach(fact -> {
					Object factValue = fact.getValue();
					if (factValue instanceof List<?>) {
						listFacts.put(fact.getName(), (List<?>) factValue);
					} else {
						simpleFacts.add(fact);
					}
				});

				if (listFacts.isEmpty()) {
					outputfacts2.add(f);
				} else {
					List<Set<Map.Entry<String, ?>>> sets = new ArrayList<>();

					listFacts.entrySet().forEach(e -> {
						Set<Map.Entry<String, ?>> set =
								e.getValue().stream().map(o -> Map.entry(e.getKey(), o)).collect(Collectors.toSet());

						sets.add(set);
					});

					Sets.cartesianProduct(sets).forEach(tuple -> {
						Facts clone = new Facts();

						simpleFacts.forEach(clone::add);

						tuple.forEach(e -> clone.put(e.getKey(), e.getValue()));
					});
				}
			});

			outputfacts.clear();
			outputfacts.addAll(outputfacts2);
		}
		return outputfacts;
	}

	public static Facts cloneFacts(Facts facts) {
		Facts mutatedFacts = new Facts();

		// Copy current state before mutations
		facts.asMap().forEach(mutatedFacts::put);
		return mutatedFacts;
	}

	public static boolean logicalAnd(Facts facts, List<String> conditions, ParserContext parserContext) {
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
