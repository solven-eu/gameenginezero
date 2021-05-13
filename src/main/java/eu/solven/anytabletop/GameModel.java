package eu.solven.anytabletop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jeasy.rules.api.Facts;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.agent.IGameAgent;
import eu.solven.anytabletop.agent.PlayerPojo;
import eu.solven.anytabletop.choice.AgentChoice;
import eu.solven.anytabletop.choice.GameChoiceInterpreter;
import eu.solven.anytabletop.choice.IAgentChoice;
import eu.solven.anytabletop.map.BoardFromMap;
import eu.solven.anytabletop.map.IBoard;
import eu.solven.anytabletop.mutations.SingleAndRangeMutations;

public class GameModel {
	private static final String KEY_INTERMEDIATE = "intermediate";

	private static final Logger LOGGER = LoggerFactory.getLogger(GameModel.class);

	final int minNbPlayers = 2;
	final int maxNbPlayers = 2;

	final IBoard board;

	final List<Map<String, ?>> parameters = Arrays.asList(Map.of("name", "maxX", "condition", List.of("maxX == 10")),
			Map.of("name", "maxY", "condition", List.of("maxY == 10")));

	final ParserContext parserContext = new ParserContext();

	final IStateProvider gameStateManager;

	final List<Map<String, ?>> rendering = new ArrayList<>();

	final Map<String, Object> constants = new LinkedHashMap<>();

	final List<Map<String, ?>> allowedMoves = new ArrayList<>();

	final Set<String> allMovesConditions = new LinkedHashSet<>();

	final GameInfo gameInfo;

	public GameModel(GameInfo gameInfo) {
		this.gameInfo = gameInfo;

		LOGGER.info("About to play a game of: {}", gameInfo.getName());

		rendering.addAll(gameInfo.getRenderings());

		constants.putAll(gameInfo.getConstants());

		board = new BoardFromMap(gameInfo.getBoard());
		gameStateManager = new StateProviderFromMap(gameInfo);

		allowedMoves.addAll(gameInfo.getAllowedMoves());

		allowedMoves.forEach(move -> {
			List<String> conditions = PepperMapHelper.getRequiredAs(move, "conditions");
			allMovesConditions.addAll(conditions);
		});
	}

	public GameInfo getGameInfo() {
		return gameInfo;
	}

	public GameState generateInitialState() {
		return gameStateManager.generateInitialState();
	}

	public GameState loadState(Map<String, ?> setup) {
		return gameStateManager.loadState(setup);
	}

	public IBoard getBoard() {
		return board;
	}

	public boolean isGameOver(GameState currentState) {
		Facts facts = this.makeFacts(currentState);

		// facts.put("set", "before_turn");

		gameInfo.getGameoverConditions().stream().filter(c -> {
			List<String> conditions = PepperMapHelper.getRequiredAs(c, "conditions");

			Set<String> variables = facts.asMap().keySet();

			List<String> winningPlayers = gameInfo.getPlayers().stream().map(PlayerPojo::getId).filter(player -> {
				Facts playerFacts = GameModelHelpers.cloneFacts(facts);
				playerFacts.put("player", player);

				List<String> allConditions = ImmutableList.<String>builder()
						.addAll(constrainsToConditions(gameInfo.getConstrains(), variables))
						.addAll(conditions)
						.build();

				return GameModelHelpers.logicalAnd(playerFacts, allConditions, parserContext);
			}).collect(Collectors.toList());

			// - "step.equals()"
			// - "player.equals('b')"
			// - "moves.isEmpty()"

			return !winningPlayers.isEmpty();
		});

		return false;
	}

	/**
	 * @param currentState
	 * @param actions
	 *            each agent can select a single action
	 * @return
	 */
	public GameState applyActions(GameState currentState, Map<String, IAgentChoice> actions) {
		if (actions.isEmpty()) {
			return currentState;
		}

		GameState newState = currentState;
		for (IAgentChoice action : actions.values()) {
			newState = applyChoice(currentState, action);
		}

		return newState;
	}

