package eu.solven.anytabletop.agent.human;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.jeasy.rules.api.Fact;
import org.jeasy.rules.api.Facts;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import cormoran.pepper.collection.PepperMapHelper;
import cormoran.pepper.thread.PepperExecutorsHelper;
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.IPlateauCoordinate;
import eu.solven.anytabletop.choice.IAgentChoice;

public class HumanPlayerAwt extends HumanPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayerAwt.class);

	private static final int POINT_SIZE = 12;

	final JFrame jf;
	final JPanel panel;
	final JPanel optionsPanel;

	// final BoxLayout boxLayout;
	final AsciiTextArea tl;

	final JButton goButton;
	final AtomicBoolean isRandom = new AtomicBoolean();
	final JButton goRandom;
	final JButton reset;

	final AtomicReference<CountDownLatch> refCdl = new AtomicReference<>();

	final GameModel gameModel;

	final String playerId;

	public HumanPlayerAwt(String playerId, GameModel gameModel) {
		super(gameModel);

		this.gameModel = gameModel;
		this.playerId = playerId;

		JFrame jf = new JFrame("Demo");
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// https://stackoverflow.com/questions/2536873/how-can-i-set-size-of-a-button
		JPanel panel = new JPanel(new GridLayout(1, 2));
		jf.setContentPane(panel);

		// Container cp = jf.getContentPane();
		tl = new AsciiTextArea();
		tl.setSize(200, 200);

		// https://stackoverflow.com/questions/16279781/getting-jtextarea-to-display-fixed-width-font-without-antialiasing
		tl.setFont(new Font("monospaced", Font.PLAIN, (int) (POINT_SIZE * 0.3F)));

		panel.add(tl);

		optionsPanel = new JPanel();

		BoxLayout boxLayout = new BoxLayout(optionsPanel, BoxLayout.Y_AXIS);

		// https://stackoverflow.com/questions/761341/error-upon-assigning-layout-boxlayout-cant-be-shared
		optionsPanel.setLayout(boxLayout);

		panel.add(optionsPanel);

		jf.setSize(300, 300);
		jf.setVisible(true);

		JButton goButton = new JButton("Execute selected option");
		// goButton.setPreferredSize(new Dimension(40, 40));
		goButton.setBounds(0, 0, 20, 20);

		goButton.addActionListener(e -> {
			refCdl.get().countDown();
		});

		JButton goRandom = new JButton("Execute random option");
		// goRandom.setPreferredSize(new Dimension(40, 40));
		goRandom.setBounds(0, 0, 20, 20);
		goRandom.addActionListener(e -> {
			isRandom.set(true);
			refCdl.get().countDown();
		});

		reset = new JButton("Reset the game");
		reset.addActionListener(e -> {
			isRandom.set(true);
			refCdl.get().countDown();
		});

		panel.add(goButton);
		panel.add(goRandom);

		this.panel = panel;
		this.jf = jf;
		this.goButton = goButton;
		this.goRandom = goRandom;
	}

	private static class AsciiTextArea extends JTextArea {
		private static final long serialVersionUID = 7866222703838446404L;

		private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayerAwt.AsciiTextArea.class);

		public void drawAscii(String ascii) {
			// currentString = ascii;
			LOGGER.debug("Rendering:{}{}", System.lineSeparator(), ascii);
			setText(ascii);
		}
	}

	@Override
	protected int selectAction(GameState currentState, List<IAgentChoice> possibleActions) {
		// LOGGER.info("Possible actions:");

		ButtonGroup buttonGroup = new ButtonGroup();

		AtomicBoolean showSelectionAction = new AtomicBoolean();
		AtomicInteger radioButtonSelected = new AtomicInteger();

		List<GameState> states = new ArrayList<>();

		// https://stackoverflow.com/questions/38349445/how-to-delete-all-components-in-a-jpanel-dynamically/38350395
		optionsPanel.removeAll();

		for (int i = 0; i < possibleActions.size(); i++) {
			JRadioButton jRadioButton = new JRadioButton();
			jRadioButton.setText("Option " + i);
			jRadioButton.setBounds(0 + 200 * i, 0 + 200 * i, 20, 20);

			if (i == 0) {
				// Select by default the first option
				jRadioButton.setSelected(true);
			}

			{
				IAgentChoice actions = possibleActions.get(i);

				GameState state = gameModel.applyChoice(currentState, actions);

				states.add(state);
			}

			final int ii = i;

			// Adding Listener to JButton.
			jRadioButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// If condition to check if jRadioButton2 is selected.
					if (jRadioButton.isSelected()) {
						int previousSelected = radioButtonSelected.getAndSet(ii);

						if (previousSelected != ii) {
							String s = BoardAscii.toAscii(gameModel, states.get(ii));
							tl.drawAscii(s);
						}
					}
				}
			});

			// Couple the radioButtons together
			buttonGroup.add(jRadioButton);

			// Ensure this radioButton is visible
			optionsPanel.add(jRadioButton);
		}

		ScheduledExecutorService singleEs = PepperExecutorsHelper.newSingleThreadScheduledExecutor("Animations");

		singleEs.scheduleWithFixedDelay(() -> {
			try {
				GameState state;

				if (showSelectionAction.get()) {
					showSelectionAction.set(false);
					state = states.get(radioButtonSelected.get());
				} else {
					showSelectionAction.set(true);
					state = currentState;
				}

				String s = BoardAscii.toAscii(gameModel, state);
				tl.drawAscii(s);
			} catch (RuntimeException e) {
				LOGGER.warn("Issue in async task", e);
			}
		}, 500, 500, TimeUnit.MILLISECONDS);

		CountDownLatch hasClickedGo = new CountDownLatch(1);

		{
			String s = BoardAscii.toAscii(gameModel, currentState);

			// jf.setLayout(null);

			tl.drawAscii(s);

			String[] rows = s.split("[\r\n]");
			int width = 400;
			int height = (width * rows.length) / rows[0].length();
			panel.setSize(width, height);

			refCdl.set(hasClickedGo);
			isRandom.set(false);

			// ensures the frame is the minimum size it needs to be
			// in order display the components within it
			jf.pack();

			tl.repaint();
		}

		try {
			hasClickedGo.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();

			singleEs.shutdown();

			throw new IllegalStateException("interrupted", e);
		}

		singleEs.shutdown();

		if (isRandom.get()) {
			return new Random().nextInt(possibleActions.size());
		} else {
			return radioButtonSelected.get();
		}
	}

}
