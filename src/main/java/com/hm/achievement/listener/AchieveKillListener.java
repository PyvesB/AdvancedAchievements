package com.hm.achievement.listener;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.hm.achievement.event.PlayerAchievementEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.db.DatabasePools;

public class AchieveKillListener implements Listener {

	private AdvancedAchievements plugin;

	public AchieveKillListener(AdvancedAchievements plugin) {

		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event) {

		if (!(event.getEntity().getKiller() instanceof Player))
			return;
		Player player = (Player) event.getEntity().getKiller();
		if (!player.hasPermission("achievement.get") || plugin.isRestrictCreative()
				&& player.getGameMode() == GameMode.CREATIVE || plugin.isInExludedWorld(player))
			return;
		Entity entity = event.getEntity();

		if (!(entity instanceof LivingEntity)) {
			return;
		}

		String mobName = null;
		if (entity instanceof Player) {
			mobName = "player";
		} else {
			mobName = entity.getType().name().toLowerCase();
		}
		if (!plugin.getConfig().isConfigurationSection("Kills." + mobName))
			return;

		Integer kills = 0;
		if (!DatabasePools.getEntityDeathHashMap().containsKey(player.getUniqueId().toString() + mobName))
			kills = plugin.getDb().getKills(player, mobName) + 1;
		else
			kills = DatabasePools.getEntityDeathHashMap().get(player.getUniqueId().toString() + mobName) + 1;

		DatabasePools.getEntityDeathHashMap().put(player.getUniqueId().toString() + mobName, kills);

		String configAchievement = "Kills." + mobName + "." + kills;
		if (plugin.getReward().checkAchievement(configAchievement)) {

			plugin.getAchievementDisplay().displayAchievement(player, configAchievement);
			SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
			plugin.getDb().registerAchievement(player, plugin.getConfig().getString(configAchievement + ".Name"),
					plugin.getConfig().getString(configAchievement + ".Message"), format.format(new Date()));

			plugin.getServer().getPluginManager().callEvent(new PlayerAchievementEvent(player, configAchievement));

			plugin.getReward().checkConfig(player, configAchievement);
		}
	}
}
