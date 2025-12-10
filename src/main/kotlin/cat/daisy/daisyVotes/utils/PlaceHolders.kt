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

    override fun getVersion(): String = "1.4"

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
        if (!isEnabled || player == null || !DaisyVotes.instance.isEnabled) return ""

        val cacheKey = "${player.uniqueId}:$identifier"

        // Check cache first
        placeholderCache[cacheKey]?.let { cached ->
            if (!cached.isExpired()) return cached.value
            placeholderCache.remove(cacheKey) // Clean up expired entry
        }

        return runCatching {
            val result =
                when (identifier) {
                    "current_votes" -> {
                        VoteManager.currentVotes.get().toString()
                    }

                    "total_votes_needed" -> {
                        config.getInt("voteparty.totalvotes", 25).toString()
                    }

                    "votes_remaining" -> {
                        val remaining = config.getInt("voteparty.totalvotes", 25) - VoteManager.currentVotes.get()
                        remaining.coerceAtLeast(0).toString()
                    }

                    else -> {
                        return ""
                    }
                }
            placeholderCache[cacheKey] = CachedValue(result, globalCacheExpiry)
            result
        }.getOrElse { e ->
            DaisyVotes.instance.logger.warning("Error processing placeholder '$identifier' for ${player.name}: ${e.message}")
            ""
        }
    }

    fun disable() {
        isEnabled = false
        unregister()
        placeholderCache.clear()
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
        private val timestamp: Long = System.currentTimeMillis(),
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > expiryTime
    }
}
