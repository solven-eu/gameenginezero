package eu.solven.anytabletop.map;

import java.util.function.Consumer;

import org.jeasy.rules.api.Facts;

import eu.solven.anytabletop.IPlateauCoordinate;

public interface IBoard {
	String getType();

	int getIntProperty(String key);

	void forEach(Consumer<IPlateauCoordinate> coordinateConsumer);

	/**
	 * Used to detect early if a condition is valid given the board state (e.g. it may prevent iterating through the
	 * board)
	 * 
	 * @param facts
	 */
	@Deprecated(since = "This requires changing from 'map.charAt(x) == p' to 'map.charAt(x).is(p)'")
	void wildcard(Facts facts);

	// Stream<IPlateauCoordinate> stream();
}
