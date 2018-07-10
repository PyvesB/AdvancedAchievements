package com.hm.achievement.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import com.hm.achievement.AdvancedAchievements;
import com.hm.mcshared.file.CommentedYamlConfiguration;

import dagger.Module;
import dagger.Provides;

@Module
public class ConfigModule {

	@Provides
	@Singleton
	Map<String, List<Long>> provideSortedThresholds() {
		return new HashMap<>();
	}

	@Provides
	@Singleton
	Map<String, String> provideAchievementsAndDisplayNames() {
		return new HashMap<>();
	}

	@Provides
	@Singleton
	Set<String> provideDisabledCategories() {
		return new HashSet<>();
	}

	@Provides
	@Singleton
	StringBuilder providePluginHeader() {
		return new StringBuilder();
	}

	@Provides
	@Singleton
	@Named("main")
	CommentedYamlConfiguration providesMainConfig(AdvancedAchievements advancedAchievements) {
		return new CommentedYamlConfiguration("config.yml", advancedAchievements);
	}

	@Provides
	@Singleton
	@Named("lang")
	CommentedYamlConfiguration providesLangConfig(@Named("main") CommentedYamlConfiguration mainConfig,
			AdvancedAchievements advancedAchievements) {
		return new CommentedYamlConfiguration(mainConfig.getString("LanguageFileName", "lang.yml"),
				advancedAchievements);
	}

	@Provides
	@Singleton
	@Named("gui")
	CommentedYamlConfiguration providesGuiConfig(AdvancedAchievements advancedAchievements) {
		return new CommentedYamlConfiguration("gui.yml", advancedAchievements);
	}

}
