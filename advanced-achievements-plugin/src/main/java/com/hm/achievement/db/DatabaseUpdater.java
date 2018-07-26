package com.hm.achievement.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;

import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.exception.PluginLoadError;
import com.hm.achievement.utils.MaterialHelper;

/**
 * Class used to update the database schema.
 * 
 * @author Pyves
 *
 */
@Singleton
public class DatabaseUpdater {

	private final Logger logger;
	private final int serverVersion;
	private final MaterialHelper materialHelper;

	@Inject
	DatabaseUpdater(Logger logger, int serverVersion, MaterialHelper materialHelper) {
		this.logger = logger;
		this.serverVersion = serverVersion;
		this.materialHelper = materialHelper;
	}

	/**
	 * Update the database table to use 1.13 materials rather than the old 1.12 ones.
	 * 
	 * @param databaseManager
	 */
	public void updateOldMaterialsToNewOnes(AbstractDatabaseManager databaseManager) {
		logger.info("Starting database upgrade to new Minecraft 1.13 material names, please wait...");
		updateOldMaterialsToNewOnes(databaseManager, MultipleAchievements.BREAKS);
		updateOldMaterialsToNewOnes(databaseManager, MultipleAchievements.CRAFTS);
		updateOldMaterialsToNewOnes(databaseManager, MultipleAchievements.PLACES);
		logger.info("Finished database upgrade.");
	}

