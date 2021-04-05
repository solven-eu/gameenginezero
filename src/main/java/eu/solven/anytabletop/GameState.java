package eu.solven.anytabletop;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameState {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameState.class);

	final String state;
	final Map<String, ?> metadata;

	public GameState(String state, Map<String, ?> metadata) {
		this.state = state;
		this.metadata = metadata;
	}

	public Map<String, ?> getMetadata() {
		return metadata;
	}

	public String getState() {
		return state;
	}

	public String toString() {
		String string = "";

		for (Map.Entry<String, ?> e : metadata.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			string += k + ": " + v + System.lineSeparator();
		}

		string += "new state: " + System.lineSeparator() + state;
		return string;
	}

	public void diffTo(GameState to) {
		LOGGER.info("from: {}", this);
		LOGGER.info("to: {}", to);
	};
}
