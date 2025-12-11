/*
 * Vote Menu - Configurable GUI for displaying vote links.
 *
 * All menu elements are customizable via gui.yml:
 * - Menu title, size, and background
 * - Vote sites (add/remove/modify freely)
 * - Info and exit buttons
 * - Full MiniMessage support for colors and formatting
 */
package cat.daisy.daisyVotes.guis

import cat.daisy.daisyVotes.managers.ConfigManager
import cat.daisy.daisyVotes.utils.TextUtils.mm
import cat.daisy.menu.openMenu
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Configuration loader for the vote menu.
 * Reads all settings from gui.yml with sensible defaults.
 */
private object VoteMenuConfig {
    private val config get() = ConfigManager.guiConfig
    private val section get() = config.getConfigurationSection("vote-menu")

    val title: String
        get() = section?.getString("title") ?: "<#FF4C4C><bold>🗳 Vote Menu"

    val rows: Int
        get() = section?.getInt("rows", 3)?.coerceIn(1, 6) ?: 3

    val linkMessage: String
        get() =
            section?.getString("link-message")
                ?: "<gradient:#FF4C4C:#FF1A1A>🔗 Vote: <click:open_url:'%url%'>%url%</click></gradient>"

    object Background {
        private val sec get() = section?.getConfigurationSection("background")
        val enabled get() = sec?.getBoolean("enabled", true) ?: true
        val material get() = sec?.getString("material")?.toMaterial() ?: Material.GRAY_STAINED_GLASS_PANE
        val name get() = sec?.getString("name") ?: " "
    }

    object Info {
        private val sec get() = section?.getConfigurationSection("info")
        val enabled get() = sec?.getBoolean("enabled", true) ?: true
        val slot get() = sec?.getInt("slot", 4) ?: 4
        val material get() = sec?.getString("material")?.toMaterial() ?: Material.BOOK
        val name get() = sec?.getString("name") ?: "<#F5C842><bold>ℹ Vote Info"
        val lore get() = sec?.getStringList("lore") ?: emptyList()
    }

    object Exit {
        private val sec get() = section?.getConfigurationSection("exit")
        val enabled get() = sec?.getBoolean("enabled", true) ?: true
        val slot get() = sec?.getInt("slot", 22) ?: 22
        val material get() = sec?.getString("material")?.toMaterial() ?: Material.BARRIER
        val name get() = sec?.getString("name") ?: "<#FF6B6B><bold>❌ Exit"
        val lore get() = sec?.getStringList("lore") ?: emptyList()
    }

    /** Loads all vote sites from config */
    fun getVoteSites(): List<VoteSite> {
        val sites = section?.getConfigurationSection("vote-sites") ?: return emptyList()

        return sites.getKeys(false).mapNotNull { key ->
            val site = sites.getConfigurationSection(key) ?: return@mapNotNull null
            val link = site.getString("link")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val material = site.getString("material")?.toMaterial() ?: return@mapNotNull null

            VoteSite(
                slot = site.getInt("slot", 0),
                material = material,
                name = site.getString("name") ?: "<white>Vote Site",
                lore = site.getStringList("lore"),
                link = link,
            )
        }
    }

    private fun String.toMaterial(): Material? = runCatching { Material.valueOf(uppercase()) }.getOrNull()
}

/** Data class for vote site configuration */
private data class VoteSite(
    val slot: Int,
    val material: Material,
    val name: String,
    val lore: List<String>,
    val link: String,
)

/** Check if PlaceholderAPI is available */
private val hasPAPI: Boolean by lazy {
    Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
}

/** Parse PlaceholderAPI placeholders in a string */
private fun String.parsePlaceholders(player: Player): String = if (hasPAPI) PlaceholderAPI.setPlaceholders(player, this) else this

/** Parse PlaceholderAPI placeholders in a list of strings */
private fun List<String>.parsePlaceholders(player: Player): List<String> =
    if (hasPAPI) map { PlaceholderAPI.setPlaceholders(player, it) } else this

/**
 * Opens the vote menu for a player.
 * Configuration is loaded fresh from gui.yml each time.
 * PlaceholderAPI placeholders are parsed in lore text.
 */
internal suspend fun openVoteMenu(player: Player) {
    val sites = VoteMenuConfig.getVoteSites()
    val linkMsg = VoteMenuConfig.linkMessage

    player.openMenu {
        title = VoteMenuConfig.title
        rows = VoteMenuConfig.rows

        // Background filler
        if (VoteMenuConfig.Background.enabled) {
            fill(VoteMenuConfig.Background.material) {
                name(VoteMenuConfig.Background.name)
            }
        }

        // Info display with parsed placeholders
        if (VoteMenuConfig.Info.enabled) {
            slot(VoteMenuConfig.Info.slot) {
                item(VoteMenuConfig.Info.material) {
                    name(VoteMenuConfig.Info.name.parsePlaceholders(player))
                    lore(VoteMenuConfig.Info.lore.parsePlaceholders(player))
                }
            }
        }

        // Vote site buttons
        sites.forEach { site ->
            slot(site.slot) {
                item(site.material) {
                    name(site.name.parsePlaceholders(player))
                    lore(site.lore.parsePlaceholders(player))
                }
                onClick { p ->
                    p.sendMessage(linkMsg.replace("%url%", site.link).mm())
                    p.closeInventory()
                }
            }
        }

        // Exit button
        if (VoteMenuConfig.Exit.enabled) {
            slot(VoteMenuConfig.Exit.slot) {
                item(VoteMenuConfig.Exit.material) {
                    name(VoteMenuConfig.Exit.name.parsePlaceholders(player))
                    lore(VoteMenuConfig.Exit.lore.parsePlaceholders(player))
                }
                onClick { p -> p.closeInventory() }
            }
        }
    }
}
