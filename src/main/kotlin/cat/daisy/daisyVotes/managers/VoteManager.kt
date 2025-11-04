package cat.daisy.daisyVotes.managers

import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.utils.Database
import cat.daisy.daisyVotes.utils.TextUtils.log
import cat.daisy.daisyVotes.utils.TextUtils.mm
import com.vexsoftware.votifier.model.VotifierEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.concurrent.atomic.AtomicInteger

class VoteManager : Listener {
    private val config = DaisyVotes.mainConfig

    companion object {
        val currentVotes = AtomicInteger(Database.getVoteCount())
    }

    private val totalVotesNeeded: Int
        get() = config.getInt("voteparty.totalvotes", 25)

    @EventHandler
    fun onVote(event: VotifierEvent) {
        runCatching {
            handleVote(event.vote.username)
        }.onFailure { e ->
            log("Error handling vote for player ${event.vote.username}: ${e.message}", "ERROR")
        }
    }

    private fun handleVote(playerName: String) {
        val player =
            Bukkit.getPlayer(playerName)?.takeIf { it.isOnline } ?: run {
                log("Player not found or not online: $playerName", "WARNING")
                return
            }

        config.getString("voting.message")?.mm()?.let { player.sendMessage(it) }
        executeRewards(playerName, config.getStringList("voting.rewards"))

        val updatedVotes = currentVotes.incrementAndGet()
        Database.updateVoteCount(updatedVotes)

        if (updatedVotes >= totalVotesNeeded) {
            triggerVoteParty()
            currentVotes.set(0)
            Database.updateVoteCount(0)
        }
    }

    private fun triggerVoteParty() {
        val message = config.getString("voteparty.message")?.mm() ?: return
        val rewards = config.getStringList("voteparty.rewards")

        when (config.getString("voteparty.rewardType", "individual")) {
            "individual" ->
                Bukkit.getOnlinePlayers().forEach { player ->
                    player.sendMessage(message)
                    executeRewards(player.name, rewards)
                }
            "server-wide" -> {
                Bukkit.broadcast(message)
                rewards.forEach { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it) }
            }
        }
    }

    private fun executeRewards(
        playerName: String,
        rewards: List<String>,
    ) {
        rewards.forEach { reward ->
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", playerName))
        }
    }
}
