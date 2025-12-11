package cat.daisy.daisyVotes.utils

import cat.daisy.daisyVotes.DaisyVotes
import cat.daisy.daisyVotes.managers.VoteManager
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * PlaceholderAPI expansion for DaisyVotes.
 *
 * Available placeholders:
 * - %daisyvotes_current_votes% - Current vote count (real-time)
 * - %daisyvotes_total_votes_needed% - Votes needed for party (cached 10s)
 * - %daisyvotes_votes_remaining% - Remaining votes needed (real-time)
 */
class PlaceHolders : PlaceholderExpansion() {
    private var isEnabled = true

    // Only cache config values (they don't change often)
    private val configCache = ConcurrentHashMap<String, CachedValue<Int>>()
    private val configCacheExpiry = TimeUnit.SECONDS.toMillis(10)

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

    override fun getIdentifier(): String = "daisyvotes"

    override fun getAuthor(): String = "Daisy"

    override fun getVersion(): String = "1.5"

    override fun persist(): Boolean = true

    override fun canRegister(): Boolean = true

    override fun onPlaceholderRequest(
        player: Player?,
        identifier: String,
    ): String = onRequest(player, identifier)

    override fun onRequest(
        player: OfflinePlayer?,
        identifier: String,
    ): String {
        if (!isEnabled || !DaisyVotes.instance.isEnabled) return ""

        return runCatching {
            when (identifier) {
                // Real-time values - no caching (atomic read is already O(1))
                "current_votes" -> {
                    VoteManager.currentVotes.get().toString()
                }

                "votes_remaining" -> {
                    val needed = getTotalVotesNeeded()
                    val current = VoteManager.currentVotes.get()
                    (needed - current).coerceAtLeast(0).toString()
                }

                // Config value - cached for 10 seconds
                "total_votes_needed" -> {
                    getTotalVotesNeeded().toString()
                }

                else -> {
                    ""
                }
            }
        }.getOrElse { e ->
            DaisyVotes.instance.logger.warning("Error processing placeholder '$identifier': ${e.message}")
            ""
        }
    }

    /** Get total votes needed from config with caching */
    private fun getTotalVotesNeeded(): Int {
        val cacheKey = "total_votes_needed"

        configCache[cacheKey]?.let { cached ->
            if (!cached.isExpired()) return cached.value
        }

        val value = DaisyVotes.mainConfig.getInt("voteparty.totalvotes", 25)
        configCache[cacheKey] = CachedValue(value, configCacheExpiry)
        return value
    }

    fun disable() {
        isEnabled = false
        unregister()
        configCache.clear()
    }

    @Suppress("unused") // Public API
    fun clearCache(uuid: UUID) {
        // No per-player cache anymore, but keep method for API compatibility
    }

    @Suppress("unused") // Public API
    fun clearAllCaches() {
        configCache.clear()
    }

    private data class CachedValue<T>(
        val value: T,
        private val expiryTime: Long,
        private val timestamp: Long = System.currentTimeMillis(),
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > expiryTime
    }
}
