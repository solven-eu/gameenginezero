package eu.solven.anytabletop.easyrules;

import java.util.Map;

import org.jeasy.rules.mvel.MVELAction;
import org.jeasy.rules.mvel.MVELCondition;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class EasyRulesHelper {
	private static final ParserContext parserContext = new ParserContext();

	private static final LoadingCache<String, MVELAction> actionCache =
			CacheBuilder.newBuilder().build(CacheLoader.from(mutation -> {
				return parseActionWithoutCache(mutation);
			}));

	private static final LoadingCache<String, MVELCondition> conditionCache =
			CacheBuilder.newBuilder().build(CacheLoader.from(condition -> {
				return parseConditionWithoutCache(condition);
			}));

	public static MVELAction parseActionWithCache(String mutation) {
		// actionCache.invalidateAll();
		return actionCache.getUnchecked(mutation);
	}

	public static MVELAction parseActionWithoutCache(String mutation) {
		MVELAction action;
		try {
			action = new MVELAction(mutation, parserContext);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Issue parsing mutation: [[" + mutation + "]]", e);
		}
		return action;
	}

	public static MVELCondition parseConditionsWithCache(String condition) {
		return conditionCache.getUnchecked(condition);
	}

	public static MVELCondition parseConditionWithoutCache(String condition) {
		return new MVELCondition(condition, parserContext);
	}

	public static Object parseValue(String expression, Map<String, ?> context) {
		return MVEL.eval(expression, context);
	}

}
