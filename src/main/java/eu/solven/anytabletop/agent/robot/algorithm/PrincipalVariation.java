package eu.solven.anytabletop.agent.robot.algorithm;

import java.util.List;

import eu.solven.anytabletop.choice.IAgentChoice;

/**
 * The {@link List} of {@link IAgentChoice} leading to a score
 * 
 * @author Benoit Lacelle
 *
 */
public class PrincipalVariation {
	final double score;
	final List<IAgentChoice> choices;

	public PrincipalVariation(double score, List<IAgentChoice> choices) {
		this.score = score;
		this.choices = choices;
	}

	public List<IAgentChoice> getChoices() {
		return choices;
	}

	public double getScore() {
		return score;
	}
}
