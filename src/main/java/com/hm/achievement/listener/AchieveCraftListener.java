package com.hm.achievement.listener;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.hm.achievement.event.PlayerAchievementEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import com.hm.achievement.AdvancedAchievements;

public class AchieveCraftListener implements Listener {

	private AdvancedAchievements plugin;

	public AchieveCraftListener(AdvancedAchievements plugin) {

		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryCraft(CraftItemEvent event) {

		if (!(event.getWhoClicked() instanceof Player) || event.getAction().name().equals("NOTHING"))
			return;

		Player player = (Player) event.getWhoClicked();
		if (!player.hasPermission("achievement.get") || plugin.isRestrictCreative()
				&& player.getGameMode() == GameMode.CREATIVE || plugin.isInExludedWorld(player))
			return;
		try {

			ItemStack item = event.getRecipe().getResult();
			String craftName = item.getType().name().toLowerCase();
			if (!plugin.getConfig().isConfigurationSection("Crafts." + craftName))
				return;

			int amount = item.getAmount();
			if (event.isShiftClick()) {
				int max = event.getInventory().getMaxStackSize();
				ItemStack[] matrix = event.getInventory().getMatrix();
				for (ItemStack itemStack : matrix) {
					if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
						int tmp = itemStack.getAmount();
						if (tmp < max && tmp > 0)
							max = tmp;
					}
				}
				amount *= max;
			}

			Integer times = plugin.getDb().updateAndGetCraft(player, item, amount);
			String configAchievement;
			for (String threshold : plugin.getConfig().getConfigurationSection("Crafts." + craftName).getKeys(false))
				if (times >= Integer.parseInt(threshold)
						&& !plugin.getDb().hasPlayerAchievement(player,
								plugin.getConfig().getString("Crafts." + craftName + "." + threshold + "." + "Name"))) {
					configAchievement = "Crafts." + craftName + "." + threshold;
					if (plugin.getReward().checkAchievement(configAchievement)) {

						plugin.getAchievementDisplay().displayAchievement(player, configAchievement);
						SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
						plugin.getDb()
								.registerAchievement(player, plugin.getConfig().getString(configAchievement + ".Name"),
										plugin.getConfig().getString(configAchievement + ".Message"),
										format.format(new Date()));

						plugin.getServer().getPluginManager().callEvent(new PlayerAchievementEvent(player, configAchievement));

						plugin.getReward().checkConfig(player, configAchievement);

					}
				}
		} catch (Exception e) {

			plugin.getLogger().severe("Error while dealing with crafting event.");
			e.printStackTrace();
		}
	}
}
