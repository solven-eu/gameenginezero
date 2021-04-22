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

	@Override
	public String toString() {
		return "PlateauCoordinateAsMap [map=" + map + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlateauCoordinateAsMap other = (PlateauCoordinateAsMap) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

}
