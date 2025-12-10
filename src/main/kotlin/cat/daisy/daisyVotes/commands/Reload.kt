package cat.daisy.daisyVotes.commands

import cat.daisy.command.dsl.daisyCommand
import cat.daisy.daisyVotes.managers.ConfigManager

fun registerReloadCommand() {
    daisyCommand("daisyvotes") {
        description = "DaisyVotes main command"
        permission = "daisyvotes.use"
        withAliases("dv")

        subcommand("reload") {
            description = "Reload configuration"
            permission = "daisyvotes.reload"

            onExecute {
                ConfigManager.reloadConfigs(player)
                success("Configuration reloaded!")
            }
        }

        onExecute {
            info("DaisyVotes Commands:")
            send("<gray>▸ <white>/daisyvotes reload <dark_gray>- Reload configuration")
        }
    }
}
