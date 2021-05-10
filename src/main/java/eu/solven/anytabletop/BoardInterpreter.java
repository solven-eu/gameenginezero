package eu.solven.anytabletop;

import eu.solven.anytabletop.map.IBoard;

public class BoardInterpreter {
	final IBoard board;

	public BoardInterpreter(IBoard board) {
		this.board = board;
	}

	public int getIntProperty(String k) {
		return board.getIntProperty(k);
	}
}
