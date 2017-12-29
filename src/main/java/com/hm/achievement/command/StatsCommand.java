package com.hm.achievement.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.map.MinecraftFont;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.mcshared.particle.ParticleEffect;

/**
 * Class in charge of handling the /aach stats command, which creates and displays a progress bar of the player's
 * achievements
 * 
 * @author Pyves
 */
public class StatsCommand extends AbstractCommand {

	// Minecraft font, used to get size information in the progress bar.
	private static final MinecraftFont FONT = MinecraftFont.Font;

	private boolean configAdditionalEffects;
	private boolean configSound;
	private String langNumberAchievements;
	private int totalAchievements;

	public StatsCommand(AdvancedAchievements plugin) {
		super(plugin);
	}

	@Override
	public void extractConfigurationParameters() {
		super.extractConfigurationParameters();

		totalAchievements = 0;
		// Calculate the total number of achievements in the config file.
		for (NormalAchievements category : NormalAchievements.values()) {
			String categoryName = category.toString();
			if (plugin.getDisabledCategorySet().contains(categoryName)) {
				// Ignore this type.
				continue;
			}
			totalAchievements += plugin.getPluginConfig().getConfigurationSection(categoryName).getKeys(false).size();
		}
		for (MultipleAchievements category : MultipleAchievements.values()) {
			String categoryName = category.toString();
			if (plugin.getDisabledCategorySet().contains(categoryName)) {
				// Ignore this type.
				continue;
			}
			for (String section : plugin.getPluginConfig().getConfigurationSection(categoryName).getKeys(false)) {
				totalAchievements += plugin.getPluginConfig().getConfigurationSection(categoryName + '.' + section)
						.getKeys(false).size();
			}
		}

		if (!plugin.getDisabledCategorySet().contains("Commands")) {
			totalAchievements += plugin.getPluginConfig().getConfigurationSection("Commands").getKeys(false).size();
		}

		// Load configuration parameters.
		configAdditionalEffects = plugin.getPluginConfig().getBoolean("AdditionalEffects", true);
		configSound = plugin.getPluginConfig().getBoolean("Sound", true);

		langNumberAchievements = plugin.getChatHeader()
				+ plugin.getPluginLang().getString("number-achievements", "Achievements received:") + " " + configColor;
	}

	@Override
	protected void executeCommand(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			return;
		}

		Player player = (Player) sender;

		// Retrieve total number of achievements received by the player.
		int achievements = plugin.getCacheManager().getPlayerTotalAchievements(player.getUniqueId());

		player.sendMessage(
				langNumberAchievements + String.format("%.1f", 100 * (double) achievements / totalAchievements) + "%");

		String middleText = " " + achievements + "/" + totalAchievements + " ";
		int verticalBarsToDisplay = 150 - configIcon.length() - FONT.getWidth(middleText);
		boolean hasDisplayedMiddleText = false;
		StringBuilder barDisplay = new StringBuilder();
		int i = 1;
		while (i < verticalBarsToDisplay) {
			if (!hasDisplayedMiddleText && i >= verticalBarsToDisplay / 2) {
				// Middle reached: append number of achievements information.
				barDisplay.append(ChatColor.GRAY).append(middleText);
				// Do not display middleText again.
				hasDisplayedMiddleText = true;
				// Iterate a number of times equal to the number of iterations so far to have the same number of
				// vertical bars left and right from the middle text.
				i = verticalBarsToDisplay - i;
			} else if (i < ((verticalBarsToDisplay - 1) * achievements) / totalAchievements) {
				// Color: progress by user.
				barDisplay.append(configColor).append('|');
				i++;
			} else {
				// Grey: amount not yet reached by user.
				barDisplay.append("&8|");
				i++;
			}
		}
		// Display enriched progress bar.
		player.sendMessage(plugin.getChatHeader() + "["
				+ ChatColor.translateAlternateColorCodes('&', barDisplay.toString()) + ChatColor.GRAY + "]");

		// Player has received all achievement; play special effect and sound.
		if (achievements >= totalAchievements) {
			if (configAdditionalEffects) {
				try {
					// Play special effect.
					ParticleEffect.SPELL_WITCH.display(0, 1, 0, 0.5f, 400, player.getLocation(), 1);
				} catch (Exception e) {
					plugin.getLogger().severe("Error while displaying additional particle effects.");
				}
			}

			// Play special sound.
			if (configSound) {
				playFireworkSound(player);
			}
		}
	}
}
