package anytabletop.unittests;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AllowedTransitions {

	protected final List<Map<String, ?>> allowedTransitions;
	protected final List<Map<String, ?>> forbiddenTransitions;

	@JsonCreator
	public AllowedTransitions(@JsonProperty("allowed_transitions") List<Map<String, ?>> allowedTransitions,
			@JsonProperty("forbidden_transitions") List<Map<String, ?>> forbiddenTransitions) {
		this.allowedTransitions = allowedTransitions;
		this.forbiddenTransitions = forbiddenTransitions;
	}

	public List<Map<String, ?>> getAllowedTransitions() {
		return allowedTransitions;
	}

	public List<Map<String, ?>> getForbiddenTransitions() {
		return forbiddenTransitions;
	}
}
