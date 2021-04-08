package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	// x is the usual mathematical x: it grows by going right
	// y is the usual mathematical y: it grows by going up
	public char charAt(int x, int y) {
		GameState latest = getLatestState();

		String[] rows = latest.getState().split("[\r\n]+");
		return rows[rows.length - y - 1].charAt(x);
	}

	public void charAt(int x, int y, char c) {
		GameState latest = getLatestState();

		String[] rows = latest.getState().split("[\r\n]+");

		int rowIndex = rows.length - y - 1;
		rows[rowIndex] = rows[rowIndex].substring(0, x) + c + rows[rowIndex].substring(x + 1);

		mutatedState.set(new GameState(Stream.of(rows).collect(Collectors.joining(System.lineSeparator())),
				latest.getMetadata()));
	}

	public void updateMetadata(String key, Object value) {
		GameState latestState = mutatedState.get();

		Map<String, Object> updatedMetadata = new LinkedHashMap<>();
		updatedMetadata.putAll(latestState.getMetadata());
		updatedMetadata.put(key, value);

		mutatedState.set(new GameState(latestState.getState(), updatedMetadata));
	}
}
