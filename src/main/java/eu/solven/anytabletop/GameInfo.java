package eu.solven.anytabletop;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.solven.anytabletop.agent.PlayerPojo;

@JsonIgnoreProperties(value = { "source" })
public class GameInfo {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameInfo.class);

	protected final String name;
	protected final String derivesFrom;

	protected final List<Map<String, ?>> rendering;
	protected final Map<String, ?> constants;
	protected final List<Map<String, ?>> constrains;
	protected final Map<String, ?> board;

	protected final List<Map<String, ?>> allowedMoves;

	protected final Map<String, ?> setup;

	protected final List<PlayerPojo> players;
	protected final List<Map<String, ?>> gameover;

	@JsonCreator
	public GameInfo(@JsonProperty("name") String name,
			@JsonProperty("names") List<String> names,
			@JsonProperty("derives_from") String derivesFrom,
			@JsonProperty("renderings") List<Map<String, ?>> rendering,
			@JsonProperty("constants") Map<String, ?> constants,
			@JsonProperty("constrains") List<Map<String, ?>> constrains,
			@JsonProperty("board") Map<String, ?> board,
			@JsonProperty("allowed_moves") List<Map<String, ?>> allowedMoves,
			@JsonProperty("setup") Map<String, ?> setup,
			@JsonProperty("players") List<PlayerPojo> players,
			@JsonProperty("gameovers") List<Map<String, ?>> gameover) {
		this.name = name;
		LOGGER.debug("{} is also named: {}", name, names);

		this.derivesFrom = derivesFrom;

		this.rendering = rendering;
		this.constants = constants;
		this.constrains = constrains;
		this.board = board;
		this.allowedMoves = allowedMoves;
		this.setup = setup;
		this.players = players;
		this.gameover = gameover;
	}

	public String getName() {
		return name;
	}

	public String getDerivesFrom() {
		return derivesFrom;
	}

	public List<? extends Map<String, ?>> getRenderings() {
		return rendering;
	}

	public Map<String, ?> getConstants() {
		if (constants == null) {
			return Collections.emptyMap();
		}
		return constants;
	}

	public List<Map<String, ?>> getConstrains() {
		return constrains;
	}

	public Map<String, ?> getBoard() {
		return board;
	}

	public Collection<? extends Map<String, ?>> getAllowedMoves() {
		return allowedMoves;
	}

	public Map<String, ?> getSetup() {
		return setup;
	}

	public List<PlayerPojo> getPlayers() {
		return players;
	}

	public List<Map<String, ?>> getGameovers() {
		return gameover;
	}
}
