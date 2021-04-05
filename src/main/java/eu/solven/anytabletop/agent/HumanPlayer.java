package eu.solven.anytabletop.agent;

import java.awt.Container;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JTextArea;

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
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.IPlateauCoordinate;

public class HumanPlayer implements IGameAgent {
	private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayer.class);

	private static final int POINT_SIZE = 12;

	final GameModel gameModel;

	final JFrame jf;
	final AsciiTextArea tl;

	public HumanPlayer(GameModel gameModel) {
		this.gameModel = gameModel;

		jf = new JFrame("Demo");
		Container cp = jf.getContentPane();
		tl = new AsciiTextArea();

		// https://stackoverflow.com/questions/16279781/getting-jtextarea-to-display-fixed-width-font-without-antialiasing
		tl.setFont(new Font("monospaced", Font.PLAIN, (int) (POINT_SIZE * 0.3F)));

		cp.add(tl);

		jf.setSize(300, 200);
		jf.setVisible(true);
	}

	@Override
	public Optional<Map<String, ?>> pickAction(GameState currentState, List<Map<String, ?>> possibleActions) {
		int noActionShift;
		if (false) {
			// +1 as 'no_action' is always an option
			noActionShift = 1;
		} else {
			noActionShift = 0;
		}

		int nbActions = noActionShift + possibleActions.size();

		int actionIndex;
		try (Scanner in = new Scanner(System.in)) {
			do {
				LOGGER.info("Possible actions:");

				// LOGGER.info("{}: {}", 0, "no action");
				ParserContext parserContext = new ParserContext();
				for (int i = 0; i < nbActions; i++) {
					Map<String, ?> actions = possibleActions.get(i);

					Facts playerFacts = gameModel.makeFacts(currentState);
					playerFacts.put("player", "?");
					PepperMapHelper.<IPlateauCoordinate>getRequiredAs(actions, "coordinates")
							.asMap()
							.forEach(playerFacts::put);

					List<String> intermediates = PepperMapHelper.getRequiredAs(actions, "intermediate");

					// Mutate with intermediate/hidden variables
					Facts enrichedFacts = gameModel.applyMutators(playerFacts, parserContext, intermediates);

					gameModel.applyMutators(enrichedFacts,
							parserContext,
							PepperMapHelper.getRequiredAs(actions, "mutation"));

					{
						String s = toAscii(currentState);

						tl.drawAscii(s);

						String[] rows = s.split("[\r\n]");
						int width = 300;
						int height = (width * rows.length) / rows[0].length();
						jf.setSize(width, height);

						// ensures the frame is the minimum size it needs to be
						// in order display the components within it
						jf.pack();

						tl.repaint();
					}

					// TODO Compute the diff between
					currentState.diffTo(PepperMapHelper.<GameMapInterpreter>getRequiredAs(playerFacts.asMap(), "map")
							.getLatestState());

					LOGGER.info("{}: {}", i + noActionShift, actions);
				}

				actionIndex = in.nextInt();

				if (actionIndex < 0 || actionIndex > 0) {
					LOGGER.warn("'{}'is not a valid option", actionIndex);
				} else {
					LOGGER.debug("'{}'is a valid option", actionIndex);
					break;
				}
			} while (true);
		}

		if (actionIndex == 0) {
			return Optional.empty();
		} else {
			return Optional.of(possibleActions.get(actionIndex + noActionShift));
		}
	}

	private String toAscii(GameState currentState) {
		IRender render = new Render();
		IContextBuilder builder = render.newBuilder();
		int width = PepperMapHelper.getRequiredNumber(currentState.getMetadata(), "maxX").intValue();
		int height = PepperMapHelper.getRequiredNumber(currentState.getMetadata(), "maxY").intValue();
		builder.width(10 * width).height(10 * height);
		// FullSize surrounding rectangle
		builder.element(new Rectangle());

		String state = currentState.getState();
		String[] rows = state.split("[\r\n]+");
		for (int i = 0; i < rows.length; i++) {
			String row = rows[i];
			for (int j = 0; j < row.length(); j++) {
				char c = row.charAt(j);

				builder.element(new Rectangle(j * 10 + 1, i * 10 + 1, 8, 8));

				if (c == ' ') {
					// nothing
				} else if (c == 'W') {
					builder.element(new Circle(j * 10 + 5, i * 10 + 5, 3));
				} else {
					// builder.element(new Circle(j * 10 + 5, i * 10 + 5, 9));
					// builder.element(new Circle(j * 10 + 5, i * 10 + 5, 7));
					// builder.element(new Circle(j * 10 + 5, i * 10 + 5, 5));
					// builder.element(new Circle(j * 10 + 5, i * 10 + 5, 4));
					builder.element(new Circle(j * 10 + 5, i * 10 + 5, 3));
					builder.element(new Circle(j * 10 + 5, i * 10 + 5, 2));
					builder.element(new Circle(j * 10 + 5, i * 10 + 5, 1));
					builder.element(new Dot(j * 10 + 5, i * 10 + 5));
				}
			}
		}

		ICanvas canvas = render.render(builder.build());
		String s = canvas.getText();
		return s;
	}

	private static class AsciiTextArea extends JTextArea {
		private static final long serialVersionUID = 7866222703838446404L;

		private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayer.AsciiTextArea.class);
		// String currentString = "This is gona be awesome";

		// @Override
		// public void paintComponent(Graphics g) {
		// if (g instanceof Graphics2D) {
		// Graphics2D g2 = (Graphics2D) g;
		// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//
		// g2.drawString(currentString, 0, 0);
		// }
		// }

		public void drawAscii(String ascii) {
			// currentString = ascii;
			LOGGER.info("Rendering:{}{}", System.lineSeparator(), ascii);
			setText(ascii);
		}
	}
}
