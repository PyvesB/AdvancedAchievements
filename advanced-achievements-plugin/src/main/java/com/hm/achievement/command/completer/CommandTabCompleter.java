package com.hm.achievement.command.completer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;

import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.lifecycle.Reloadable;
import com.hm.mcshared.file.CommentedYamlConfiguration;

/**
 * Class in charge of handling auto-completion for achievements and categories when using /aach check, /aach reset,
 * /aach give or /aach delete commands.
 *
 * @author Pyves
 */
@Singleton
public class CommandTabCompleter implements TabCompleter, Reloadable {

	private static final int MAX_LIST_LENGTH = 50;

	private final Set<String> enabledCategoriesWithSubcategories = new HashSet<>();
	private final CommentedYamlConfiguration mainConfig;
	private final Map<String, String> achievementsAndDisplayNames;
	private final Set<String> disabledCategories;

	private Set<String> configCommandsKeys;

	@Inject
	public CommandTabCompleter(@Named("main") CommentedYamlConfiguration mainConfig,
			Map<String, String> achievementsAndDisplayNames, Set<String> disabledCategories) {
		this.mainConfig = mainConfig;
		this.achievementsAndDisplayNames = achievementsAndDisplayNames;
		this.disabledCategories = disabledCategories;
	}

	@Override
	public void extractConfigurationParameters() {
		configCommandsKeys = mainConfig.getShallowKeys("Commands");

		enabledCategoriesWithSubcategories.clear();
		for (MultipleAchievements category : MultipleAchievements.values()) {
			for (String subcategory : mainConfig.getShallowKeys(category.toString())) {
				enabledCategoriesWithSubcategories.add(category + "." + StringUtils.deleteWhitespace(subcategory));
			}
		}
		for (NormalAchievements category : NormalAchievements.values()) {
			enabledCategoriesWithSubcategories.add(category.toString());
		}
		enabledCategoriesWithSubcategories.add("Commands");
		// Only auto-complete with non-disabled categories.
		enabledCategoriesWithSubcategories.removeAll(disabledCategories);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!"aach".equals(command.getName()) || args.length == 3 && !"add".equalsIgnoreCase(args[0])
				|| args.length == 4 && "add".equalsIgnoreCase(args[0])) {
			// Complete with players.
			return null;
		} else if (args.length == 2 && "reset".equalsIgnoreCase(args[0])) {
			return getPartialList(sender, enabledCategoriesWithSubcategories, args[1]);
		} else if (args.length == 3 && "add".equalsIgnoreCase(args[0])) {
			return getPartialList(sender, enabledCategoriesWithSubcategories, args[2]);
		} else if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
			return getPartialList(sender, configCommandsKeys, args[1]);
		} else if (args.length == 2 && ("delete".equalsIgnoreCase(args[0]) || "check".equalsIgnoreCase(args[0]))) {
			return getPartialList(sender, achievementsAndDisplayNames.keySet(), args[1]);
		} else if (args.length == 2 && "inspect".equalsIgnoreCase(args[0])) {
			// Spaces are not replaced.
			return getPartialList(sender, achievementsAndDisplayNames.values(), args[1]);
		}
		// No completion.
		return Collections.singletonList("");
	}

	/**
	 * Returns a partial list based on the input set. Members of the returned list must start with what the player has
	 * types so far. The list also has a limited length to avoid filling the player's screen.
	 *
	 *
	 * @param sender
	 * @param options
	 * @param prefix
	 * @return a list limited in length, containing elements matching the prefix,
	 */
	private List<String> getPartialList(CommandSender sender, Collection<String> options, String prefix) {
		if (sender instanceof ConsoleCommandSender) {
			// Console mapper uses the given parameters, spaces and all.
			return getFormattedMatchingOptions(options, prefix, Function.identity());
		} else {
			// Default mapper replaces spaces with an Open Box character to prevent completing wrong word.
			// Prevented Behaviour:
			// T -> Tamer -> Teleport Man -> Teleport The Avener -> Teleport The The Smelter
			return getFormattedMatchingOptions(options, prefix, s -> StringUtils.replace(s, " ", "\u2423"));
		}
	}

	private List<String> getFormattedMatchingOptions(Collection<String> options, String prefix,
			Function<String, String> displayMapper) {
		// Remove chat colors
		// Find matching options
		// Map matches to be displayed properly with displayMapper
		// Sort matching elements by alphabetical order.
		List<String> allOptions = options.stream()
				.map(s -> StringUtils.removePattern(s, "&([a-f]|r|[k-o]|[0-9]){1}"))
				.filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
				.map(displayMapper).sorted().collect(Collectors.toList());

		if (allOptions.size() > MAX_LIST_LENGTH) {
			List<String> matchingOptions = allOptions.subList(0, MAX_LIST_LENGTH - 2);
			// Suspension points to show that list was truncated.
			matchingOptions.add("\u2022\u2022\u2022");
			return matchingOptions;
		}
		return allOptions;
	}
}