	public GameState applyChoice(GameState currentState, IAgentChoice action) {
		Facts playerFacts = this.makeFacts(currentState);
		action.getCoordinate().asMap().forEach(playerFacts::put);

		// Mutate with intermediate/hidden variables
		Facts postIntermediates = GameModelHelpers.applyPointMutators(playerFacts, action.getIntermediates());

		Facts postMutations = GameModelHelpers.applyPointMutators(postIntermediates, action.getMutations());

		return PepperMapHelper.<GameMapInterpreter>getRequiredAs(postMutations.asMap(), "map").getLatestState();
	}

	public List<IAgentChoice> nextPossibleActions(GameState currentState, String playerId) {
		Facts facts = makeFacts(currentState);

		Object presetPlayer = facts.get("player");
		if (null == presetPlayer) {
			// The game does not manage manually the allowed players: all players may have an action on any turn
			facts.put("player", playerId);
		} else if (!presetPlayer.equals(playerId)) {
			LOGGER.debug("The player is preset to: {} while reauested choices are for: {}", presetPlayer, playerId);
			return Collections.emptyList();
		}

		List<IAgentChoice> availableActions = new ArrayList<>();

		List<Map<String, ?>> playedAllowedMoves;

		{
			Facts boardWildcardFacts = GameModelHelpers.cloneFacts(facts);

			// board.wildcard(boardWildcardFacts);

			playedAllowedMoves = allowedMoves.stream().filter(m -> {
				// Reject early if none of these coordinates is valid
				List<String> allConditions =
						ImmutableList.<String>builder().addAll(getGenericConditions(boardWildcardFacts)).build();

				boolean conditionIsOk =
						GameModelHelpers.unsafeLogicalAnd(boardWildcardFacts, allConditions, parserContext);

				if (!conditionIsOk) {
					LOGGER.debug("Given player {}, move {} is not allowed", playerId, m);
				}

				return conditionIsOk;
			}).collect(Collectors.toList());

		}

		if (playedAllowedMoves.isEmpty()) {
			// No need to iterate through the board
			return Collections.emptyList();
		}

		// TODO Remove early any move with require a piece which does not exist at all on the board
		board.forEach(coordinate -> {
			LOGGER.debug("Considering: player={} coordinate={}", playerId, coordinate);
			availableActions.addAll(nextPossibleActions(currentState, playerId, facts, coordinate, playedAllowedMoves));
		});

		// May be empty when waiting for an external events: e.g. during an animation
		return availableActions;
	}

	public List<IAgentChoice> nextPossibleActions(GameState currentState,
			String playerId,
			Facts originalFacts,
			IPlateauCoordinate coordinate) {
		return nextPossibleActions(currentState, playerId, originalFacts, coordinate, allowedMoves);
	}

