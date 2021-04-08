package eu.solven.anytabletop.agent;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import org.jeasy.rules.api.Facts;
import org.mvel2.ParserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cormoran.pepper.collection.PepperMapHelper;
import cormoran.pepper.thread.PepperExecutorsHelper;
import eu.solven.anytabletop.GameMapInterpreter;
import eu.solven.anytabletop.GameModel;
import eu.solven.anytabletop.GameState;
import eu.solven.anytabletop.IPlateauCoordinate;

public class HumanPlayerAwt extends HumanPlayer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HumanPlayerAwt.class);

	private static final int POINT_SIZE = 12;

	final JFrame jf;
	final JPanel panel;
	final JPanel optionsPanel;

	// final BoxLayout boxLayout;
	final AsciiTextArea tl;

	final JButton goButton;

	final AtomicReference<CountDownLatch> refCdl = new AtomicReference<>();

	public HumanPlayerAwt(GameModel gameModel) {
		super(gameModel);

		JFrame jf = new JFrame("Demo");
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// https://stackoverflow.com/questions/2536873/how-can-i-set-size-of-a-button
		JPanel panel = new JPanel(new GridLayout(1, 2));
		jf.setContentPane(panel);

		// Container cp = jf.getContentPane();
		tl = new AsciiTextArea();

		// https://stackoverflow.com/questions/16279781/getting-jtextarea-to-display-fixed-width-font-without-antialiasing
		tl.setFont(new Font("monospaced", Font.PLAIN, (int) (POINT_SIZE * 0.3F)));

		panel.add(tl);

		optionsPanel = new JPanel();

		BoxLayout boxLayout = new BoxLayout(optionsPanel, BoxLayout.Y_AXIS);

		// https://stackoverflow.com/questions/761341/error-upon-assigning-layout-boxlayout-cant-be-shared
		optionsPanel.setLayout(boxLayout);

		panel.add(optionsPanel);

		jf.setSize(300, 200);
		jf.setVisible(true);

		JButton goButton = new JButton("GO");
		goButton.setPreferredSize(new Dimension(40, 40));
		goButton.setBounds(0, 0, 50, 50);

		goButton.addActionListener(e -> {
			refCdl.get().countDown();
		});

		panel.add(goButton);

		this.panel = panel;
		this.jf = jf;
		this.goButton = goButton;
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
	protected int selectAction(GameState currentState, List<Map<String, ?>> possibleActions) {
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
				ParserContext parserContext = new ParserContext();
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

				GameState state =
						PepperMapHelper.<GameMapInterpreter>getRequiredAs(playerFacts.asMap(), "map").getLatestState();

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
							String s = toAscii(states.get(ii));
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

				String s = toAscii(state);
				tl.drawAscii(s);
			} catch (RuntimeException e) {
				LOGGER.warn("Issue in async task", e);
			}
		}, 500, 500, TimeUnit.MILLISECONDS);

		CountDownLatch hasClickedGo = new CountDownLatch(1);

		{
			String s = toAscii(currentState);

			// jf.setLayout(null);

			tl.drawAscii(s);

			String[] rows = s.split("[\r\n]");
			int width = 300;
			int height = (width * rows.length) / rows[0].length();
			panel.setSize(width, height);

			refCdl.set(hasClickedGo);

			goButton.addActionListener(e -> {
				hasClickedGo.countDown();
			});

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

		return radioButtonSelected.get();
	}

}
