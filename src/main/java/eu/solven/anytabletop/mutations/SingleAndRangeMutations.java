package eu.solven.anytabletop.mutations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.mvel.MVELAction;

import eu.solven.anytabletop.GameModelHelpers;
import eu.solven.anytabletop.easyrules.EasyRulesHelper;

public class SingleAndRangeMutations {
	protected final List<String> pointMutations;
	protected final List<String> rangeMutations;

	protected SingleAndRangeMutations(List<String> pointMutations, List<String> rangeMutations) {
		this.pointMutations = pointMutations;
		this.rangeMutations = rangeMutations;
	}

	public static SingleAndRangeMutations from(Facts baseFacts, List<String> mutations) {
		// We clone as we need to apply intermediate mutation to know which one are range mutations
		Facts clone = GameModelHelpers.cloneFacts(baseFacts);

		GameModelHelpers.enableMutations(clone);

		List<String> pointMutations = new ArrayList<>();
		List<String> rangeMutations = new ArrayList<>();

		// BEWARE: we consider as a rangeMutation the first actually range mutation, and all the following ones. Indeed,
		// there is no much use in all point mutations, as a pointMutation after a rangeMutation is not applyable as it
		// probably depends on the previous rangeMutation
		AtomicBoolean isRange = new AtomicBoolean();

		mutations.forEach(mutation -> {
			MVELAction action = EasyRulesHelper.parseActionWithCache(mutation);

			try {
				action.execute(clone);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Issue executing mutation: [[" + mutation + "]]", e);
			} catch (IncompatibleClassChangeError e) {
				// We may observe since relying on a cache:
				// java.lang.IncompatibleClassChangeError: Method 'java.util.stream.IntStream
				// java.util.stream.IntStream.range(int, int)' must be InterfaceMethodref constant
				// TODO This is dangerous as we may end applying twice the same mutation to the same fact
				action = EasyRulesHelper.parseActionWithoutCache(mutation);
				action.execute(clone);
			}

			Map<String, Object> pointToApply = new LinkedHashMap<>();

			clone.forEach(fact -> {
				String factName = fact.getName();
				Object factValue = fact.getValue();

				if (factValue instanceof List<?>) {
					isRange.set(true);

					// Choose the first option to continue working over a pointFact
					List<?> factValueAsList = (List<?>) factValue;
					if (factValueAsList.isEmpty()) {
						throw new IllegalStateException("A fact value is empty for k=" + factName);
					}
					pointToApply.put(factName, factValueAsList.get(0));
				} else if (factValue instanceof int[]) {
					isRange.set(true);

					// Choose the first option to continue working over a pointFact
					int[] factValueAsArray = (int[]) factValue;
					if (factValueAsArray.length == 0) {
						throw new IllegalStateException("A fact value is empty for k=" + factName);
					}
					pointToApply.put(factName, factValueAsArray[0]);
				}
			});

			pointToApply.forEach(clone::put);

			if (isRange.get()) {
				rangeMutations.add(mutation);
			} else {
				pointMutations.add(mutation);
			}
		});

		return new SingleAndRangeMutations(pointMutations, rangeMutations);
	}

	public List<String> getPointMutations() {
		return pointMutations;
	}

	public List<String> getRangeMutations() {
		return rangeMutations;
	}
}
