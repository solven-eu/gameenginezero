package eu.solven.anytabletop.agent.human;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.jeasy.rules.api.Facts;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indvd00m.ascii.render.Render;
import com.indvd00m.ascii.render.api.ICanvas;
import com.indvd00m.ascii.render.api.IContextBuilder;
import com.indvd00m.ascii.render.api.IRender;
import com.indvd00m.ascii.render.elements.Dot;
import com.indvd00m.ascii.render.elements.Ellipse;
import com.indvd00m.ascii.render.elements.PseudoText;
import com.indvd00m.ascii.render.elements.Rectangle;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.anytabletop.GameInfo;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameModelHelpers;
import eu.solven.anytabletop.map.BoardFromMap;
import eu.solven.anytabletop.map.IBoard;
import eu.solven.anytabletop.state.GameState;

public class BoardAscii {
	private static final Logger LOGGER = LoggerFactory.getLogger(BoardAscii.class);

	// As fonts are general higher than wider, we apply this ratio to turn standing-rectangles into square
	final static float xyRatio = 1.8F;
	// even in order to have a proper center for circles
	final static int squareSize = 11;

	// We adjust X-coordinate due to font not being squares
	private static int adjustX(int x, int gap) {
		return (int) ((x * squareSize) * xyRatio) + gap;
	}

	// We adjust Y as Ascii library consider Y goes from top to bottom (i.e. Y is row index), while we consider y as
	// bottom to top (i.e. standard math cvoordinates)
	private static int adjustY(int maxRow, int y, int gap) {
		return maxRow - (y * squareSize + gap);
	}

	public static String toAscii(GameModel gameModel, GameState currentState) {
		IRender boardRender = new Render();
		IContextBuilder boardBuilder = boardRender.newBuilder();
		int width = gameModel.getBoard().getIntProperty("width");
		int height = gameModel.getBoard().getIntProperty("height");

		int maxRow = squareSize * height;
		boardBuilder.width(adjustX(width, 0)).height(maxRow);
		// FullSize surrounding rectangle
		boardBuilder.element(new Rectangle());

		GameInfo gameInfo = gameModel.getGameInfo();
		List<? extends Map<String, ?>> renderings = gameInfo.getRenderings();

		IBoard board = new BoardFromMap(gameInfo.getBoard());

		Facts facts = gameModel.makeFacts(currentState);

		ParserContext parserContext = new ParserContext();
		board.forEach(coordinate -> {
			coordinate.asMap().forEach(facts::put);

			int x = (int) coordinate.asMap().get("x");
			int y = (int) coordinate.asMap().get("y");

			renderings.forEach(rendering -> {
				List<String> conditions = PepperMapHelper.getRequiredAs(rendering, "conditions");

				Facts renderingFacts = GameModelHelpers.cloneFacts(facts);

				boolean conditionIsOk = GameModelHelpers.logicalAnd(renderingFacts, conditions, parserContext);

				if (conditionIsOk) {
					String style = PepperMapHelper.getRequiredAs(rendering, "style");

					int centerX = adjustX(1, -1) / 2;
					int centerY = (squareSize - 1) / 2;
					if ("bg: black".equals(style)) {
						for (int i = 0; i < adjustX(1, 0); i++) {
							for (int j = 0; j < squareSize; j++) {
								// boardBuilder.element(new Dot(adjustX(x, i), adjustY(maxRow, y, j)));
							}
						}
					} else if ("bg: white".equals(style)) {
						LOGGER.debug("Leave empty");
					} else if ("border: black".equals(style)) {
						Rectangle rawBorder = new Rectangle(x, y, 1, 1);
						int gapHeight = -1;
						int finalHeight = rawBorder.getHeight() * squareSize + gapHeight;
						Rectangle border = new Rectangle(adjustX(rawBorder.getX(), 1),
								adjustY(maxRow, rawBorder.getY(), 1) - finalHeight + 1,
								adjustX(rawBorder.getWidth(), -1),
								finalHeight);
						// System.out.println(border);
						boardBuilder.element(border);
					} else if ("item: light_man".equals(style)) {
						// boardBuilder.element(new Circle(adjustX(x, center), y * squareSize + center, center -
						// 1));
						Ellipse ellipse = new Ellipse(adjustX(x, centerX),
								adjustY(maxRow, y, centerY),
								2 * centerX,
								2 * centerY - 1);
						// System.out.println(ellipse);
						boardBuilder.element(ellipse);
					} else if ("item: dark_man".equals(style)) {
						// Concentric circles to paint in black
						IntStream.range(1, centerX).forEach(rayon -> {
							boardBuilder.element(new Ellipse(adjustX(x, centerX),
									adjustY(maxRow, y, centerY),
									2 * rayon,
									(int) (2L * rayon / xyRatio)));
						});

						// boardBuilder.element(new Ellipse(adjustX(x, centerX),
						// adjustY(maxRow, y, centerY),
						// 2 * centerX,
						// 2 * centerX - 1));
						// IntStream.range(1, centerY).forEach(rayon -> {
						// boardBuilder.element(new Circle(adjustX(x, centerX), adjustY(maxRow, y, centerY), rayon));
						// });

						// A simple dot in the very middle to complete the black
						boardBuilder.element(new Dot(adjustX(x, centerX), adjustY(maxRow, y, centerY)));
					} else if ("item: double_light_man".equals(style)) {
						// TODO Adjust X given text width
						boardBuilder.element(new PseudoText("D_W",
								adjustX(x, squareSize / 2),
								adjustY(maxRow, y + 1, 0),
								squareSize));
					} else if ("item: double_dark_man".equals(style)) {
						boardBuilder.element(new PseudoText("D_B",
								adjustX(x, squareSize / 2),
								adjustY(maxRow, y + 1, 0),
								squareSize));
					} else {
						LOGGER.warn("Unknown style: {}", style);
						boardBuilder.element(new PseudoText("?",
								adjustX(x, 0) - squareSize / 2,
								adjustY(maxRow, y + 1, 0),
								squareSize));
					}
				}
			});
		});

		ICanvas canvas = boardRender.render(boardBuilder.build());
		String s = canvas.getText();
		return s;
	}
}
