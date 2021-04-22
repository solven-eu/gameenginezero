package eu.solven.anytabletop.choice;

import java.util.List;

import eu.solven.anytabletop.IPlateauCoordinate;

public interface IAgentChoice {

	String getPlayerId();

	IPlateauCoordinate getCoordinate();

	List<String> getIntermediates();

	List<String> getMutations();

}
