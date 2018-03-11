package com.hm.achievement.listener.statistics;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemBreakEvent;

import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.command.ReloadCommand;
import com.hm.achievement.db.CacheManager;
import com.hm.achievement.utils.RewardParser;
import com.hm.mcshared.file.CommentedYamlConfiguration;

/**
 * Listener class to deal with ItemBreaks achievements.
 * 
 * @author Pyves
 *
 */
@Singleton
public class ItemBreaksListener extends AbstractListener {

	@Inject
	public ItemBreaksListener(@Named("main") CommentedYamlConfiguration mainConfig, int serverVersion,
			Map<String, List<Long>> sortedThresholds, CacheManager cacheManager, RewardParser rewardParser,
			ReloadCommand reloadCommand) {
		super(mainConfig, serverVersion, sortedThresholds, cacheManager, rewardParser, reloadCommand);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerItemBreak(PlayerItemBreakEvent event) {
		Player player = event.getPlayer();
		NormalAchievements category = NormalAchievements.ITEMBREAKS;
		if (!shouldIncreaseBeTakenIntoAccount(player, category)) {
			return;
		}

		updateStatisticAndAwardAchievementsIfAvailable(player, category, 1);
	}
}