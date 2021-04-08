package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

public class StateProviderFromMap implements IStateProvider {
	private static final String EOL = System.lineSeparator();

	final GameInfo gameInfo;

	public StateProviderFromMap(GameInfo gameInfo) {
		this.gameInfo = gameInfo;
	}

	@Override
	public GameState generateInitialState() {
		String plateau = ((List<String>) gameInfo.getSetup().get("board")).stream().collect(Collectors.joining(EOL));

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.putAll(gameInfo.getConstants());
		metadata.put("player", gameInfo.getSetup().get("player"));

		return new GameState(plateau, ImmutableMap.copyOf(metadata));
	}

}
