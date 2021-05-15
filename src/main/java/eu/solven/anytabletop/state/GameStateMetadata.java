package eu.solven.anytabletop.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

import eu.solven.anytabletop.GameModel;

public class GameStateMetadata {
	final Map<String, Object> customMetadata;
	final Map<String, Map<String, Object>> playersMetadata;

	public GameStateMetadata(Map<String, Object> metadata, Map<String, Map<String, Object>> playersMetadata) {
		this.customMetadata = metadata;
		this.playersMetadata = playersMetadata;

		if (metadata.containsKey(GameModel.KEY_PLAYER)) {
			throw new IllegalArgumentException(
					"It is forbidden to have a custom metdata named: " + GameModel.KEY_PLAYER);
		}
	}

	public Map<String, Object> getCustomMetadata() {
		return customMetadata;
	}

	public Map<String, Map<String, Object>> getPlayersMetadata() {
		return playersMetadata;
	}

	@Override
	public int hashCode() {
		return Objects.hash(customMetadata, playersMetadata);
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
		GameStateMetadata other = (GameStateMetadata) obj;
		return Objects.equals(customMetadata, other.customMetadata)
				&& Objects.equals(playersMetadata, other.playersMetadata);
	}

	public static GameStateMetadata fromMap(Map<String, ?> metadata) {
		Map<String, ?> inputCustom = (Map<String, ?>) metadata.get("custom");
		Map<String, Object> customMetadata;
		if (inputCustom == null) {
			customMetadata = new LinkedHashMap<>();
		} else {
			customMetadata = new LinkedHashMap<>(inputCustom);
		}

		Map<String, Map<String, ?>> intputPlayersMetadata = (Map<String, Map<String, ?>>) metadata.get("players");

		Map<String, Map<String, Object>> playersMetadata;
		if (intputPlayersMetadata == null) {
			playersMetadata = new LinkedHashMap<>();
		} else {
			playersMetadata = new LinkedHashMap<>(intputPlayersMetadata.size());
			intputPlayersMetadata.forEach((player, m) -> playersMetadata.put(player, new LinkedHashMap<>(m)));
		}

		return new GameStateMetadata(customMetadata, playersMetadata);
	}

	public static Map<String, ?> toMap(GameStateMetadata metadata) {
		return ImmutableMap.of("custom", metadata.getCustomMetadata(), "players", metadata.getPlayersMetadata());
	}

}
