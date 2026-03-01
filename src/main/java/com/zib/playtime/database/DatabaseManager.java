package com.zib.playtime.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zib.playtime.Playtime;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.config.Reward;

public class DatabaseManager {

    private HikariDataSource dataSource;
    private final File dataFolder;
    private boolean isMySQL;

    private final Logger logger = LoggerFactory.getLogger("Playtime-DB");

    public boolean isMySQL() {
        return isMySQL;
    }

    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void init() {
        PlaytimeConfig.DatabaseSettings settings = Playtime.get().getConfigManager().getConfig().database;
        HikariConfig config = new HikariConfig();

        if (settings.type.equalsIgnoreCase("mysql")) {
            config.setJdbcUrl("jdbc:mysql://" + settings.host + ":" + settings.port + "/" + settings.databaseName + "?useSSL=" + settings.useSSL);
            config.setUsername(settings.username);
            config.setPassword(settings.password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            isMySQL = true;
            logger.info("Connecting to MySQL Database...");
        } else {
            if (!dataFolder.exists()) dataFolder.mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "playtime.db").getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            isMySQL = false;
            logger.info("Using local SQLite Database.");
        }

        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        createTable();
    }

    private void createTable() {
        String sessionsSql;
        String rewardsSql;
        String milestonesSql;

        if (isMySQL) {
            sessionsSql = "CREATE TABLE IF NOT EXISTS playtime_sessions (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid VARCHAR(36)," +
                    "username VARCHAR(16)," +
                    "start_time BIGINT," +
                    "duration BIGINT," +
                    "session_date DATE" +
                    ")";

            rewardsSql = "CREATE TABLE IF NOT EXISTS playtime_rewards_log (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid VARCHAR(36)," +
                    "reward_id VARCHAR(64)," +
                    "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            milestonesSql = "CREATE TABLE IF NOT EXISTS playtime_milestones_log (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "milestone_id VARCHAR(64) NOT NULL," +
                    "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        } else {
            sessionsSql = "CREATE TABLE IF NOT EXISTS playtime_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36)," +
                    "username VARCHAR(16)," +
                    "start_time BIGINT," +
                    "duration BIGINT," +
                    "session_date DATE DEFAULT CURRENT_DATE" +
                    ")";

            rewardsSql = "CREATE TABLE IF NOT EXISTS playtime_rewards_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36)," +
                    "reward_id VARCHAR(64)," +
                    "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            milestonesSql = "CREATE TABLE IF NOT EXISTS playtime_milestones_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "milestone_id VARCHAR(64) NOT NULL," +
                    "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        }

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sessionsSql);
            stmt.execute(rewardsSql);
            stmt.execute(milestonesSql);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_uuid ON playtime_sessions(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_date ON playtime_sessions(session_date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_uuid_date ON playtime_sessions(uuid, session_date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_rewards_uuid ON playtime_rewards_log(uuid, reward_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_milestones_uuid ON playtime_milestones_log(uuid, milestone_id)");
            logger.info("Successfully created/verified database tables.");
        } catch (SQLException e) {
            logger.error("Failed to create table: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create database table", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return getConnectionWithRetry(3);
    }

    private Connection getConnectionWithRetry(int maxAttempts) throws SQLException {
        SQLException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                lastException = e;
                logger.warn("Database connection attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt - 1) * 100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw lastException;
    }

    public boolean isHealthy() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public void close() {
        if (dataSource != null) dataSource.close();
    }

    // NEW: Check if a reward is already claimed for the current period
    public boolean hasClaimedReward(String uuid, Reward reward) {
        String timeClause = "";

        // Determine SQL logic for periods
        if (isMySQL) {
            if (reward.period.equalsIgnoreCase("daily")) {
                timeClause = " AND DATE(claim_date) = CURDATE()";
            } else if (reward.period.equalsIgnoreCase("weekly")) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
            } else if (reward.period.equalsIgnoreCase("monthly")) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            }
        } else {
            // SQLite
            if (reward.period.equalsIgnoreCase("daily")) {
                timeClause = " AND date(claim_date) = date('now')";
            } else if (reward.period.equalsIgnoreCase("weekly")) {
                timeClause = " AND date(claim_date) >= date('now', '-7 days')";
            } else if (reward.period.equalsIgnoreCase("monthly")) {
                timeClause = " AND date(claim_date) >= date('now', '-1 month')";
            }
        }

        String query = "SELECT id FROM playtime_rewards_log WHERE uuid = ? AND reward_id = ?" + timeClause;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, reward.id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking reward claim", e);
            return true; // Fail safe: assume claimed to prevent exploit on error
        }
    }

    public void logRewardClaim(String uuid, String rewardId) {
        String sql = "INSERT INTO playtime_rewards_log (uuid, reward_id) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, rewardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error logging reward claim", e);
        }
    }

    /**
     * Check if a milestone has been claimed by a player.
     */
    public boolean hasMilestoneClaimed(String uuid, String milestoneId) {
        String query = "SELECT id FROM playtime_milestones_log WHERE uuid = ? AND milestone_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, milestoneId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking milestone claim", e);
            return true; // Fail safe: assume claimed to prevent exploit
        }
    }

    /**
     * Check if a milestone has been claimed within the current period (for repeatable milestones).
     */
    public boolean hasMilestoneClaimedInPeriod(String uuid, String milestoneId, String period) {
        String timeClause = "";

        if (isMySQL) {
            if (period.equalsIgnoreCase("daily")) {
                timeClause = " AND DATE(claim_date) = CURDATE()";
            } else if (period.equalsIgnoreCase("weekly")) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
            } else if (period.equalsIgnoreCase("monthly")) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            }
            // "all" period: no time clause, same as hasMilestoneClaimed
        } else {
            if (period.equalsIgnoreCase("daily")) {
                timeClause = " AND date(claim_date) = date('now')";
            } else if (period.equalsIgnoreCase("weekly")) {
                timeClause = " AND date(claim_date) >= date('now', '-7 days')";
            } else if (period.equalsIgnoreCase("monthly")) {
                timeClause = " AND date(claim_date) >= date('now', '-1 month')";
            }
        }

        String query = "SELECT id FROM playtime_milestones_log WHERE uuid = ? AND milestone_id = ?" + timeClause;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, milestoneId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking milestone claim for period", e);
            return true; // Fail safe
        }
    }

    /**
     * Log a milestone claim for a player.
     */
    public void logMilestoneClaim(String uuid, String milestoneId) {
        String sql = "INSERT INTO playtime_milestones_log (uuid, milestone_id) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, milestoneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error logging milestone claim", e);
        }
    }
}