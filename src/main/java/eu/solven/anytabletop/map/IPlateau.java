package eu.solven.anytabletop.map;

import java.util.function.Consumer;

import eu.solven.anytabletop.IPlateauCoordinate;

public interface IPlateau {
	String getType();

	int getIntProperty(String key);

	void forEach(Consumer<IPlateauCoordinate> coordinateConsumer);
}
