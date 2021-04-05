package eu.solven.anytabletop.map;

import java.util.Map;
import java.util.function.Consumer;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.IPlateauCoordinate;

public class PlateauMap implements IPlateau {
	final Map<String, ?> map;

	public PlateauMap(Map<String, ?> map) {
		this.map = map;
	}

	@Override
	public String getType() {
		return PepperMapHelper.getRequiredString(map, "type");
	}

	@Override
	public int getIntProperty(String key) {
		return (int) map.get(key);
	}

	@Override
	public void forEach(Consumer<IPlateauCoordinate> coordinateConsumer) {
		String type = getType();
		if ("grid".equals(type)) {
			int maxX = getIntProperty("maxX");
			int maxY = getIntProperty("maxY");

			for (int i = 0; i < maxX; i++) {
				for (int j = 0; j < maxY; j++) {
					coordinateConsumer.accept(new PlateauCoordinateAsMap(Map.of("oX", i, "oY", j)));
				}
			}
		} else {
			throw new IllegalStateException("Not managed type: " + type);
		}

	}

}
