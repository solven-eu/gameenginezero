package anytabletop;

import org.junit.jupiter.api.Test;

import eu.solven.anytabletop.GameState;
import nl.jqno.equalsverifier.EqualsVerifier;

public class TestGameState {
	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(GameState.class).verify();
	}
}
