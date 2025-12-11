package cat.daisy.daisyVotes.utils

import cat.daisy.daisyVotes.utils.TextUtils.log
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SQLite database manager using HikariCP connection pooling and Exposed ORM.
 * Provides thread-safe vote count persistence.
 */
object Database {
    private var dataSource: HikariDataSource? = null
    private val connected = AtomicBoolean(false)

    /** Vote count table - stores a single row with the current vote count */
    object VoteCountTable : Table("vote_count") {
        val id = integer("id").default(1)
        val count = integer("count").default(0)
        override val primaryKey = PrimaryKey(id)
    }

    /**
     * Connects to the SQLite database.
     * @param pluginFolder The plugin's data folder path
     * @return true if connection successful, false otherwise
     */
    fun connect(pluginFolder: String): Boolean {
        if (connected.get()) {
            log("Database already connected", "WARNING")
            return true
        }

        return runCatching {
            val dbFile =
                File(pluginFolder, "database.db").apply {
                    parentFile?.mkdirs()
                    if (!exists()) createNewFile()
                }

            dataSource = HikariDataSource(createHikariConfig(dbFile))
            Database.connect(dataSource!!)

            transaction {
                SchemaUtils.create(VoteCountTable)
                initializeVoteCount()
            }

            connected.set(true)
            log("Database connected (SQLite + HikariCP + Exposed)", "SUCCESS")
            true
        }.getOrElse { e ->
            log("Database connection failed: ${e.message}", "ERROR", throwable = e)
            false
        }
    }

    /**
     * Disconnects from the database and releases all resources.
     */
    fun disconnect() {
        if (!connected.getAndSet(false)) return

        runCatching {
            TransactionManager.currentOrNull()?.db?.let { db ->
                TransactionManager.closeAndUnregister(db)
            }
            dataSource?.close()
            dataSource = null
            log("Database disconnected", "INFO")
        }.onFailure { e ->
            log("Error during database shutdown: ${e.message}", "WARNING")
        }
    }

    /**
     * Gets the current vote count from the database.
     * @return The current vote count, or 0 if an error occurs
     */
    fun getVoteCount(): Int {
        if (!connected.get()) return 0

        return runCatching {
            transaction {
                VoteCountTable
                    .selectAll()
                    .where { VoteCountTable.id eq 1 }
                    .firstOrNull()
                    ?.get(VoteCountTable.count) ?: 0
            }
        }.getOrElse { e ->
            log("Failed to get vote count: ${e.message}", "ERROR")
            0
        }
    }

    /**
     * Updates the vote count in the database.
     * @param newCount The new vote count (will be clamped to >= 0)
     */
    fun updateVoteCount(newCount: Int) {
        if (!connected.get()) return

        runCatching {
            transaction {
                VoteCountTable.update({ VoteCountTable.id eq 1 }) {
                    it[count] = newCount.coerceAtLeast(0)
                }
            }
        }.onFailure { e ->
            log("Failed to update vote count: ${e.message}", "ERROR", throwable = e)
        }
    }

    /**
     * Checks if the database is connected.
     */
    @Suppress("unused") // Public API
    fun isConnected(): Boolean = connected.get()

    private fun initializeVoteCount() {
        val exists =
            VoteCountTable
                .selectAll()
                .where { VoteCountTable.id eq 1 }
                .count() > 0

        if (!exists) {
            VoteCountTable.insert {
                it[id] = 1
                it[count] = 0
            }
        }
    }

    private fun createHikariConfig(dbFile: File) =
        HikariConfig().apply {
            poolName = "DaisyVotes-Pool"
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"

            // SQLite-optimized pool settings
            maximumPoolSize = 3
            minimumIdle = 1
            connectionTimeout = 10_000
            idleTimeout = 300_000
            maxLifetime = 600_000

            // SQLite performance optimizations
            addDataSourceProperty("journal_mode", "WAL")
            addDataSourceProperty("synchronous", "NORMAL")
            addDataSourceProperty("cache_size", "2000")
            addDataSourceProperty("foreign_keys", "ON")

            connectionTestQuery = "SELECT 1"
        }
}
