package anytabletop;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import eu.solven.anytabletop.checkers.RunCheckersHumanVersusRobot;

public class RunCheckers {
	public static void main(String[] args) {
		final Resource rulesResource = new ClassPathResource("checkers.yml");
		RunCheckersHumanVersusRobot.playGame(rulesResource);
	}
}
