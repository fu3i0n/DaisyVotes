package cat.daisy.daisyVotes.commands

import cat.daisy.daisyVotes.managers.ConfigManager
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.subcommand

fun registerReloadCommand() {
    commandAPICommand("daisyvotes") {
        subcommand("reload") {
            withPermission("daisyvotes.reload")
            anyExecutor { sender, _ ->
                ConfigManager.reloadConfigs()
            }
        }
    }
}
