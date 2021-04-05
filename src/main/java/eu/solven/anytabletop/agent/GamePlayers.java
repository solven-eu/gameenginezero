package eu.solven.anytabletop.agent;

import java.util.List;

public class GamePlayers {
	final List<IGameAgent> players;

	public GamePlayers(List<IGameAgent> players) {
		this.players = players;
	}

	public IGameAgent getAgent(int i) {
		return players.get(i);
	}
}