	public List<IAgentChoice> nextPossibleActions(GameState currentState,
			String playerId,
			Facts originalFacts,
			IPlateauCoordinate coordinate,
			List<Map<String, ?>> movesToConsider) {
		Facts coordinatesFacts = GameModelHelpers.cloneFacts(originalFacts);
		coordinate.asMap().forEach(coordinatesFacts::put);

		coordinatesFacts.put("player", playerId);

		// Reject early if none of these coordinates is valid
		{

			List<String> allConditions =
					ImmutableList.<String>builder().addAll(getGenericConditions(coordinatesFacts)).build();

			boolean conditionIsOk = GameModelHelpers.unsafeLogicalAnd(coordinatesFacts, allConditions, parserContext);

			if (!conditionIsOk) {
				// Given the board coordinate, not a single move is eligible
				return Collections.emptyList();
			}
		}

		// Check if at least one move would be compatible with this coordinate
		{
			List<String> allConditions = ImmutableList.<String>builder().addAll(allMovesConditions).build();

			boolean conditionIsOk = GameModelHelpers.unsafeLogicalOr(coordinatesFacts, allConditions, parserContext);

			if (!conditionIsOk) {
				// Given the board coordinate, not a single move is eligible
				return Collections.emptyList();
			}
		}

		return allowedMoves.stream().flatMap(move -> {
			Optional<String> optComment = PepperMapHelper.getOptionalString(move, "comment");
			LOGGER.debug("Consider move: {}", optComment.orElse("-"));
			// TODO Group allowedMoves by player clause, in order to discard irrelevant moves early
			// RegEx: player.equals('b')

			List<String> conditions = PepperMapHelper.getRequiredAs(move, "conditions");

			// Check if at least this move would be compatible with this coordinate
			{
				List<String> allConditions = ImmutableList.<String>builder().addAll(allMovesConditions).build();

				boolean conditionIsOk =
						GameModelHelpers.unsafeLogicalOr(coordinatesFacts, allConditions, parserContext);

				if (!conditionIsOk) {
					// Given the board coordinate, the move is not compatible
					return Stream.empty();
				}
			}

			// Intermediates should not edit the GameState, but only introduce intermediate variables to help defining a
			// new state
			// Some of these variables may be a range type (e.g. any integer in '[0;10[')
			List<String> intermediates = PepperMapHelper.getRequiredAs(move, KEY_INTERMEDIATE);

			SingleAndRangeMutations intermediatesSplitted =
					SingleAndRangeMutations.from(coordinatesFacts, intermediates);

			Facts factsWithPointIntemediates =
					GameModelHelpers.applyPointMutators(coordinatesFacts, intermediatesSplitted.getPointMutations());

			List<String> mutations = PepperMapHelper.getRequiredAs(move, "mutations");

			// Mutate with intermediate/hidden variables
			return GameModelHelpers
					.applyRangeMutators(factsWithPointIntemediates, intermediatesSplitted.getRangeMutations())
					.stream()
					.flatMap(agentOptionFacts -> {
						{
							List<String> allConditions = ImmutableList.<String>builder()
									.addAll(getGenericConditions(agentOptionFacts))
									.addAll(conditions)
									.build();

							boolean conditionIsOk =
									GameModelHelpers.logicalAnd(agentOptionFacts, allConditions, parserContext);

							if (!conditionIsOk) {
								return Stream.empty();
							}
						}

						List<String> singleValueIntermediates =
								resolveRangeInIntermediates(coordinatesFacts, intermediates, agentOptionFacts);

						// Beware there might be intermediates which should be overriden by
						return Stream.of(new AgentChoice(playerId, coordinate, mutations, singleValueIntermediates));
					});
		}).collect(Collectors.toList());
	}

	private List<? extends String> getGenericConditions(Facts coordinatesFacts) {
		return constrainsToConditions(gameInfo.getConstrains(), coordinatesFacts.asMap().keySet());
	}

