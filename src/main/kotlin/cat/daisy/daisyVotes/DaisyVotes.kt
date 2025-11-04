package cat.daisy.daisyVotes

import cat.daisy.daisyVotes.commands.registerReloadCommand
import cat.daisy.daisyVotes.managers.ConfigManager
import cat.daisy.daisyVotes.managers.VoteManager
import cat.daisy.daisyVotes.utils.Database
import cat.daisy.daisyVotes.utils.PlaceHolders
import cat.daisy.daisyVotes.utils.TextUtils.log
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class DaisyVotes : JavaPlugin() {
    private var isShuttingDown = false
    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = listOf({ VoteManager() })

    companion object {
        lateinit var instance: DaisyVotes
            private set
        lateinit var mainConfig: FileConfiguration
        lateinit var mainConfigFile: File
            private set
    }

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
    }

    override fun onEnable() {
        instance = this
        val startTime = System.currentTimeMillis()

        CommandAPI.onEnable()
        registerReloadCommand()

        if (Database.connect(dataFolder.absolutePath)) {
            setupConfigurations()
            registerEvents()
            registerPlaceholders()
            checkDependencies()

            val loadTime = System.currentTimeMillis() - startTime
            log("DaisyVotes plugin enabled successfully in ${loadTime}ms!", "SUCCESS")
        } else {
            log("Database connection failed. Disabling plugin.", "ERROR")
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        if (isShuttingDown) return
        isShuttingDown = true

        pluginScope.cancel("Plugin shutting down")
        CommandAPI.onDisable()
        PlaceHolders.cleanup()
        Database.disconnect()

        log("DaisyVotes plugin disabled.", "INFO")
    }

    private fun setupConfigurations() {
        dataFolder.mkdirs()
        mainConfigFile = File(dataFolder, "config.yml")
        mainConfig = ConfigManager.loadConfig(this, "config.yml")
        ConfigManager.saveMainConfig()
    }

    private fun registerEvents() {
        listeners.forEach { listenerSupplier ->
            runCatching {
                val listener = listenerSupplier()
                server.pluginManager.registerEvents(listener, this)
                log("${listener::class.simpleName} registered.", "SUCCESS")
            }.onFailure { e ->
                log("Listener registration failed", "ERROR", throwable = e)
            }
        }
    }

    private fun registerPlaceholders() {
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            PlaceHolders().register()
            log("PlaceholderAPI placeholders registered.", "SUCCESS")
        } else {
            log("PlaceholderAPI not found, placeholders will not work.", "WARNING")
        }
    }

    private fun checkDependencies() {
        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            log("Warning: Missing recommended dependencies: PlaceholderAPI", "WARNING")
        }
    }

    fun getPluginScope(): CoroutineScope = pluginScope
}
