package anytabletop;

import org.junit.jupiter.api.Test;

import eu.solven.anytabletop.GameExecutor;

public class TestJeuDeDames {
	@Test
	public void testJeuDeDame() {
		new GameExecutor().main(new String[0]);
	}
}
