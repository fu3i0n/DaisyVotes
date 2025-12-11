package cat.daisy.daisyVotes.commands

import cat.daisy.command.dsl.daisyCommand
import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.guis.openVoteMenu
import kotlinx.coroutines.launch

/**
 * Registers the /vote command.
 * Opens the vote GUI menu for players.
 */
fun registerVoteCommand() {
    daisyCommand("vote") {
        description = "Open the vote menu to support the server"
        playerOnly = true

        onExecute {
            DaisyVotes.instance.pluginScope.launch {
                openVoteMenu(player!!)
            }
        }
    }
}
