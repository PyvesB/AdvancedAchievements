package com.hm.achievement.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;

/**
 * Advanced Achievements API. Unless explicitly stated otherwise, implementations are expected to be thread-safe.
 * 
 * @author Pyves
 *
 */
public interface AdvancedAchievementsAPI {

	/**
	 * Formats Advanced Achievements's version as an integer. The version is computed as follows: 100 * major + 10 *
	 * minor + micro. For instance plugin version 5.0 will return 500; plugin version 5.1.2 will return 512.
	 * 
	 * @since 5.0
	 * @return version code
	 */
    int getAdvancedAchievementsVersionCode();

	/**
	 * Checks whether player has received achievement {@code achievementName}. Implementation benefits from Advanced
	 * Achievements caching when player is online and if method called from server thread.
	 * 
	 * @since 5.0
	 * @param player
	 * @param achievementName as defined by the Name parameter in Advanced Achievements config.yml
	 * @return true if player has received the achievement, false otherwise
	 */
    boolean hasPlayerReceivedAchievement(UUID player, String achievementName);

	/**
	 * Retrieves all achievements received by the player.
	 * 
	 * @since 5.0
	 * @param player
	 * @return list of {@code Achievement} objects received by the player
	 */
    List<Achievement> getPlayerAchievementsList(UUID player);

	/**
	 * Retrieves the total number of achievements received by the player. Implementation benefits from Advanced
	 * Achievements caching when player is online.
	 * 
	 * @since 5.0
	 * @param player
	 * @return total achievements by the player
	 */
    int getPlayerTotalAchievements(UUID player);

	/**
	 * Retrieves the {@code Rank} object of a player over a given period.
	 * 
	 * @since 5.0
	 * @param player
	 * @param rankingPeriodStart time in millis since epoch; rank will be calculated for achievements received between
	 *            that starting point and now
	 * @return rank of the player; if no achievements were received over the period, his rank will be Integer.MAX_VALUE
	 */
    Rank getPlayerRank(UUID player, long rankingPeriodStart);

	/**
	 * Retrieves the players who have received the most achievements during a given period.
	 * 
	 * @since 5.0
	 * @param numOfPlayers to return in the list
	 * @param rankingPeriodStart time in millis since epoch; ranks will be calculated for achievements received between
	 *            that starting point and now
	 * @return list of players, ordered from best to worst
	 */
    List<UUID> getTopPlayers(int numOfPlayers, long rankingPeriodStart);

	/**
	 * Retrieves a statistic for a normal category. Implementation benefits from Advanced Achievements caching when
	 * player is online and if method called from server thread.
	 * 
	 * @since 5.0
	 * @param player
	 * @param category
	 * @return the statistic for the normal category
	 */
    long getStatisticForNormalCategory(UUID player, NormalAchievements category);

	/**
	 * Retrieves a statistic for a multiple category. Implementation benefits from Advanced Achievements caching when
	 * player is online and if method called from server thread.
	 * 
	 * @since 5.0
	 * @param player
	 * @param category
	 * @param subcategory within the main multiple category
	 * @return the statistic for the multiple category
	 */
    long getStatisticForMultipleCategory(UUID player, MultipleAchievements category, String subcategory);

	/**
	 * Returns the DisplayName parameter for a given achievement Name parameter. If no DisplayName was found for the
	 * achievement {@code achievementName}, an empty String is returned. If the achievement {@code achievementName} was
	 * not found in Advanced Achievements's configuration, null is returned.
	 * 
	 * @since 5.0
	 * @param achievementName as defined by the Name parameter in Advanced Achievements config.yml
	 * @return the DisplayName parameter of an achievement or "" or null
	 */
    String getDisplayNameForName(String achievementName);

	/**
	 * Retrieves the total numbers of achievements received by every player who has at least one achievement.
	 * 
	 * @since 5.2
	 * @return map containing total achievements for every player
	 */
    Map<UUID, Integer> getPlayersTotalAchievements();

	final class Rank {

		public final int playerRank;
		public final int totalPlayers;

		public Rank(int playerRank, int totalPlayers) {
			this.playerRank = playerRank;
			this.totalPlayers = totalPlayers;
		}
	}

	final class Achievement {

		public final String name;
		public final String message;
		public final String formattedDate;

		public Achievement(String name, String message, String formattedDate) {
			this.name = name;
			this.message = message;
			this.formattedDate = formattedDate;
		}
	}
}
