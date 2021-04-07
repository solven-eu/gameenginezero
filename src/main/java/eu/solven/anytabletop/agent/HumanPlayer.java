package eu.solven.anytabletop.agent;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.jeasy.rules.api.Facts;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indvd00m.ascii.render.Render;
import com.indvd00m.ascii.render.api.ICanvas;
import com.indvd00m.ascii.render.api.IContextBuilder;
import com.indvd00m.ascii.render.api.IRender;
import com.indvd00m.ascii.render.elements.Circle;
import com.indvd00m.ascii.render.elements.Dot;
import com.indvd00m.ascii.render.elements.Rectangle;

import cormoran.pepper.collection.PepperMapHelper;
import cormoran.pepper.thread.PepperExecutorsHelper;
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.IPlateauCoordinate;

public abstract class HumanPlayer implements IGameAgent {
	private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayer.class);

	final GameModel gameModel;

	public HumanPlayer(GameModel gameModel) {
		this.gameModel = gameModel;
	}

	@Override
	public Optional<Map<String, ?>> pickAction(GameState currentState, List<Map<String, ?>> possibleActions) {
		int actionIndex;
		do {
			actionIndex = selectAction(currentState, possibleActions);

			if (actionIndex < 0 || actionIndex > 0) {
				LOGGER.warn("'{}'is not a valid option", actionIndex);
			} else {
				LOGGER.debug("'{}'is a valid option", actionIndex);
				break;
			}
		} while (true);

		return Optional.of(possibleActions.get(actionIndex));
	}

	protected abstract int selectAction(GameState currentState, List<Map<String, ?>> possibleActions);

	public static String toAscii(GameState currentState) {
		IRender render = new Render();
		IContextBuilder builder = render.newBuilder();
		int width = PepperMapHelper.getRequiredNumber(currentState.getMetadata(), "maxX").intValue();
		int height = PepperMapHelper.getRequiredNumber(currentState.getMetadata(), "maxY").intValue();

		// even in order to have a proper center for circles
		int squareSize = 11;

		builder.width(squareSize * width).height(squareSize * height);
		// FullSize surrounding rectangle
		builder.element(new Rectangle());

		String state = currentState.getState();
		String[] rows = state.split("[\r\n]+");
		for (int i = 0; i < rows.length; i++) {
			String row = rows[i];
			for (int j = 0; j < row.length(); j++) {
				char c = row.charAt(j);

				builder.element(new Rectangle(j * squareSize + 1, i * squareSize + 1, squareSize - 2, squareSize - 2));

				if (c == ' ') {
					// nothing
				} else if (c == 'W') {
					builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 4));
				} else {
					// builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 9));
					// builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 7));
					// builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 5));
					// builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 4));
					builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 4));
					builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 3));
					builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 2));
					builder.element(new Circle(j * squareSize + 5, i * squareSize + 5, 1));
					builder.element(new Dot(j * squareSize + 5, i * squareSize + 5));
				}
			}
		}

		ICanvas canvas = render.render(builder.build());
		String s = canvas.getText();
		return s;
	}

}
