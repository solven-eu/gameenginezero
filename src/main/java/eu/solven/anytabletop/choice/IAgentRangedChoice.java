package eu.solven.anytabletop.choice;

import java.util.List;

import eu.solven.anytabletop.IPlateauCoordinate;

/**
 * This is useful for choices which are to be made, and can be any values in given ranges. Once picked by the use, all
 * ranges should be resolved into single values
 * 
 * @author Benoit Lacelle
 *
 */
public interface IAgentRangedChoice {

	String getPlayerId();

	IPlateauCoordinate getCoordinate();

	List<String> getIntermediates();

	List<String> getMutations();

}