	private List<String> resolveRangeInIntermediates(Facts originalFacts,
			List<String> intermediates,
			Facts enrichedFacts) {
		AtomicReference<Facts> mutatingFacts = new AtomicReference<>(originalFacts);

		// TODO e.g. handle more complex case like 'p in [-1,1] and q=2*p'
		List<String> singleValueIntermediates = new ArrayList<>();
		intermediates.forEach(intermediate -> {
			List<Facts> options = GameModelHelpers.applyRangeMutators(mutatingFacts.get(), Arrays.asList(intermediate));
			if (options.size() >= 2) {
				Map<String, ?> mutatingMutatorHidden = hideMutator(mutatingFacts.get().asMap());
				Map<String, ?> hiddenEnriched = hideMutator(enrichedFacts.asMap());

				List<Facts> relevantOptions = options.stream().filter(o -> {
					Map<String, ?> hiddenOption = hideMutator(o.asMap());
					boolean containsAll = hiddenOption.entrySet().containsAll(mutatingMutatorHidden.entrySet());
					boolean containsAll2 = hiddenEnriched.entrySet().containsAll(hiddenOption.entrySet());

					return containsAll && containsAll2;
				}).collect(Collectors.toList());

				if (relevantOptions.size() != 1) {
					throw new IllegalArgumentException("Not handled: " + options);
				}

				// This is a range intermediate
				Facts relevantOption = relevantOptions.get(0);
				MapDifference<String, Object> diff =
						Maps.difference(mutatingMutatorHidden, hideMutator(relevantOption.asMap()));
				// Set<String> keySet = .keySet();
				if (diff.entriesOnlyOnRight().size() != 1) {
					throw new IllegalArgumentException("Not handled: " + diff);
				}
				String key = diff.entriesOnlyOnRight().keySet().iterator().next();
				Object valueSelected = enrichedFacts.get(key);
				singleValueIntermediates.add("mutator.put('" + key + "'," + valueSelected + ")");
				mutatingFacts.set(relevantOption);
			} else {
				// This is a single value mutator
				singleValueIntermediates.add(intermediate);
				mutatingFacts.set(options.get(0));
			}
		});
		return singleValueIntermediates;
	}

	private Map<String, ?> hideMutator(Map<String, Object> asMap) {
		Map<String, Object> clone = new LinkedHashMap<>(asMap);

		clone.remove(GameModelHelpers.KEY_MUTATOR);
		clone.remove("map");

		return clone;
	}

	/**
	 * We restrict constrain to relevant variables, as it is meaning-less to check x>= is 0 does not exist in current
	 * context.
	 * 
	 * @param constrains
	 * @param usedVariables
	 * @return
	 */
	private List<? extends String> constrainsToConditions(List<Map<String, ?>> constrains, Set<String> usedVariables) {

		return constrains.stream().flatMap(constrain -> {
			List<String> variables = PepperMapHelper.getRequiredAs(constrain, "variables");

			Set<String> relevantVariables =
					Sets.intersection(usedVariables, variables.stream().collect(Collectors.toSet()));

			int min = PepperMapHelper.getRequiredNumber(constrain, "minIncluded").intValue();
			int max = PepperMapHelper.getRequiredNumber(constrain, "maxExcluded").intValue();

			return relevantVariables.stream().flatMap(v -> Stream.of(v + ">=" + min, v + "<" + max));
		}).collect(Collectors.toList());
	}

	public Facts makeFacts(GameState state) {
		Facts facts = new Facts();

		constants.forEach(facts::put);

		facts.put("map", new GameMapInterpreter(state));
		facts.put("board", new BoardInterpreter(board));

		// Typically used to set the current player
		state.getMetadata().forEach(facts::put);

		return facts;
	}

	/**
	 * This should be called only when the game is over (what ever the reason (game rules, a player is not responding, a
	 * player resigned, ...))
	 * 
	 * @param playerIdToAgent
	 * @param currentState
	 * @param availableChoices
	 */
	public void notifyGameOverToPlayers(Map<String, IGameAgent> playerIdToAgent,
			GameState currentState,
			ListMultimap<String, IAgentChoice> availableChoices) {
		GameChoiceInterpreter interpreter = new GameChoiceInterpreter(availableChoices);

		Facts baseFacts = this.makeFacts(currentState);
		baseFacts.put("moves", interpreter);

		playerIdToAgent.forEach((playerId, agent) -> {
			// List<IAgentChoice> choices = availableChoices.get(playerId);

			AtomicBoolean lose = new AtomicBoolean();
			AtomicBoolean win = new AtomicBoolean();

			gameInfo.getGameoverConditions().forEach(c -> {
				if (lose.get() || win.get()) {
					// We must not consider additional rules
					return;
				}

				List<String> conditions = PepperMapHelper.getRequiredAs(c, "conditions");

				Facts playerFacts = GameModelHelpers.cloneFacts(baseFacts);
				playerFacts.put("player", playerId);

				Set<String> variables = playerFacts.asMap().keySet();

				List<String> allConditions = ImmutableList.<String>builder()
						.addAll(constrainsToConditions(gameInfo.getConstrains(), variables))
						.addAll(conditions)
						.build();

				boolean trigger = GameModelHelpers.logicalAnd(playerFacts, allConditions, parserContext);

				if (trigger) {

				}
			});

			// List<String> conditions = PepperMapHelper.getRequiredAs(c, "conditions");
			//
			// Set<String> variables = facts.asMap().keySet();
			//
			// List<String> winningPlayers = gameInfo.getPlayers().stream().map(PlayerPojo::getId).filter(player -> {
			// Facts playerFacts = cloneFacts(facts);
			// playerFacts.put("player", player);
			//
			// List<String> allConditions = ImmutableList.<String>builder()
			// .addAll(constrainsToConditions(gameInfo.getConstrains(), variables))
			// .addAll(conditions)
			// .build();
			//
			// return logicalAnd(playerFacts, allConditions, parserContext);
			// }).collect(Collectors.toList());

		});
	}

