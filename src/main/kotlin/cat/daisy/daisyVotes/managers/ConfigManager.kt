package cat.daisy.daisyVotes.managers

import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.DaisyVotes.Companion.mainConfig
import cat.daisy.daisyVotes.utils.TextUtils.log
import cat.daisy.daisyVotes.utils.TextUtils.mm
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized configuration management for DaisyVotes.
 * Handles loading, caching, reloading, and saving of all config files.
 */
object ConfigManager {
    private val configCache = ConcurrentHashMap<String, CachedConfig>()

    /** GUI configuration - loaded from gui.yml */
    lateinit var guiConfig: FileConfiguration
        private set

    private val plugin: DaisyVotes
        get() = DaisyVotes.instance

    /**
     * Loads a configuration file, using cache if available and not stale.
     */
    fun loadConfig(
        fileName: String,
        forceReload: Boolean = false,
    ): FileConfiguration {
        if (!forceReload) {
            configCache[fileName]?.let { cached ->
                if (!cached.isStale()) return cached.config
            }
        }

        val file = File(plugin.dataFolder, fileName)

        // Save default resource if file doesn't exist
        if (!file.exists()) {
            runCatching {
                plugin.saveResource(fileName, false)
            }.onFailure { e ->
                log("Failed to save default resource '$fileName': ${e.message}", "WARNING")
            }
        }

        val config = YamlConfiguration.loadConfiguration(file)
        configCache[fileName] = CachedConfig(config, file)

        return config
    }

    /**
     * Loads all configuration files required by the plugin.
     */
    fun loadConfigs() {
        configCache.clear()
        mainConfig = loadConfig("config.yml")
        guiConfig = loadConfig("gui.yml")
        log("All configurations loaded successfully", "SUCCESS")
    }

    /**
     * Reloads all configurations and notifies the executor.
     */
    fun reloadConfigs(executor: Player? = null) {
        runCatching {
            loadConfigs()

            executor?.sendMessage("<green>✔ All configs reloaded successfully.".mm())
            log("Configurations reloaded by ${executor?.name ?: "Console"}", "SUCCESS")
        }.onFailure { e ->
            val errorMsg = "Failed to reload configs: ${e.message}"
            executor?.sendMessage("<red>✖ $errorMsg".mm())
            log(errorMsg, "ERROR", throwable = e)
        }
    }

    /**
     * Saves the main configuration file.
     */
    @Suppress("unused") // Public API
    fun saveMainConfig() {
        saveConfig("config.yml", mainConfig)
    }

    /**
     * Saves a configuration to its file.
     */
    fun saveConfig(
        fileName: String,
        config: FileConfiguration,
    ) {
        runCatching {
            val file = File(plugin.dataFolder, fileName)
            config.save(file)
        }.onFailure { e ->
            log("Failed to save config '$fileName': ${e.message}", "ERROR", throwable = e)
        }
    }

    /**
     * Gets a cached config file, or null if not loaded.
     */
    @Suppress("unused") // Public API
    fun getConfig(fileName: String): FileConfiguration? = configCache[fileName]?.config

    /**
     * Clears the config cache (useful for testing or forced reload).
     */
    @Suppress("unused") // Public API
    fun clearCache() {
        configCache.clear()
    }

    /**
     * Wrapper for cached configuration with staleness detection.
     */
    private data class CachedConfig(
        val config: FileConfiguration,
        val file: File,
        val loadedAt: Long = System.currentTimeMillis(),
    ) {
        fun isStale(): Boolean = file.lastModified() > loadedAt
    }
}
