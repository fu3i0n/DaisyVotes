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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VoteManager : Listener {
    private val config get() = DaisyVotes.mainConfig
    private val voteLock = ReentrantLock()

    companion object {
        val currentVotes = AtomicInteger(Database.getVoteCount())
    }

    private val totalVotesNeeded: Int
        get() = config.getInt("voteparty.totalvotes", 25).coerceAtLeast(1)

    @EventHandler
    fun onVote(event: VotifierEvent) {
        runCatching {
            handleVote(event.vote.username)
        }.onFailure { e ->
            log("Error handling vote for player ${event.vote.username}: ${e.message}", "ERROR")
        }
    }

    private fun handleVote(playerName: String) {
        val player = Bukkit.getPlayer(playerName)?.takeIf { it.isOnline }

        if (player == null) {
            log("Player not found or not online: $playerName", "WARNING")
            return
        }

        // Send vote message and rewards
        config.getString("voting.message")?.takeIf { it.isNotBlank() }?.mm()?.let {
            player.sendMessage(it)
        }
        executeRewards(playerName, config.getStringList("voting.rewards"))

        // Thread-safe vote counting with vote party check
        voteLock.withLock {
            val updatedVotes = currentVotes.incrementAndGet()
            Database.updateVoteCount(updatedVotes)

            if (updatedVotes >= totalVotesNeeded) {
                triggerVoteParty()
                currentVotes.set(0)
                Database.updateVoteCount(0)
            }
        }
    }

    private fun triggerVoteParty() {
        val message =
            config
                .getString("voteparty.message")
                ?.takeIf { it.isNotBlank() }
                ?.mm()
                ?: return

        val rewards = config.getStringList("voteparty.rewards")
        if (rewards.isEmpty()) {
            log("Vote party triggered but no rewards configured", "WARNING")
        }

        when (config.getString("voteparty.rewardType", "individual")?.lowercase()) {
            "individual" -> {
                val onlinePlayers = Bukkit.getOnlinePlayers()
                if (onlinePlayers.isEmpty()) return

                onlinePlayers.forEach { player ->
                    player.sendMessage(message)
                    executeRewards(player.name, rewards)
                }
                log("Vote party triggered for ${onlinePlayers.size} players", "SUCCESS")
            }

            "server-wide" -> {
                Bukkit.broadcast(message)
                rewards.forEach { reward ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward)
                }
                log("Server-wide vote party triggered", "SUCCESS")
            }

            else -> {
                log("Unknown reward type: ${config.getString("voteparty.rewardType")}", "WARNING")
            }
        }
    }

    private fun executeRewards(
        playerName: String,
        rewards: List<String>,
    ) {
        if (rewards.isEmpty()) return

        rewards.forEach { reward ->
            val command = reward.replace("%player%", playerName)
            runCatching {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }.onFailure { e ->
                log("Failed to execute reward command '$command': ${e.message}", "ERROR")
            }
        }
    }
}
