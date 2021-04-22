package anytabletop;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import eu.solven.anytabletop.GameExecutor;

public class RunCheckers {
	public static void main(String[] args) {
		final Resource rulesResource = new ClassPathResource("checkers.yml");
		GameExecutor.playGame(rulesResource);
	}
}
