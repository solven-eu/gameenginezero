package anytabletop;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.jeasy.rules.api.Facts;
import org.junit.jupiter.api.Test;

import eu.solven.anytabletop.GameModelHelpers;

public class TestGameModel {

	@Test
	public void testApplyMutators_singleRange() {
		Facts facts = new Facts();
		facts.put("someName", "someValue");

		List<String> mutations = new ArrayList<>();
		mutations.add("mutator.put('p',java.util.Arrays.asList(1,-1))");

		List<Facts> output = GameModelHelpers.applyRangeMutators(facts, mutations);

		Assertions.assertThat(output).hasSize(2);
	}

	@Test
	public void testApplyMutators_doubleRange() {
		Facts facts = new Facts();
		facts.put("someName", "someValue");

		List<String> mutations = new ArrayList<>();
		mutations.add("mutator.put('p',java.util.Arrays.asList(1,-1))");
		mutations.add("mutator.put('q',java.util.Arrays.asList(1,-1))");

		List<Facts> output = GameModelHelpers.applyRangeMutators(facts, mutations);

		Assertions.assertThat(output).hasSize(4);
	}
}
