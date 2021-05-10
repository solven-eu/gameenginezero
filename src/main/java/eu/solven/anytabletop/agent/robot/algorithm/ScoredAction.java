package eu.solven.anytabletop.agent.robot.algorithm;

import eu.solven.anytabletop.choice.IAgentChoice;

/**
 * A Pair of an {@link IAgentChoice} and a score
 * 
 * @author Benoit Lacelle
 *
 */
public class ScoredAction {
	final double score;
	final IAgentChoice choice;

	public ScoredAction(double score, IAgentChoice choice) {
		this.score = score;
		this.choice = choice;
	}

	public IAgentChoice getChoice() {
		return choice;
	}

	public double getScore() {
		return score;
	}
}
