package cat.daisy.daisyVotes.commands

import cat.daisy.command.dsl.daisyCommand
import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.managers.ConfigManager
import cat.daisy.daisyVotes.managers.VoteManager

/**
 * Registers the /daisyvotes admin command.
 * Provides reload and status functionality.
 */
fun registerReloadCommand() {
    daisyCommand("daisyvotes") {
        description = "DaisyVotes admin commands"
        permission = "daisyvotes.admin"
        withAliases("dv", "dvotes")

        subcommand("reload") {
            description = "Reload all configuration files"
            permission = "daisyvotes.admin.reload"

            onExecute {
                ConfigManager.reloadConfigs(player)
            }
        }

        subcommand("status") {
            description = "View current vote status"
            permission = "daisyvotes.admin.status"

            onExecute {
                val current = VoteManager.currentVotes.get()
                val needed = DaisyVotes.mainConfig.getInt("voteparty.totalvotes", 25)
                val remaining = (needed - current).coerceAtLeast(0)

                info("Vote Party Status:")
                send("<gray>▸ <white>Current Votes: <green>$current")
                send("<gray>▸ <white>Votes Needed: <yellow>$needed")
                send("<gray>▸ <white>Remaining: <aqua>$remaining")
            }
        }

        subcommand("reset") {
            description = "Reset the vote counter"
            permission = "daisyvotes.admin.reset"

            onExecute {
                VoteManager.resetVotes()
                success("Vote counter has been reset to 0!")
            }
        }

        // Default action - show help
        onExecute {
            info("DaisyVotes v${DaisyVotes.instance.pluginMeta.version}")
            send("<gray>▸ <white>/dv reload <dark_gray>- Reload configuration")
            send("<gray>▸ <white>/dv status <dark_gray>- View vote status")
            send("<gray>▸ <white>/dv reset <dark_gray>- Reset vote counter")
        }
    }
}
