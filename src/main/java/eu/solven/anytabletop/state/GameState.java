package eu.solven.anytabletop.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameState {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameState.class);

	final String state;
	final GameStateMetadata metadata;

	public GameState(String state, Map<String, ?> metadata) {
		this.state = state;
		this.metadata = GameStateMetadata.fromMap(metadata);
	}

	public GameState(String state, GameStateMetadata metadata) {
		this.state = state;
		this.metadata = metadata;
	}

	public Map<String, ?> getCustomMetadata() {
		return metadata.getCustomMetadata();
	}

	public Map<String, Map<String, Object>> getPlayersMetadata() {
		return metadata.getPlayersMetadata();
	}

	public GameStateMetadata getMetadata() {
		return metadata;
	}

	public Map<String, ?> getMetadataAsMap() {
		return GameStateMetadata.toMap(metadata);
	}

	public String getState() {
		return state;
	}

	public String toString() {
		String string = "";

		if (!metadata.getCustomMetadata().isEmpty()) {
			string += "custom:" + System.lineSeparator();
			for (Map.Entry<String, ?> e : metadata.getCustomMetadata().entrySet()) {
				String k = e.getKey();
				Object v = e.getValue();
				string += k + ": " + v + System.lineSeparator();
			}
		}

		if (!metadata.getPlayersMetadata().isEmpty()) {
			string += "players:" + System.lineSeparator();
			for (Map.Entry<String, Map<String, Object>> e : metadata.getPlayersMetadata().entrySet()) {
				String k = e.getKey();
				Map<String, Object> v = e.getValue();
				string += k + ": " + v + System.lineSeparator();
			}
		}

		string += "new state: " + System.lineSeparator() + state;
		return string;
	}

	public void diffTo(GameState to) {
		LOGGER.info("from: {}", this);
		LOGGER.info("to: {}", to);
	}

	/**
	 * This is useful for tests where a state may not be fully expressed
	 * 
	 * @param other
	 * @return
	 */
	public boolean containsState(GameState other) {
		if (!this.state.equals(other.getState())) {
			return false;
		}

		if (!this.metadata.getCustomMetadata().entrySet().containsAll(other.getCustomMetadata().entrySet())) {
			return false;
		}

		Optional<Entry<String, Map<String, Object>>> playerMetadataNotContained =
				this.metadata.getPlayersMetadata().entrySet().stream().filter(e -> {
					Map<String, Object> otherPlayerMetadata = other.getPlayersMetadata().get(e.getKey());

					return !e.getValue().entrySet().containsAll(otherPlayerMetadata.entrySet());
				}).findAny();

		return playerMetadataNotContained.isEmpty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(metadata, state);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		GameState other = (GameState) obj;
		return Objects.equals(metadata, other.metadata) && Objects.equals(state, other.state);
	}

	public void setPlayerGameOver(String player) {
		Map<String, Object> playerMetadata = metadata.getPlayersMetadata().get(player);
		if (playerMetadata == null) {
			playerMetadata = new LinkedHashMap<>();
			metadata.getPlayersMetadata().put(player, playerMetadata);
		}

		playerMetadata.put("game_over", Boolean.TRUE);
	}

}
