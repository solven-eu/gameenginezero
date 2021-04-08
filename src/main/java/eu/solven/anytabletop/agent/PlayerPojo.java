package eu.solven.anytabletop.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerPojo {
	final String id;

	@JsonCreator
	public PlayerPojo(@JsonProperty("id") String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
