package wtf.amari.daisyVotes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import wtf.amari.daisyVotes.managers.ConfigManager
import wtf.amari.daisyVotes.managers.VoteManager
import wtf.amari.daisyVotes.utils.Database
import wtf.amari.daisyVotes.utils.PlaceHolders
import wtf.amari.daisyVotes.utils.TextUtils.log
import java.io.File

class DaisyVotes : JavaPlugin() {
    private var isShuttingDown = false
    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onEnable() {
        instance = this

        val startTime = System.currentTimeMillis()

        if (Database.connect(dataFolder.absolutePath)) {
            initializePlugin()

            val loadTime = System.currentTimeMillis() - startTime
            log("DaisyVotes plugin enabled successfully in ${loadTime}ms!", "SUCCESS")
        } else {
            log("Database connection failed. Disabling plugin.", "ERROR")
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        if (!isShuttingDown) {
            isShuttingDown = true

            pluginScope.cancel("Plugin shutting down")

            cleanupResources()
            Database.disconnect()

            log("DaisyVotes plugin disabled.", "INFO")
        }
    }

    private fun initializePlugin() {
        setupConfigurations()
        registerComponents()
        checkDependencies()
    }

    private fun setupConfigurations() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        mainConfigFile = File(dataFolder, "config.yml")
        mainConfig = ConfigManager.loadConfig(this, "config.yml")
        ConfigManager.saveMainConfig()
    }

    private fun registerComponents() {
        registerEvents()
        registerPlaceholders()
    }

    private fun checkDependencies() {
        val missingDependencies = mutableListOf<String>()

        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            missingDependencies.add("PlaceholderAPI")
        }

        if (missingDependencies.isNotEmpty()) {
            log(
                "Warning: Missing recommended dependencies: ${missingDependencies.joinToString(", ")}",
                "WARNING",
            )
        }
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
        val placeholderAPI = server.pluginManager.getPlugin("PlaceholderAPI")
        if (placeholderAPI != null) {
            PlaceHolders().register()
            log("PlaceholderAPI placeholders registered.", "SUCCESS")
        } else {
            log("PlaceholderAPI not found, placeholders will not work.", "WARNING")
        }
    }

    private fun cleanupResources() {
        PlaceHolders.cleanup()
    }

    fun getPluginScope(): CoroutineScope = pluginScope

    companion object {
        lateinit var instance: DaisyVotes
            private set
        lateinit var mainConfig: FileConfiguration
        lateinit var mainConfigFile: File
            private set
    }

    private val listeners =
        listOf(
            { VoteManager() },
        )
}
