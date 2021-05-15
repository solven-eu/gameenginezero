package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.solven.anytabletop.map.BoardFromMap;
import eu.solven.anytabletop.state.GameState;
import eu.solven.anytabletop.state.GameStateMetadata;

public class GameMapInterpreter {
	final GameState originalState;
	final AtomicReference<GameState> mutatedState;

	public GameMapInterpreter(GameState state) {
		this.originalState = state;
		this.mutatedState = new AtomicReference<>(state);
	}

	public GameState getLatestState() {
		return mutatedState.get();
	}

	@Override
	public String toString() {
		return "GameMapInterpreter";
	}

	// x is the usual mathematical x: it grows by going right
	// y is the usual mathematical y: it grows by going up
	public char charAt(int x, int y) {
		GameState latest = getLatestState();

		String[] rows = latest.getState().split("[\r\n]+");
		int rowIndex = rows.length - y - 1;

		if (rowIndex < 0 || rowIndex >= rows.length) {
			throw new IllegalArgumentException("Invalid y=" + y);
		}

		String row = rows[rowIndex];

		if (x < 0 || x > row.length()) {
			throw new IllegalArgumentException("Invalid x=" + x);
		}
		return row.charAt(x);
	}

	public char charAt(String x, String y) {
		if (!BoardFromMap.WILDCARD.equals(y)) {
			throw new IllegalStateException("Invalid x: " + x);
		}
		if (!BoardFromMap.WILDCARD.equals(x)) {
			throw new IllegalStateException("Invalid y: " + y);
		}

		throw new UnsupportedOperationException("TODO");
	}

	public void charAt(int x, int y, char c) {
		GameState latest = getLatestState();

		String[] rows = latest.getState().split("[\r\n]+");

		int rowIndex = rows.length - y - 1;
		rows[rowIndex] = rows[rowIndex].substring(0, x) + c + rows[rowIndex].substring(x + 1);

		String boardState = Stream.of(rows).collect(Collectors.joining(System.lineSeparator()));
		mutatedState.set(new GameState(boardState, latest.getMetadataAsMap()));
	}

	public void updateMetadata(String key, Object value) {
		GameState latestState = mutatedState.get();

		Map<String, Object> updatedMetadata = new LinkedHashMap<>();
		updatedMetadata.putAll(latestState.getCustomMetadata());
		updatedMetadata.put(key, value);

		GameStateMetadata gameStateMetadata = new GameStateMetadata(updatedMetadata, latestState.getPlayersMetadata());
		mutatedState.set(new GameState(latestState.getState(), gameStateMetadata));
	}
}
