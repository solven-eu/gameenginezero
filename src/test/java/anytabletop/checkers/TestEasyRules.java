package anytabletop.checkers;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.mvel.MVELAction;
import org.junit.jupiter.api.Test;

import eu.solven.anytabletop.easyrules.EasyRulesHelper;

public class TestEasyRules {

	// We encountered a strange java Error with such an expression
	@Test
	public void testRulesOverIntStream() {
		{
			MVELAction action =
					EasyRulesHelper.parseActionWithCache("java.util.stream.IntStream.range(1, 2).toArray()");

			Facts facts = new Facts();
			action.execute(facts);
		}
		{
			MVELAction action =
					EasyRulesHelper.parseActionWithCache("java.util.stream.IntStream.range(1, 2).toArray()");

			Facts facts = new Facts();
			action.execute(facts);
		}
	}
}
