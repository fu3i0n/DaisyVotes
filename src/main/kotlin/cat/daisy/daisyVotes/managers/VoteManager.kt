package cat.daisy.daisyVotes.managers

import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.utils.Database
import cat.daisy.daisyVotes.utils.TextUtils.log
import cat.daisy.daisyVotes.utils.TextUtils.mm
import com.vexsoftware.votifier.model.VotifierEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Handles all vote-related events and vote party logic.
 * Thread-safe implementation for concurrent vote processing.
 */
class VoteManager : Listener {
    private val config get() = DaisyVotes.mainConfig
    private val voteLock = ReentrantLock()

    companion object {
        /** Current vote count - thread-safe atomic integer */
        val currentVotes = AtomicInteger(Database.getVoteCount())

        /** Resets the vote counter (for admin commands) */
        fun resetVotes() {
            currentVotes.set(0)
            Database.updateVoteCount(0)
        }
    }

    private val totalVotesNeeded: Int
        get() = config.getInt("voteparty.totalvotes", 25).coerceAtLeast(1)

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onVote(event: VotifierEvent) {
        val playerName = event.vote.username

        runCatching {
            processVote(playerName)
        }.onFailure { e ->
            log("Error processing vote for $playerName: ${e.message}", "ERROR", throwable = e)
        }
    }

    private fun processVote(playerName: String) {
        // Send rewards even if player is offline (commands will handle it)
        sendVoteMessage(playerName)
        executeRewards(playerName, config.getStringList("voting.rewards"))

        // Thread-safe vote counting
        voteLock.withLock {
            val newCount = currentVotes.incrementAndGet()
            Database.updateVoteCount(newCount)

            log("Vote received from $playerName ($newCount/$totalVotesNeeded)", "INFO")

            if (newCount >= totalVotesNeeded) {
                triggerVoteParty()
                currentVotes.set(0)
                Database.updateVoteCount(0)
            }
        }
    }

    private fun sendVoteMessage(playerName: String) {
        val player = Bukkit.getPlayer(playerName) ?: return

        config
            .getString("voting.message")
            ?.takeIf { it.isNotBlank() }
            ?.mm()
            ?.let { player.sendMessage(it) }
    }

    private fun triggerVoteParty() {
        val message =
            config
                .getString("voteparty.message")
                ?.takeIf { it.isNotBlank() }
                ?.mm()

        val rewards = config.getStringList("voteparty.rewards")

        if (rewards.isEmpty()) {
            log("Vote party triggered but no rewards configured!", "WARNING")
        }

        when (config.getString("voteparty.rewardType", "individual")?.lowercase()) {
            "individual" -> distributeIndividualRewards(message, rewards)
            "server-wide" -> distributeServerWideRewards(message, rewards)
            else -> log("Unknown voteparty.rewardType in config", "WARNING")
        }
    }

    private fun distributeIndividualRewards(
        message: net.kyori.adventure.text.Component?,
        rewards: List<String>,
    ) {
        val onlinePlayers = Bukkit.getOnlinePlayers()

        if (onlinePlayers.isEmpty()) {
            log("Vote party triggered but no players online!", "WARNING")
            return
        }

        onlinePlayers.forEach { player ->
            message?.let { player.sendMessage(it) }
            executeRewards(player.name, rewards)
        }

        log("Vote party! Distributed rewards to ${onlinePlayers.size} players", "SUCCESS")
    }

    private fun distributeServerWideRewards(
        message: net.kyori.adventure.text.Component?,
        rewards: List<String>,
    ) {
        message?.let { Bukkit.broadcast(it) }

        rewards.forEach { reward ->
            runCatching {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward)
            }.onFailure { e ->
                log("Failed to execute server-wide reward '$reward': ${e.message}", "ERROR")
            }
        }

        log("Server-wide vote party triggered!", "SUCCESS")
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
                log("Failed to execute reward '$command': ${e.message}", "ERROR")
            }
        }
    }
}
