package cat.daisy.daisyVotes.managers

import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.utils.Database
import cat.daisy.daisyVotes.utils.TextUtils.log
import cat.daisy.daisyVotes.utils.TextUtils.mm
import com.vexsoftware.votifier.model.VotifierEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class VoteManager : Listener {
    private val plugin: JavaPlugin = DaisyVotes.instance
    private val config = DaisyVotes.mainConfig

    companion object {
        val currentVotes = AtomicInteger(Database.getVoteCount())
        private val voteCache = ConcurrentHashMap<String, Int>()
    }

    private val totalVotesNeeded: Int
        get() = config.getInt("voteparty.totalvotes", 25)

    fun handleVote(playerName: String) {
        val player = Bukkit.getPlayer(playerName)
        if (player?.isOnline == true) {
            val voteMessage = config.getString("voting.message")?.mm() ?: return
            player.sendMessage(voteMessage)
            config.getStringList("voting.rewards").forEach { reward ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", playerName))
            }
        } else {
            log("Player not found or not online: $playerName", "WARNING")
            return
        }

        val updatedVotes = currentVotes.incrementAndGet()
        Database.updateVoteCount(updatedVotes)

        if (updatedVotes >= totalVotesNeeded) {
            triggerVoteParty()
            currentVotes.set(0)
            Database.updateVoteCount(0)
        }
    }

    private fun triggerVoteParty() {
        val votePartyMessage = config.getString("voteparty.message")?.mm() ?: return
        val rewardType = config.getString("voteparty.rewardType", "individual")

        if (rewardType == "individual") {
            Bukkit.getOnlinePlayers().forEach { player ->
                player.sendMessage(votePartyMessage)
                config.getStringList("voteparty.rewards").forEach { reward ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", player.name))
                }
            }
        } else if (rewardType == "server-wide") {
            Bukkit.broadcast(votePartyMessage)
            config.getStringList("voteparty.rewards").forEach { reward ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward)
            }
        }
    }

    @EventHandler
    fun onVote(event: VotifierEvent) {
        val playerName = event.vote.username
        try {
            handleVote(playerName)
        } catch (e: Exception) {
            log("Error handling vote for player $playerName: ${e.message}", "ERROR")
        }
    }
}