	/**
	 * Renames the database tables with the prefix given in the configuration file. This method is only used and only
	 * works if the tables had the default name. It does not support multiple successive table renamings.
	 * 
	 * @param databaseManager
	 * @param databaseAddress
	 * @throws PluginLoadError
	 */
	void renameExistingTables(AbstractDatabaseManager databaseManager, String databaseAddress) throws PluginLoadError {
		// If a prefix is set in the config, check whether the tables with the default names exist. If so do renaming.
		if (StringUtils.isNotBlank(databaseManager.getPrefix())) {
			Connection conn = databaseManager.getSQLConnection();
			try (Statement st = conn.createStatement()) {
				ResultSet rs;
				if (databaseManager instanceof SQLiteDatabaseManager) {
					rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='achievements'");
				} else if (databaseManager instanceof MySQLDatabaseManager) {
					rs = st.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='"
							+ databaseAddress.substring(databaseAddress.lastIndexOf('/') + 1)
							+ "' AND table_name ='achievements'");
				} else {
					rs = st.executeQuery(
							"SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public' AND c.relname = 'achievements' AND c.relkind = 'r'");
				}

				// Table achievements still has its default name (ie. no prefix), but a prefix is set in the
				// configuration; do a renaming of all tables.
				if (rs.next()) {
					st.addBatch("ALTER TABLE achievements RENAME TO " + databaseManager.getPrefix() + "achievements");
					for (NormalAchievements category : NormalAchievements.values()) {
						st.addBatch("ALTER TABLE " + category.toDBName() + " RENAME TO " + databaseManager.getPrefix()
								+ category.toDBName());
					}
					for (MultipleAchievements category : MultipleAchievements.values()) {
						st.addBatch("ALTER TABLE " + category.toDBName() + " RENAME TO " + databaseManager.getPrefix()
								+ category.toDBName());
					}
					st.executeBatch();
				}
			} catch (SQLException e) {
				throw new PluginLoadError("Error while setting prefix of database tables.", e);
			}
		}
	}

	/**
	 * Initialises database tables by creating non existing ones. We batch the requests to send a unique batch to the
	 * database.
	 * 
	 * @param databaseManager
	 * @throws PluginLoadError
	 */
	void initialiseTables(AbstractDatabaseManager databaseManager) throws PluginLoadError {
		Connection conn = databaseManager.getSQLConnection();
		try (Statement st = conn.createStatement()) {
			st.addBatch("CREATE TABLE IF NOT EXISTS " + databaseManager.getPrefix()
					+ "achievements (playername char(36),achievement varchar(64),description varchar(128),date DATE,PRIMARY KEY (playername, achievement))");

			for (MultipleAchievements category : MultipleAchievements.values()) {
				st.addBatch("CREATE TABLE IF NOT EXISTS " + databaseManager.getPrefix() + category.toDBName()
						+ " (playername char(36)," + category.toSubcategoryDBName() + " varchar(51)," + category.toDBName()
						+ " INT,PRIMARY KEY(playername, " + category.toSubcategoryDBName() + "))");
			}

			for (NormalAchievements category : NormalAchievements.values()) {
				if (category == NormalAchievements.CONNECTIONS) {
					st.addBatch("CREATE TABLE IF NOT EXISTS " + databaseManager.getPrefix() + category.toDBName()
							+ " (playername char(36)," + category.toDBName()
							+ " INT,date varchar(10),PRIMARY KEY (playername))");
				} else {
					st.addBatch("CREATE TABLE IF NOT EXISTS " + databaseManager.getPrefix() + category.toDBName()
							+ " (playername char(36)," + category.toDBName() + " BIGINT,PRIMARY KEY (playername))");
				}
			}
			st.executeBatch();
		} catch (SQLException e) {
			throw new PluginLoadError("Error while initialising database tables.", e);
		}
	}

	/**
	 * Update the database tables for Breaks, Crafts and Places achievements (from int to varchar for identification
	 * column). The tables are now using material names and no longer item IDs, which are deprecated; this also allows
	 * to store extra data information, extending the number of items available for the user.
	 * 
	 * @param databaseManager
	 * @throws PluginLoadError
	 */
	void updateOldDBToMaterial(AbstractDatabaseManager databaseManager) throws PluginLoadError {
		Connection conn = databaseManager.getSQLConnection();
		String type = "";
		try (Statement st = conn.createStatement()) {
			ResultSet rs = st.executeQuery("SELECT blockid FROM " + databaseManager.getPrefix()
					+ MultipleAchievements.BREAKS.toDBName() + " LIMIT 1");
			type = rs.getMetaData().getColumnTypeName(1);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Database error while checking if Material upgrade is needed:", e);
		}

		// Old column type for versions prior to 2.4.1 was integer for SQLite and smallint unsigned for MySQL.
		if ("integer".equalsIgnoreCase(type) || "smallint unsigned".equalsIgnoreCase(type)) {
			logger.info("Updating database tables with Material names, please wait...");
			if (serverVersion < 13) {
				updateOldDBToMaterial(databaseManager, MultipleAchievements.BREAKS);
				updateOldDBToMaterial(databaseManager, MultipleAchievements.CRAFTS);
				updateOldDBToMaterial(databaseManager, MultipleAchievements.PLACES);
			} else {
				throw new PluginLoadError("The database must be updated using tools no longer available in Bukkit. "
						+ "Start this plugin build once with a Minecraft version prior to 1.13. "
						+ "You can then happily use Advanced Achievements with Minecraft 1.13+!");
			}
		}
	}

	/**
	 * Update the database tables for a MultipleAchievements category.
	 * 
	 * @param databaseManager
	 * @param category
	 */
	private void updateOldDBToMaterial(AbstractDatabaseManager databaseManager, MultipleAchievements category) {
		String tableName = databaseManager.getPrefix() + category.toDBName();
		Connection conn = databaseManager.getSQLConnection();
		try (Statement st = conn.createStatement()) {
			// Create new temporary table.
			st.execute("CREATE TABLE tempTable (playername char(36)," + category.toSubcategoryDBName() + " varchar(64),"
					+ tableName + " INT UNSIGNED,PRIMARY KEY(playername, " + category.toSubcategoryDBName() + "))");
			try (PreparedStatement prep = conn.prepareStatement("INSERT INTO tempTable VALUES (?,?,?);")) {
				ResultSet rs = st.executeQuery("SELECT * FROM " + tableName + "");
				List<String> uuids = new ArrayList<>();
				List<Integer> ids = new ArrayList<>();
				List<Integer> amounts = new ArrayList<>();

				while (rs.next()) {
					uuids.add(rs.getString(1));
					ids.add(rs.getInt(2));
					amounts.add(rs.getInt(3));
				}

				// Preallocate space in array containing the values in the new format.
				List<String> materials = new ArrayList<>(ids.size());

				for (int id : ids) {
					// Convert from ID to Material name. getMaterial(int id) is only available on Minecraft versions
					// prior to 1.13.
					Material material = (Material) Material.class.getMethod("getMaterial", int.class).invoke(null, id);
					materials.add(material.name().toLowerCase());
				}
				// Prevent from doing any commits before entire transaction is ready.
				conn.setAutoCommit(false);

				// Populate new table with contents of the old one and material strings. Batch the insert requests.
				for (int i = 0; i < uuids.size(); ++i) {
					prep.setString(1, uuids.get(i));
					prep.setString(2, materials.get(i));
					prep.setInt(3, amounts.get(i));
					prep.addBatch();
				}
				prep.executeBatch();
			} finally {
				// Delete old table.
				st.execute("DROP TABLE " + tableName);
				// Rename new table to old one.
				st.execute("ALTER TABLE tempTable RENAME TO " + tableName);
			}
			// Commit entire transaction.
			conn.commit();
			conn.setAutoCommit(true);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Database error while updating old table to remove Minecraft IDs:", e);
		}
	}

	/**
	 * Updates the database achievements table. The table is now using a date type for the date column. We also increase
	 * the number of chars allowed for the achievement names and descriptions.
	 * 
	 * @param databaseManager
	 */
	void updateOldDBToDates(AbstractDatabaseManager databaseManager) {
		Connection conn = databaseManager.getSQLConnection();
		try (Statement st = conn.createStatement()) {
			ResultSet rs = st.executeQuery("SELECT date FROM " + databaseManager.getPrefix() + "achievements LIMIT 1");
			String type = rs.getMetaData().getColumnTypeName(1);
			// Old column type for versions prior to 3.0 was text for SQLite, char for MySQL and varchar for PostgreSQL
			// (even though PostgreSQL was not supported on versions prior to 3.0, we still support the upgrade for it
			// in case a user imports another database into PostgreSQL without doing the table upgrade beforehand).
			if ("text".equalsIgnoreCase(type) || "char".equalsIgnoreCase(type) || "varchar".equalsIgnoreCase(type)) {
				logger.info("Updating database table with date datatype for achievements, please wait...");
				// Create new temporary table.
				st.execute("CREATE TABLE tempTable (playername char(36),achievement varchar(64),description varchar(128),"
						+ "date DATE,PRIMARY KEY (playername, achievement))");
				try (PreparedStatement prep = conn.prepareStatement("INSERT INTO tempTable VALUES (?,?,?,?);")) {
					// Early versions of the plugin added colors to the date. We have to get rid of them by using a
					// regex pattern, else parsing will fail.
					Pattern regexPattern = Pattern.compile("&([a-f]|[0-9]){1}");
					// Old date format, which was stored as a string.
					SimpleDateFormat oldFormat = new SimpleDateFormat("dd/MM/yyyy");
					// Load entire achievements table into memory.
					rs = st.executeQuery("SELECT * FROM " + databaseManager.getPrefix() + "achievements");
					List<String> uuids = new ArrayList<>();
					List<String> achs = new ArrayList<>();
					List<String> descs = new ArrayList<>();
					List<String> oldDates = new ArrayList<>();

					// Parse entire table into arrays.
					while (rs.next()) {
						uuids.add(rs.getString(1));
						achs.add(rs.getString(2));
						descs.add(rs.getString(3));
						oldDates.add(rs.getString(4));
					}

					// Preallocate space in array containing the values in the new format.
					List<Date> newDates = new ArrayList<>(oldDates.size());

					try {
						for (String date : oldDates) {
							// Convert to SQL date format.
							newDates.add(new Date(oldFormat.parse(regexPattern.matcher(date).replaceAll("")).getTime()));
						}
					} catch (ParseException e) {
						logger.log(Level.SEVERE, "Database error while parsing dates:", e);
					}
					// Prevent from doing any commits before entire transaction is ready.
					conn.setAutoCommit(false);

					// Populate new table with contents of the old one and date values.
					for (int i = 0; i < uuids.size(); ++i) {
						prep.setString(1, uuids.get(i));
						prep.setString(2, achs.get(i));
						prep.setString(3, descs.get(i));
						prep.setDate(4, newDates.get(i));
						prep.addBatch();
					}
					prep.executeBatch();
				} finally {
					// Delete old table.
					st.execute("DROP TABLE " + databaseManager.getPrefix() + "achievements");
					// Rename new table to old one.
					st.execute("ALTER TABLE tempTable RENAME TO " + databaseManager.getPrefix() + "achievements");
				}
				// Commit entire transaction.
				conn.commit();
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Database error while updating old connections table:", e);
		}
	}

	/**
	 * Increases size of the mobname column of the kills table to accommodate new parameters such as
	 * specificplayer-56c79b19-4500-466c-94ea-514a755fdd09.
	 * 
	 * @param databaseManager
	 */
	void updateOldDBMobnameSize(AbstractDatabaseManager databaseManager) {
		Connection conn = databaseManager.getSQLConnection();
		// SQLite ignores size for varchar datatype.
		if (!(databaseManager instanceof SQLiteDatabaseManager)) {
			int size = 51;
			try (Statement st = conn.createStatement()) {
				ResultSet rs = st.executeQuery("SELECT mobname FROM " + databaseManager.getPrefix() + "kills LIMIT 1");
				size = rs.getMetaData().getPrecision(1);
				// Old kills table prior to version 4.2.1 contained a capacity of only 32 chars.
				if (size == 32) {
					logger.info("Updating kills database table with new mobname column, please wait...");
					// Increase size of table.
					if (databaseManager instanceof PostgreSQLDatabaseManager) {
						st.execute("ALTER TABLE " + databaseManager.getPrefix()
								+ "kills ALTER COLUMN mobname TYPE varchar(51)");
					} else {
						st.execute("ALTER TABLE " + databaseManager.getPrefix() + "kills MODIFY mobname varchar(51)");
					}
				}
			} catch (SQLException e) {
				logger.log(Level.SEVERE, "Database error while updating old kills table:", e);
			}
		}
	}

	/**
	 * Update the database table to use 1.13 materials rather than the old 1.12 ones for a given Multiple category. This
	 * methods performs a best effort upgrade based on the functionality provided in the Bukkit.
	 * 
	 * @param databaseManager
	 * @param category
	 */
	private void updateOldMaterialsToNewOnes(AbstractDatabaseManager databaseManager, MultipleAchievements category) {
		String tableName = databaseManager.getPrefix() + category.toDBName();
		Connection conn = databaseManager.getSQLConnection();
		try (Statement st = conn.createStatement()) {
			// Create new temporary table.
			st.execute("CREATE TABLE tempTable (playername char(36)," + category.toSubcategoryDBName() + " varchar(64),"
					+ tableName + " INT UNSIGNED,PRIMARY KEY(playername, " + category.toSubcategoryDBName() + "))");
			try (PreparedStatement prep = conn.prepareStatement("INSERT INTO tempTable VALUES (?,?,?);")) {
				ResultSet rs = st.executeQuery("SELECT * FROM " + tableName + "");
				List<String> uuids = new ArrayList<>();
				List<String> materialKeys = new ArrayList<>();
				List<Integer> amounts = new ArrayList<>();

				while (rs.next()) {
					uuids.add(rs.getString(1));
					materialKeys.add(convertToNewMaterialKey(rs.getString(2)));
					amounts.add(rs.getInt(3));
				}
				// Prevent from doing any commits before entire transaction is ready.
				conn.setAutoCommit(false);

				// Populate new table with contents of the old one and material strings. Batch the insert requests.
				for (int i = 0; i < uuids.size(); ++i) {
					prep.setString(1, uuids.get(i));
					prep.setString(2, materialKeys.get(i));
					prep.setInt(3, amounts.get(i));
					prep.addBatch();
				}
				prep.executeBatch();
			} finally {
				// Delete old table.
				st.execute("DROP TABLE " + tableName);
				// Rename new table to old one.
				st.execute("ALTER TABLE tempTable RENAME TO " + tableName);
			}
			// Commit entire transaction.
			conn.commit();
			conn.setAutoCommit(true);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Database error while updating old material names to new Minecraft 1.13 ones:", e);
		}
	}

	/**
	 * Converts a material key containing pre-1.13 materials (e.g. "workbench|explosive_minecart") to an equivalent key
	 * with 1.13 materials (e.g. "crafting_table|tnt_minecart").
	 * 
	 * @param oldMaterialKey
	 * @return a key with 1.13 material names.
	 */
	private String convertToNewMaterialKey(String oldMaterialKey) {
		List<String> newMaterials = new ArrayList<>();
		for (String oldMaterial : oldMaterialKey.split("\\|")) {
			String[] parts = oldMaterial.split(":");
			Optional<Material> material = materialHelper.matchMaterial(parts[0], "the database (" + oldMaterialKey + ")");
			if (material.isPresent()) {
				newMaterials.add(parts.length > 1 ? material.get().name().toLowerCase() + ":" + parts[1]
						: material.get().name().toLowerCase());
			} else {
				newMaterials.add(oldMaterial);
			}
		}
		return StringUtils.join(newMaterials, "|");
	}
}
