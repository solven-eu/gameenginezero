package eu.solven.anytabletop.rules;

import org.jeasy.rules.api.Facts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FactMutator {

	private static final Logger LOGGER = LoggerFactory.getLogger(FactMutator.class);

	final Facts facts;

	public FactMutator(Facts facts) {
		this.facts = facts;
	}

	public void put(String name, Object value) {
		LOGGER.trace(".put({},{})", name, value);
		facts.put(name, value);
	}

}
