package anytabletop.unittests;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AllowedTransitions {

	protected final List<Map<String, ?>> allowedTransitions;
	protected final List<Map<String, ?>> forbiddenTransitions;
	protected final List<Map<String, ?>> gameovers;

	@JsonCreator
	public AllowedTransitions(@JsonProperty("allowed_transitions") List<Map<String, ?>> allowedTransitions,
			@JsonProperty("forbidden_transitions") List<Map<String, ?>> forbiddenTransitions,
			@JsonProperty("gameovers") List<Map<String, ?>> gameovers) {
		this.allowedTransitions = Optional.ofNullable(allowedTransitions).orElse(Collections.emptyList());
		this.forbiddenTransitions = Optional.ofNullable(forbiddenTransitions).orElse(Collections.emptyList());
		this.gameovers = Optional.ofNullable(gameovers).orElse(Collections.emptyList());
	}

	public List<Map<String, ?>> getAllowedTransitions() {
		return allowedTransitions;
	}

	public List<Map<String, ?>> getForbiddenTransitions() {
		return forbiddenTransitions;
	}

	public List<Map<String, ?>> getGameovers() {
		return gameovers;
	}
}
