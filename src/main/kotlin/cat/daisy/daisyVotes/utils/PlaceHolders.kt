package cat.daisy.daisyVotes.utils

import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.managers.VoteManager
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PlaceHolders : PlaceholderExpansion() {
    private val config = DaisyVotes.mainConfig
    private var isEnabled = true
    private val placeholderCache = ConcurrentHashMap<String, CachedValue<String>>()
    private val globalCacheExpiry = TimeUnit.MINUTES.toMillis(1)

    override fun getIdentifier(): String = "daisyvotes"

    override fun getAuthor(): String = "Amari"

    override fun getVersion(): String = "1.0"

    override fun persist(): Boolean = true

    override fun canRegister(): Boolean = true

    fun disable() {
        isEnabled = false
        unregister()
        placeholderCache.clear()
    }

    override fun onPlaceholderRequest(
        player: Player?,
        identifier: String,
    ): String = onRequest(player, identifier)

    override fun onRequest(
        player: OfflinePlayer?,
        identifier: String,
    ): String {
        if (!isEnabled || player == null || !DaisyVotes.instance.isEnabled) return ""

        val cacheKey = "${player.uniqueId}:$identifier"
        placeholderCache[cacheKey]?.let {
            if (!it.isExpired()) {
                return it.value
            }
        }

        val totalVotesNeeded = config.getInt("voteparty.totalvotes")

        return try {
            when (identifier) {
                "current_votes" -> {
                    val votes = VoteManager.currentVotes.toString()
                    placeholderCache[cacheKey] = CachedValue(votes, globalCacheExpiry)
                    votes
                }
                "total_votes_needed" -> {
                    placeholderCache[cacheKey] = CachedValue(totalVotesNeeded.toString(), globalCacheExpiry)
                    totalVotesNeeded.toString()
                }
                else -> ""
            }
        } catch (e: Exception) {
            DaisyVotes.instance.logger.warning("Error processing placeholder $identifier for ${player.name}: ${e.message}")
            ""
        }
    }

    fun clearCache(uuid: UUID) {
        placeholderCache.keys.removeIf { it.startsWith("$uuid:") }
    }

    fun clearAllCaches() {
        placeholderCache.clear()
    }

    private data class CachedValue<T>(
        val value: T,
        private val expiryTime: Long,
    ) {
        private val timestamp = System.currentTimeMillis()

        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > expiryTime
    }

    companion object {
        @Volatile
        private var instance: PlaceHolders? = null

        fun getInstance(): PlaceHolders =
            instance ?: synchronized(this) {
                instance ?: PlaceHolders().also { instance = it }
            }

        fun cleanup() {
            instance?.disable()
            instance = null
        }
    }
}
