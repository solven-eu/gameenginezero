package anytabletop;

import org.junit.jupiter.api.Test;

import eu.solven.anytabletop.state.GameState;
import nl.jqno.equalsverifier.EqualsVerifier;

public class TestGameState {
	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(GameState.class).verify();
	}
}
