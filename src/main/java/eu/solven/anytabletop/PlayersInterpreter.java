package eu.solven.anytabletop;

import eu.solven.anytabletop.state.GameState;

/**
 * Represent the player in game, i.e. the state of the player in the game model
 * 
 * @author Benoit Lacelle
 *
 */
public class PlayersInterpreter {
	final GameState gameState;

	public PlayersInterpreter(GameState gameState) {
		this.gameState = gameState;
	}

}
