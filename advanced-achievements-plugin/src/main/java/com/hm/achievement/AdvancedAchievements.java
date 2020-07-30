package com.hm.achievement;

import com.hm.achievement.api.AdvancedAchievementsAPI;
import com.hm.achievement.api.AdvancedAchievementsBukkitAPI;
import com.hm.achievement.exception.PluginLoadError;
import com.hm.achievement.lifecycle.PluginLoader;
import com.hm.achievement.module.*;
import dagger.BindsInstance;
import dagger.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bukkit instantiates an instance of this class and calls the onEnable and onDisable methods when relevant. This class
 * is the root of the dependency graph constructed with Dagger and is used to bind the instance created by Bukkit with
 * the rest of the plugin modules. It delegates the actual enabling and disabling operations to the PluginLoader class.
 * 
 * @author Pyves
 */
public class AdvancedAchievements extends JavaPlugin {

	private PluginLoader pluginLoader;
	private AdvancedAchievementsAPI advancedAchievementsAPI;

	@Override
	public void onEnable() {
		long startTime = System.currentTimeMillis();
		// DaggerAdvancedAchievementsComponent is generated by Dagger. Add target/generated-sources/annotations to your
		// build path if the IDE complains here. In any case this will not actually prevent you from compiling.
		AdvancedAchievementsComponent advancedAchievementsComponent = DaggerAdvancedAchievementsComponent.builder()
				.advancedAchievements(this).logger(getLogger()).build();

		pluginLoader = advancedAchievementsComponent.pluginLoader();
		advancedAchievementsAPI = advancedAchievementsComponent.advancedAchievementsBukkitAPI();

		try {
			pluginLoader.loadAdvancedAchievements(true);
		} catch (PluginLoadError e) {
			getLogger().log(Level.SEVERE,
					"A non recoverable error was encountered while loading the plugin, disabling it:", e);
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		getLogger().info(
				"Plugin has finished loading and is ready to run! Took " + (System.currentTimeMillis() - startTime) + "ms.");
	}

	@Override
	public void onDisable() {
		pluginLoader.disableAdvancedAchievements();
	}

	public AdvancedAchievementsAPI getAdvancedAchievementsAPI() {
		return advancedAchievementsAPI;
	}
}

@Singleton
@Component(modules = {
		CleanableModule.class,
		CommandModule.class,
		ConfigModule.class,
		DatabaseModule.class,
		ReloadableModule.class,
		ServerVersionModule.class,
		UpdateCheckerModule.class,
})

interface AdvancedAchievementsComponent {

	PluginLoader pluginLoader();

	AdvancedAchievementsBukkitAPI advancedAchievementsBukkitAPI();

	@Component.Builder
	interface Builder {

		@BindsInstance
		Builder advancedAchievements(AdvancedAchievements advancedAchievements);

		@BindsInstance
		Builder logger(Logger logger);

		AdvancedAchievementsComponent build();
	}
}
