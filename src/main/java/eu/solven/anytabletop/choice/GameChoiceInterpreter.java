package eu.solven.anytabletop.choice;

import com.google.common.collect.ListMultimap;

public class GameChoiceInterpreter {
	final ListMultimap<String, IAgentChoice> playerToChoices;

	public GameChoiceInterpreter(ListMultimap<String, IAgentChoice> playerToChoices) {
		this.playerToChoices = playerToChoices;
	}

	// public List<IAgentChoice> moves(String player) {
	// return Multimaps.asMap(playerToChoices).get(player);
	// }

	public boolean isEmpty(String player) {
		return playerToChoices.get(player).isEmpty();
	}
}
