package eu.solven.anytabletop.choice;

import java.util.List;

import eu.solven.anytabletop.IPlateauCoordinate;

public class AgentChoice implements IAgentChoice {
	final String player;
	final IPlateauCoordinate coordinate;
	final List<String> mutations;
	final List<String> intermediates;

	public AgentChoice(String player,
			IPlateauCoordinate coordinate,
			List<String> mutations,
			List<String> intermediates) {
		super();
		this.player = player;
		this.coordinate = coordinate;
		this.mutations = mutations;
		this.intermediates = intermediates;
	}

	@Override
	public String getPlayerId() {
		return player;
	}

	@Override
	public List<String> getIntermediates() {
		return intermediates;
	}

	@Override
	public IPlateauCoordinate getCoordinate() {
		return coordinate;
	}

	@Override
	public List<String> getMutations() {
		return mutations;
	}
}
