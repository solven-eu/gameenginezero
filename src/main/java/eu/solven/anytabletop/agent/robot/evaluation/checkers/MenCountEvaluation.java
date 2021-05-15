package eu.solven.anytabletop.agent.robot.evaluation.checkers;

import java.util.concurrent.atomic.AtomicInteger;

import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.agent.robot.evaluation.IGameStateEvaluator;
import eu.solven.anytabletop.map.BoardFromMap;
import eu.solven.anytabletop.map.IBoard;
import eu.solven.anytabletop.state.GameState;

/**
 * We consider a position is stronger if it has more possible mens.
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/ykaragol/checkersmaster/blob/master/CheckersMaster/src/checkers/evaluation/MenCountEvaluation.java
public class MenCountEvaluation implements IGameStateEvaluator {

	@Override
	public double evaluate(GameModel gameModel, GameState state, String player) {
		IBoard board = new BoardFromMap(gameModel.getGameInfo().getBoard());

		AtomicInteger lightMinusDark = new AtomicInteger();

		GameMapInterpreter interpreter = new GameMapInterpreter(state);
		board.forEach(coordinate -> {

			int x = (int) coordinate.asMap().get("x");
			int y = (int) coordinate.asMap().get("y");

			char currentChar = interpreter.charAt(x, y);

			if (currentChar == 'W' || currentChar == 'X') {
				lightMinusDark.incrementAndGet();
			} else if (currentChar == 'B' || currentChar == 'C') {
				lightMinusDark.decrementAndGet();
			}
		});

		if (player.equals("w")) {
			return lightMinusDark.doubleValue();
		} else if (player.equals("b")) {
			return lightMinusDark.doubleValue();
		} else {
			throw new IllegalArgumentException("Invalid player: " + player);
		}
	}
}