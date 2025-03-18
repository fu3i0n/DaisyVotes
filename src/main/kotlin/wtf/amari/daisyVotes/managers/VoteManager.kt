package wtf.amari.daisyVotes.managers

import com.vexsoftware.votifier.model.VotifierEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import wtf.amari.daisyVotes.DaisyVotes
import wtf.amari.daisyVotes.utils.Database
import wtf.amari.daisyVotes.utils.TextUtils.log
import wtf.amari.daisyVotes.utils.TextUtils.mm
import java.util.concurrent.ConcurrentHashMap

class VoteManager : Listener {
    private val plugin: JavaPlugin = DaisyVotes.instance
    private val config = DaisyVotes.mainConfig

    companion object {
        @Volatile
        internal var currentVotes: Int = Database.getVoteCount()
            private set

        private val voteCache = ConcurrentHashMap<String, Int>()
    }

    private val totalVotesNeeded: Int
        get() = config.getInt("voteparty.totalvotes", 25)

    fun handleVote(playerName: String) {
        val player = Bukkit.getPlayer(playerName)
        player?.let {
            if (it.isOnline) {
                val voteMessage = config.getString("voting.message")?.mm() ?: return
                it.sendMessage(voteMessage)
                config.getStringList("voting.rewards").forEach { reward ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", playerName))
                }
            }
        } ?: run {
            log("Player not found or not online: $playerName", "WARNING")
            return
        }

        synchronized(this) {
            currentVotes++
            Database.updateVoteCount(currentVotes)
            if (currentVotes >= totalVotesNeeded) {
                triggerVoteParty()
                currentVotes = 0
                Database.updateVoteCount(currentVotes)
            }
        }
    }

    private fun triggerVoteParty() {
        val votePartyMessage = config.getString("votingparty.message")?.mm() ?: return
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendMessage(votePartyMessage)
            config.getStringList("voteparty.rewards").forEach { reward ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", player.name))
            }
        }
    }

    @EventHandler
    fun onVote(event: VotifierEvent) {
        val vote = event.vote
        val playerName = vote.username
        try {
            handleVote(playerName)
        } catch (e: IllegalArgumentException) {
            log("Error handling vote for player $playerName: ${e.message}", "ERROR")
        }
    }
}
