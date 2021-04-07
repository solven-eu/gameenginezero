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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.agent.GamePlayers;
import eu.solven.anytabletop.agent.HumanPlayer;
import eu.solven.anytabletop.agent.HumanPlayerAwt;
import eu.solven.anytabletop.agent.RobotAlwaysFirstOption;
import eu.solven.anytabletop.map.IPlateau;
import eu.solven.anytabletop.map.PlateauMap;
import eu.solven.anytabletop.rules.FactMutator;

public class GameModel {
	final int minNbPlayers = 2;
	final int maxNbPlayers = 2;

	final IPlateau plateau = new PlateauMap(Map.of("type", "grid", "maxX", 10, "maxY", 10));

	final List<Map<String, ?>> parameters = Arrays.asList(Map.of("name", "maxX", "condition", List.of("maxX == 10")),
			Map.of("name", "maxY", "condition", List.of("maxY == 10")));

	final IStateProvider initialStateProvider = new DameStateProvider(plateau);

	final List<Map<String, ?>> rendering = new ArrayList<>();

	final Map<String, Object> constants = new LinkedHashMap<>();

	final List<Map<String, ?>> allowedMoves = new ArrayList<>();

	public GameModel() {
		rendering.add(Map.of("style", "border: black"));
		rendering.add(Map.of("style", "bg: white", "condition", List.of("(i+j) % 2 == 0")));
		rendering.add(Map.of("style", "bg: black", "condition", List.of("(i+j) % 2 == 1")));

		rendering.add(Map.of("style", "item: white", "condition", List.of("map.charAt(oX,oY) == 'W'")));
		rendering.add(Map.of("style", "item: black", "condition", List.of("map.charAt(oX,oY) == 'B'")));

		constants.put("maxX", 10);
		constants.put("maxY", 10);

		// Beware these conditions are evaluated first, else we may try checking a mapped value for invalid coordinates
		List<String> moveConditions = List.of("tX>=0", "tX<maxX", "tY>=0", "tY<maxY");
		{
			List<String> mutations = List.of("map.charAt(oX,oY,' ')", "map.charAt(tX,tY,'W')");
			List<String> whiteConditions = List.of("player.equals(\"w\")",
					"map.charAt(oX,oY) == 'W'",
					"map.charAt(tX,tY) == ' ' || map.charAt(tX,tY) == 'B'");
			List<String> conditions =
					ImmutableList.<String>builder().addAll(moveConditions).addAll(whiteConditions).build();

			// White goes rightUp
			{
				List<String> intermediate = List.of("mutator.put('tX',oX+1)", "mutator.put('tY',oY+1)");
				allowedMoves.add(Map.of("intermediate", intermediate, "condition", conditions, "mutation", mutations));
			}
			// White goes leftUp
			{
				List<String> intermediate = List.of("mutator.put('tX',oX-1)", "mutator.put('tY',oY+1)");
				allowedMoves.add(Map.of("intermediate", intermediate, "condition", conditions, "mutation", mutations));
			}
		}

		{
			List<String> mutations = List.of("map.charAt(oX,oY,' ')", "map.charAt(tX,tY,'B')");
			List<String> whiteConditions = List.of("player.equals(\"b\")",
					"map.charAt(oX,oY) == 'B'",
					"map.charAt(tX,tY) == ' ' || map.charAt(tX,tY) == 'W'");
			List<String> conditions =
					ImmutableList.<String>builder().addAll(moveConditions).addAll(whiteConditions).build();

			// Black goes rightDown
			{
				List<String> intermediate = List.of("mutator.put('tX',oX+1)", "mutator.put('tY',oY-1)");
				allowedMoves.add(Map.of("intermediate", intermediate, "condition", conditions, "mutation", mutations));
			}
			// Black goes leftDown
			{
				List<String> intermediate = List.of("mutator.put('tX',oX-1)", "mutator.put('tY',oY-1)");
				allowedMoves.add(Map.of("intermediate", intermediate, "condition", conditions, "mutation", mutations));
			}
		}
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
			playerFacts.put("player", "?");
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

		// int maxX = PepperMapHelper.getRequiredNumber(currentState.getMetadata(), "maxX").intValue();
		// int maxY = PepperMapHelper.getRequiredNumber(currentState.getMetadata(), "maxY").intValue();

		List<Map<String, ?>> availableActions = new ArrayList<>();

		ParserContext parserContext = new ParserContext();

		plateau.forEach(coordinate -> {
			coordinate.asMap().forEach(facts::put);

			allowedMoves.forEach(move -> {
				List<String> intermediates = PepperMapHelper.getRequiredAs(move, "intermediate");

				// Mutate with intermediate/hidden variables
				Facts enrichedFacts = applyMutators(facts, parserContext, intermediates);

				List<String> conditions = PepperMapHelper.getRequiredAs(move, "condition");
				List<String> mutations = PepperMapHelper.getRequiredAs(move, "mutation");
				// List<String> validations = PepperMapHelper.getRequiredAs(move, "validation");

				Stream.of("w", "b").forEach(player -> {
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
						// Facts mutatedFacts = applyMutators(playerFacts, parserContext, mutations);

						// boolean validated = logicalAnd(mutatedFacts, validations, parserContext);
						//
						// if (validated) {
						availableActions.add(ImmutableMap.<String, Object>builder()
								.put("player", player)
								.put("coordinates", coordinate)
								.put("mutation", mutations)
								.put("intermediate", intermediates)
								.build());
						// }
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
