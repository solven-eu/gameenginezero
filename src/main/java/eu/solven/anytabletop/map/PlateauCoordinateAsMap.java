package eu.solven.anytabletop.map;

import java.util.Map;

import eu.solven.anytabletop.IPlateauCoordinate;

public class PlateauCoordinateAsMap implements IPlateauCoordinate {

	final Map<String, ?> map;

	public PlateauCoordinateAsMap(Map<String, ?> map) {
		this.map = map;
	}

	@Override
	public Map<String, ?> asMap() {
		return map;
	}

}
