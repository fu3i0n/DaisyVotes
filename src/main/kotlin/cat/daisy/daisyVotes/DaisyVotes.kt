package cat.daisy.daisyVotes

import cat.daisy.command.core.DaisyCommands
import cat.daisy.daisyVotes.commands.registerReloadCommand
import cat.daisy.daisyVotes.commands.registerVoteCommand
import cat.daisy.daisyVotes.managers.ConfigManager
import cat.daisy.daisyVotes.managers.VoteManager
import cat.daisy.daisyVotes.utils.Database
import cat.daisy.daisyVotes.utils.PlaceHolders
import cat.daisy.daisyVotes.utils.TextUtils.log
import cat.daisy.menu.DaisyMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DaisyVotes - A modern, lightweight voting plugin for Minecraft servers.
 *
 * Features:
 * - Vote tracking with SQLite database
 * - Configurable vote party system
 * - PlaceholderAPI integration
 * - MiniMessage support for all messages
 * - Clean, modern GUI for vote links
 */
class DaisyVotes : JavaPlugin() {
    /** Plugin coroutine scope for async operations */
    val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val isShuttingDown = AtomicBoolean(false)

    /** Lazily instantiated listeners */
    private val listeners: List<() -> Listener> =
        listOf(
            { VoteManager() },
        )

    companion object {
        /** Plugin instance - available after onEnable */
        lateinit var instance: DaisyVotes
            private set

        /** Main configuration file */
        lateinit var mainConfig: FileConfiguration

        /** Main configuration file reference */
        lateinit var mainConfigFile: File
            private set
    }

    override fun onEnable() {
        instance = this
        val startTime = System.currentTimeMillis()

        runCatching {
            initializePlugin()

            val loadTime = System.currentTimeMillis() - startTime
            log("DaisyVotes v${pluginMeta.version} enabled in ${loadTime}ms!", "SUCCESS")
        }.onFailure { e ->
            log("Failed to enable DaisyVotes: ${e.message}", "ERROR", throwable = e)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        if (isShuttingDown.getAndSet(true)) return

        runCatching {
            shutdownPlugin()
            log("DaisyVotes disabled.", "INFO")
        }.onFailure { e ->
            log("Error during shutdown: ${e.message}", "ERROR", throwable = e)
        }
    }

    private fun initializePlugin() {
        // Initialize frameworks first
        DaisyCommands.initialize(this)
        DaisyMenu.initialize(this)

        // Setup configurations
        setupConfigurations()

        // Connect to database
        if (!Database.connect(dataFolder.absolutePath)) {
            throw IllegalStateException("Database connection failed")
        }

        // Register commands
        registerReloadCommand()
        registerVoteCommand()

        // Register events and integrations
        registerListeners()
        registerPlaceholders()

        // Log dependency status
        checkDependencies()
    }

    private fun shutdownPlugin() {
        // Shutdown in reverse order of initialization
        DaisyCommands.shutdown()
        DaisyMenu.shutdown()
        PlaceHolders.cleanup()
        Database.disconnect()
        pluginScope.cancel()
    }

    private fun setupConfigurations() {
        dataFolder.mkdirs()
        mainConfigFile = File(dataFolder, "config.yml")
        ConfigManager.loadConfigs()
    }

    private fun registerListeners() {
        listeners.forEach { listenerSupplier ->
            runCatching {
                val listener = listenerSupplier()
                server.pluginManager.registerEvents(listener, this)
                log("${listener::class.simpleName} registered", "SUCCESS")
            }.onFailure { e ->
                log("Failed to register listener: ${e.message}", "ERROR", throwable = e)
            }
        }
    }

    private fun registerPlaceholders() {
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            PlaceHolders().register()
            log("PlaceholderAPI integration enabled", "SUCCESS")
        }
    }

    private fun checkDependencies() {
        val missing = mutableListOf<String>()

        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            missing.add("PlaceholderAPI")
        }
        if (server.pluginManager.getPlugin("Votifier") == null &&
            server.pluginManager.getPlugin("NuVotifier") == null
        ) {
            missing.add("Votifier/NuVotifier")
        }

        if (missing.isNotEmpty()) {
            log("Missing optional dependencies: ${missing.joinToString()}", "WARNING")
        }
    }
}
