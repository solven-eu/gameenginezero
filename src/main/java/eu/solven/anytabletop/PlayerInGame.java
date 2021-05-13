package eu.solven.anytabletop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represent the player in game, i.e. the state of the player in the game model
 * 
 * @author Benoit Lacelle
 *
 */
public class PlayerInGame {
	final Map<String, Object> state = new LinkedHashMap<>();

	final AtomicBoolean gameOver = new AtomicBoolean();

}
