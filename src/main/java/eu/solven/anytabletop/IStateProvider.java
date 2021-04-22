package eu.solven.anytabletop;

import java.util.Map;

public interface IStateProvider {

	GameState generateInitialState();

	GameState loadState(Map<String, ?> setup);

}
