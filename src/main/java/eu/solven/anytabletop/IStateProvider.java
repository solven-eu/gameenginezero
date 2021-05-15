package eu.solven.anytabletop;

import java.util.Map;

import eu.solven.anytabletop.state.GameState;

public interface IStateProvider {

	GameState generateInitialState();

	GameState loadState(Map<String, ?> setup);

}
