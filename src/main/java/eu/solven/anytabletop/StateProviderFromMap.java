package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.state.GameState;

public class StateProviderFromMap implements IStateProvider {
	private static final String EOL = System.lineSeparator();

	final GameInfo gameInfo;

	public StateProviderFromMap(GameInfo gameInfo) {
		this.gameInfo = gameInfo;
	}

	@Override
	public GameState generateInitialState() {
		Map<String, ?> setup = gameInfo.getSetup();

		return loadState(setup);
	}

	@Override
	public GameState loadState(Map<String, ?> setup) {
		String plateau = ((List<String>) setup.get("board")).stream().collect(Collectors.joining(EOL));

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.putAll(gameInfo.getConstants());

		// Optionally as may be missing on unit-tests
		PepperMapHelper.<Map<String, ?>>getOptionalAs(setup, "metadata")
				.ifPresent(setupMetadata -> metadata.putAll(setupMetadata));

		return new GameState(plateau, ImmutableMap.copyOf(metadata));
	}

}
