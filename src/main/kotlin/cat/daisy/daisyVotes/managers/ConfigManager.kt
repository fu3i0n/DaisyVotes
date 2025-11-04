package cat.daisy.daisyVotes.managers

import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.DaisyVotes.Companion.mainConfig
import cat.daisy.daisyVotes.utils.TextUtils.mm
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

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
        runCatching {
            DaisyVotes.instance.reloadConfig()
            loadConfigs()
            executor?.sendMessage("&aAll configs reloaded.".mm())
            DaisyVotes.instance.logger.info("All configs reloaded successfully.")
        }.onFailure { e ->
            executor?.sendMessage("&cFailed to reload configs: ${e.message}".mm())
            DaisyVotes.instance.logger.severe("Failed to reload configs: ${e.message}")
            throw e
        }
    }

    fun saveMainConfig() {
        runCatching {
            mainConfig.save(DaisyVotes.mainConfigFile)
        }.onFailure { e ->
            DaisyVotes.instance.logger.severe("Failed to save config ${DaisyVotes.mainConfigFile.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}
