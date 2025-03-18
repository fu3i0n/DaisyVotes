package wtf.amari.daisyVotes.managers

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import wtf.amari.daisyVotes.DaisyVotes
import wtf.amari.daisyVotes.DaisyVotes.Companion.mainConfig
import wtf.amari.daisyVotes.utils.TextUtils.mm
import java.io.File
import java.io.IOException

object ConfigManager {
    private val configCache = mutableMapOf<String, FileConfiguration>()

    fun loadConfig(
        plugin: JavaPlugin,
        fileName: String,
    ): FileConfiguration =
        configCache.getOrPut(fileName) {
            val file = File(plugin.dataFolder, fileName)
            if (!file.exists()) {
                plugin.saveResource(fileName, false)
            }
            YamlConfiguration.loadConfiguration(file)
        }

    fun loadConfigs() {
        configCache.clear()
        mainConfig = loadConfig(DaisyVotes.instance, "config.yml")
    }

    fun reloadConfigs(executor: Player? = null) {
        try {
            DaisyVotes.instance.reloadConfig()
            loadConfigs()
            executor?.sendMessage("&aAll configs reloaded.".mm())
            DaisyVotes.instance.logger.info("All configs reloaded successfully.")
        } catch (e: Exception) {
            executor?.sendMessage("&cFailed to reload configs: ${e.message}".mm())
            DaisyVotes.instance.logger.severe("Failed to reload configs: ${e.message}")
            throw e
        }
    }

    private fun saveConfig(
        config: FileConfiguration,
        file: File,
    ) {
        try {
            config.save(file)
        } catch (e: IOException) {
            DaisyVotes.instance.logger.severe("Failed to save config ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    fun saveMainConfig() {
        saveConfig(DaisyVotes.mainConfig, DaisyVotes.mainConfigFile)
    }

    fun saveAllConfigs() {
        saveMainConfig()
    }
}
