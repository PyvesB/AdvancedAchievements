package com.hm.achievement.db;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.exception.PluginLoadError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import utilities.MockUtility;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Class for testing SQLite Database.
 *
 * @author Rsl1122
 */
@RunWith(MockitoJUnitRunner.class)
public class SQLiteDatabaseManagerTest {

    private SQLiteDatabaseManager db;

    private final UUID testUUID = UUID.randomUUID();
    private final String testAchievement = "TestAchievement";
    private final String testAchievementMsg = "TestMessage";

    @Before
    public void setUp() throws Exception {
        MockUtility mockUtility = MockUtility.setUp()
                .mockLogger()
                .mockPluginConfig();
        AdvancedAchievements pluginMock = mockUtility.getPluginMock();

        db = new SQLiteDatabaseManager(pluginMock);
    }

    @After
    public void tearDown() throws Exception {
        if (db != null) {
            db.shutdown();
        }
    }

    @Test
    public void testInitialization() throws PluginLoadError {
        initDB();
    }

    private void initDB() throws PluginLoadError {
        db.initialise();
        db.extractConfigurationParameters();
    }

    @Test
    public void testGetAchievementList() throws PluginLoadError {
        initDB();
        registerAchievement();

        List<String> achievements = db.getPlayerAchievementsList(testUUID);
        assertFalse("List was empty", achievements.isEmpty());

        String actual = achievements.get(0);
        System.out.println("Saved Achievement: " + actual);
        assertEquals(testAchievement, actual);

    }

    private void registerAchievement() {
        System.out.println("Saving test achievement: " + testUUID + " | " + testAchievement + " | " + testAchievementMsg);
        db.registerAchievement(testUUID, testAchievement, testAchievementMsg);
    }

    @Test
    public void testAchievementCount() throws PluginLoadError {
        initDB();
        registerAchievement();

        Map<UUID, Integer> map = db.getPlayersAchievementsAmount();
        assertFalse("Map was empty", map.isEmpty());

        Integer amount = map.get(testUUID);
        assertNotNull(amount);
        assertEquals(1, (int) amount);
    }

    @Test
    public void test() throws PluginLoadError {
        initDB();
        registerAchievement();

        String date = db.getPlayerAchievementDate(testUUID, testAchievement);

        assertNotNull(date);
        assertEquals("", date);
    }
}