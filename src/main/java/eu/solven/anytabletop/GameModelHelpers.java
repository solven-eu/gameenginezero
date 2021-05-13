package eu.solven.anytabletop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.mvel.MVELAction;
import org.jeasy.rules.mvel.MVELCondition;
import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import eu.solven.anytabletop.rules.FactMutator;

public class GameModelHelpers {
	public static final String KEY_MUTATOR = "mutator";

	private static final Logger LOGGER = LoggerFactory.getLogger(GameModelHelpers.class);

	private static final ParserContext parserContext = new ParserContext();

	protected GameModelHelpers() {
		// hidden
	}

	public static Facts cloneFacts(Facts facts) {
		Facts mutatedFacts = new Facts();

		// Copy current state before mutations
		facts.asMap().forEach(mutatedFacts::put);

		if (mutatedFacts.asMap().containsKey(KEY_MUTATOR)) {
			enableMutations(mutatedFacts);
		}

		return mutatedFacts;
	}

	public static boolean logicalAnd(Facts facts, Collection<String> conditions, ParserContext parserContext) {
		return filterNotEvaluated(facts, conditions, parserContext).isEmpty();
	}

	public static Optional<String> filterNotEvaluated(Facts facts,
			Collection<String> conditions,
			ParserContext parserContext) {
		// We return only the first not evaluated condition, as remaining conditions may not be evaluatable (e.g. if we
		// are consider a coordinate our of the board, we should not try checking the value as given position)
		return conditions.stream().filter(condition -> {
			MVELCondition c = new MVELCondition(condition, parserContext);

			boolean evaluated;
			try {
				evaluated = c.evaluate(facts);
			} catch (RuntimeException e) {
				throw new RuntimeException("Issue with condition: " + condition, e);
			}

			return !evaluated;
		}).findAny();
	}

	/**
	 * 
	 * @return true if any condition is true or not computable: it is useful to compute conditions early (i.e. before
	 *         its full context is available)
	 */
	public static boolean unsafeLogicalAnd(Facts facts, Collection<String> conditions, ParserContext parserContext) {
		boolean conditionIsOk = true;

		for (String condition : conditions) {
			MVELCondition c = new MVELCondition(condition, parserContext);

			boolean evaluated;
			try {
				evaluated = c.evaluate(facts);
			} catch (PropertyAccessException e) {
				// This may happen on condition relying on facts not already present in current context
				LOGGER.trace("This condition (" + condition + ") is not yet evaluatable", e);
				evaluated = true;
			} catch (RuntimeException e) {
				// This may happen on WILDCARDS
				LOGGER.trace("This condition (" + condition + ") is not yet evaluatable", e);
				evaluated = true;
			}
			if (!evaluated) {
				conditionIsOk = false;
				break;
			}
		}
		return conditionIsOk;
	}

	public static boolean unsafeLogicalOr(Facts facts, Collection<String> conditions, ParserContext parserContext) {
		boolean conditionIsOk = false;

		for (String condition : conditions) {
			MVELCondition c = new MVELCondition(condition, parserContext);

			boolean evaluated;
			try {
				evaluated = c.evaluate(facts);
			} catch (PropertyAccessException e) {
				LOGGER.trace("This condition (" + condition + ") is not yet evaluatable", e);
				evaluated = false;
			} catch (RuntimeException e) {
				throw new RuntimeException("Issue with condition: " + condition, e);
			}
			if (evaluated) {
				conditionIsOk = true;
				break;
			}
		}
		return conditionIsOk;
	}

	public static Facts applyPointMutators(Facts facts, List<String> mutations) {
		Facts mutatedFacts = cloneFacts(facts);

		enableMutations(mutatedFacts);

		for (String mutation : mutations) {
			MVELAction action;
			try {
				action = new MVELAction(mutation, parserContext);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Issue parsing mutation: [[" + mutation + "]]", e);
			}

			try {
				action.execute(mutatedFacts);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Issue executing mutation: [[" + mutation + "]]", e);
			}
		}

		mutatedFacts.remove(KEY_MUTATOR);

		mutatedFacts.asMap()
				.values()
				.stream()
				.filter(o -> o instanceof List<?> || o instanceof int[])
				.findAny()
				.ifPresent(o -> {
					throw new IllegalArgumentException("Illegal state: a point mutation lead to a rangeFacts: o=" + o);
				});

		return mutatedFacts;
	}

	// Some mutators will generate a set of possible outputs
	public static List<Facts> applyRangeMutators(Facts facts, List<String> mutations) {
		List<Facts> outputFacts = new ArrayList<>();

		{
			Facts enrichedFacts = cloneFacts(facts);

			// Add a mutator, enabling mutation
			enableMutations(enrichedFacts);

			// Consider the input facts as the initial template to which range mutations will be applied
			outputFacts.add(enrichedFacts);
		}

		for (String mutation : mutations) {
			MVELAction action;
			try {
				action = new MVELAction(mutation, parserContext);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Issue parsing mutation: [[" + mutation + "]]", e);
			}

			List<Facts> outputfacts2 = new ArrayList<>();

			outputFacts.forEach(rawF -> {
				Facts f = cloneFacts(rawF);

				try {
					action.execute(f);
				} catch (RuntimeException e) {
					throw new IllegalArgumentException("Issue executing mutation: [[" + mutation + "]]", e);
				}

				List<Fact<?>> simpleFacts = new ArrayList<>();
				Map<String, List<?>> listFacts = new LinkedHashMap<>();
				f.forEach(fact -> {
					Object factValue = fact.getValue();
					if (factValue instanceof List<?>) {
						listFacts.put(fact.getName(), (List<?>) factValue);
					} else if (factValue instanceof int[]) {
						listFacts.put(fact.getName(), Ints.asList((int[]) factValue));
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
						Facts clone = cloneFacts(f);

						simpleFacts.forEach(clone::add);

						tuple.forEach(e -> clone.put(e.getKey(), e.getValue()));

						outputfacts2.add(clone);
					});
				}
			});

			outputFacts.clear();
			outputFacts.addAll(outputfacts2);
		}
		return outputFacts;
	}

	public static void enableMutations(Facts facts) {
		facts.put(KEY_MUTATOR, new FactMutator(facts));
	}
}
