package com.hm.achievement.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.category.MultipleAchievements;

/**
 * Listener class to deal with Places achievements.
 * 
 * @author Pyves
 *
 */
public class AchieveBlockPlaceListener extends AbstractListener {

	public AchieveBlockPlaceListener(AdvancedAchievements plugin) {
		super(plugin);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (!shouldIncreaseBeTakenIntoAccountNoPermissions(player)) {
			return;
		}

		Block block = event.getBlock();

		MultipleAchievements category = MultipleAchievements.PLACES;

		String blockName = block.getType().name().toLowerCase();
		if (!player.hasPermission(category.toPermName() + '.' + blockName)) {
			return;
		}
		if (plugin.getPluginConfig().isConfigurationSection(
				category + "." + blockName + ':' + block.getState().getData().toItemStack(0).getDurability())) {
			blockName += ":" + block.getState().getData().toItemStack(0).getDurability();
		} else if (!plugin.getPluginConfig().isConfigurationSection(category + "." + blockName)) {
			return;
		}

		updateStatisticAndAwardAchievementsIfAvailable(player, category, blockName, 1);
	}
}