	public List<? extends IAgentChoice> debugWhyNotPossible(GameState currentState,
			String playerId,
			IPlateauCoordinate coordinate,
			Map<String, ?> move) {
		Facts originalFacts = makeFacts(currentState);

		Facts coordinatesFacts = GameModelHelpers.cloneFacts(originalFacts);
		coordinate.asMap().forEach(coordinatesFacts::put);

		coordinatesFacts.put("player", playerId);

		List<String> conditions = PepperMapHelper.getRequiredAs(move, "conditions");

		// Intermediates should not edit the GameState, but only introduce intermediate variables to help defining a
		// new state
		// Some of these variables may be a range type (e.g. any integer in '[0;10[')
		List<String> intermediates = PepperMapHelper.getRequiredAs(move, KEY_INTERMEDIATE);

		SingleAndRangeMutations intermediatesSplitted = SingleAndRangeMutations.from(coordinatesFacts, intermediates);

		Facts factsWithPointIntemediates =
				GameModelHelpers.applyPointMutators(coordinatesFacts, intermediatesSplitted.getPointMutations());

		List<String> mutations = PepperMapHelper.getRequiredAs(move, "mutations");

		// Mutate with intermediate/hidden variables
		return GameModelHelpers
				.applyRangeMutators(factsWithPointIntemediates, intermediatesSplitted.getRangeMutations())
				.stream()
				.map(agentOptionFacts -> {
					List<String> allConditions = ImmutableList.<String>builder()
							.addAll(getGenericConditions(agentOptionFacts))
							.addAll(conditions)
							.build();

					Optional<String> notEvaluated =
							GameModelHelpers.filterNotEvaluated(agentOptionFacts, allConditions, parserContext);

					if (notEvaluated.isEmpty()) {
						List<String> singleValueIntermediates =
								resolveRangeInIntermediates(coordinatesFacts, intermediates, agentOptionFacts);

						// Beware there might be intermediates which should be overriden by
						AgentChoice choice = new AgentChoice(playerId, coordinate, mutations, singleValueIntermediates);

						GameState toState = applyChoice(currentState, choice);

						LOGGER.info("OK to apply: {}{}. It would lead to: {}{}",
								System.lineSeparator(),
								choice,
								System.lineSeparator(),
								toState);

						return Optional.of(choice);
					} else {
						Map<String, Object> cleanFacts = new LinkedHashMap<>(agentOptionFacts.asMap());

						cleanFacts.remove("map");
						cleanFacts.remove("board");
						cleanFacts.remove(GameModelHelpers.KEY_MUTATOR);

						// TODO Do not try applying the mutations as the move is not eligible: it may even be non-sense
						LOGGER.info("KO on {} ({}) due to {}", cleanFacts, mutations, notEvaluated);

						return Optional.<IAgentChoice>empty();
					}
				})
				.flatMap(Optional::stream)
				.collect(Collectors.toList());
	}
}
