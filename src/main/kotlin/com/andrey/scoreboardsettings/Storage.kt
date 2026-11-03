package com.andrey.scoreboardsettings

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class Storage(private val plugin: JavaPlugin, section: ConfigurationSection) {
    private val mysql = section.getString("type", "sqlite").equals("mysql", ignoreCase = true)

    private val source = HikariDataSource(HikariConfig().apply {
        poolName = "ScoreboardSettings"
        if (mysql) {
            driverClassName = "com.mysql.cj.jdbc.Driver"
            jdbcUrl = "jdbc:mysql://${section.getString("mysql.host", "localhost")}:${section.getInt("mysql.port", 3306)}/${section.getString("mysql.database", "minecraft")}"
            username = section.getString("mysql.username", "root")
            password = section.getString("mysql.password", "")
            maximumPoolSize = 4
        } else {
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = "jdbc:sqlite:" + File(plugin.dataFolder, "data.db").absolutePath
            maximumPoolSize = 1
        }
    })

    private val executor = Executors.newSingleThreadExecutor { Thread(it, "ScoreboardSettings Storage") }

    init {
        source.connection.use { connection ->
            connection.createStatement().use {
                it.executeUpdate("CREATE TABLE IF NOT EXISTS sbsettings_players (uuid VARCHAR(36) PRIMARY KEY, hidden TEXT NOT NULL)")
            }
        }
    }

    fun load(uuid: UUID, done: (Set<String>) -> Unit) {
        executor.execute {
            val hidden = runCatching {
                source.connection.use { connection ->
                    connection.prepareStatement("SELECT hidden FROM sbsettings_players WHERE uuid = ?").use { statement ->
                        statement.setString(1, uuid.toString())
                        statement.executeQuery().use { result ->
                            if (result.next()) result.getString(1).split(',').filter { it.isNotEmpty() }.toSet() else emptySet()
                        }
                    }
                }
            }.getOrElse {
                plugin.logger.log(Level.WARNING, "Could not load the scoreboard settings of $uuid", it)
                emptySet()
            }
            done(hidden)
        }
    }

    fun save(uuid: UUID, hidden: Set<String>) {
        val value = hidden.joinToString(",")
        executor.execute {
            runCatching {
                source.connection.use { connection ->
                    val sql = if (mysql) {
                        "INSERT INTO sbsettings_players (uuid, hidden) VALUES (?, ?) ON DUPLICATE KEY UPDATE hidden = VALUES(hidden)"
                    } else {
                        "INSERT INTO sbsettings_players (uuid, hidden) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET hidden = excluded.hidden"
                    }
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, uuid.toString())
                        statement.setString(2, value)
                        statement.executeUpdate()
                    }
                }
            }.onFailure { plugin.logger.log(Level.WARNING, "Could not save the scoreboard settings of $uuid", it) }
        }
    }

    fun close() {
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
        source.close()
    }
}
