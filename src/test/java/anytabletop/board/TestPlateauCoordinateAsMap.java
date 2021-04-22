package anytabletop.board;

import org.junit.jupiter.api.Test;

import eu.solven.anytabletop.map.PlateauCoordinateAsMap;
import nl.jqno.equalsverifier.EqualsVerifier;

public class TestPlateauCoordinateAsMap {
	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(PlateauCoordinateAsMap.class).verify();
	}
}
