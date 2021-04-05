package eu.solven.anytabletop;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

import eu.solven.anytabletop.map.IPlateau;

public class DameStateProvider implements IStateProvider {
	private static final String EOL = System.lineSeparator();

	final IPlateau grid;

	public DameStateProvider(IPlateau grid) {
		if (!(grid.getIntProperty("maxX") == 10)) {
			throw new IllegalArgumentException("BOOM");
		} else if (!(grid.getIntProperty("maxY") == 10)) {
			throw new IllegalArgumentException("BOOM");
		}

		this.grid = grid;
	}

	@Override
	public GameState generateInitialState() {
		// http://www.lecomptoirdesjeux.com/regle-jeu-dames.htm
		String plateau = Stream.of(" B B B B B",
				"B B B B B ",
				" B B B B B",
				"B B B B B ",
				"          ",
				"          ",
				" W W W W W",
				"W W W W W ",
				" W W W W W",
				"W W W W W ").collect(Collectors.joining(EOL));

		return new GameState(plateau, ImmutableMap.of("player", "w", "maxX", 10, "maxY", 10));
	}

}
