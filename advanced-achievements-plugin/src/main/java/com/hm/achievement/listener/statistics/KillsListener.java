package com.hm.achievement.listener.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;

import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.db.CacheManager;
import com.hm.achievement.utils.RewardParser;
import com.hm.mcshared.file.CommentedYamlConfiguration;

/**
 * Listener class to deal with Kills achievements.
 * 
 * @author Pyves
 *
 */
@Singleton
public class KillsListener extends AbstractListener {

	@Inject
	public KillsListener(@Named("main") CommentedYamlConfiguration mainConfig, int serverVersion,
			Map<String, List<Long>> sortedThresholds, CacheManager cacheManager, RewardParser rewardParser) {
		super(mainConfig, serverVersion, sortedThresholds, cacheManager, rewardParser);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event) {
		Player player = event.getEntity().getKiller();

		if (player == null || !shouldIncreaseBeTakenIntoAccountNoPermissions(player)) {
			return;
		}

		Entity entity = event.getEntity();

		String mobType;
		if (entity instanceof Player) {
			mobType = "player";
		} else {
			mobType = entity.getType().name().toLowerCase();
			if (entity instanceof Creeper && ((Creeper) entity).isPowered()) {
				mobType = "poweredcreeper";
			}
		}

		MultipleAchievements category = MultipleAchievements.KILLS;

		Set<String> foundAchievements = new HashSet<>();

		if (player.hasPermission(category.toPermName() + '.' + mobType)) {
			foundAchievements.addAll(findAchievementsByCategoryAndName(category, mobType));
		}

		String customName = "customname-" + entity.getCustomName();
		if (player.hasPermission(category.toPermName() + '.' + customName)) {
			foundAchievements.addAll(findAchievementsByCategoryAndName(category, customName));
		}

		if (entity instanceof Player) {
			String specificPlayer = "specificplayer-" + entity.getUniqueId().toString().toLowerCase();
			if (player.hasPermission(category.toPermName() + '.' + specificPlayer)) {
				foundAchievements.addAll(findAchievementsByCategoryAndName(category, specificPlayer));
			}
		}

		foundAchievements.forEach(achievement -> updateStatisticAndAwardAchievementsIfAvailable(player, category,
				achievement, 1));
	}
}
