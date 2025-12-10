package cat.daisy.daisyVotes.utils

import cat.daisy.daisyVotes.DaisyVotes
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

object Database {
    private lateinit var dataSource: HikariDataSource
    private var connected = false

    object VoteCountTable : Table("vote_count") {
        val id = integer("id").default(1) // Always 1 for single row
        val count = integer("count").default(0)
        override val primaryKey = PrimaryKey(id)
    }

    fun connect(pluginFolder: String): Boolean =
        runCatching {
            val dbFile =
                File(pluginFolder, "database.db").apply {
                    if (!exists()) {
                        parentFile.mkdirs()
                        createNewFile()
                    }
                }

            dataSource = HikariDataSource(createHikariConfig(dbFile))
            Database.connect(dataSource)

            transaction {
                SchemaUtils.create(VoteCountTable)
                if (VoteCountTable.selectAll().empty()) {
                    VoteCountTable.insert {
                        it[id] = 1
                        it[count] = 0
                    }
                }
            }

            connected = true
            log("Connected to SQLite database with HikariCP and Exposed", "SUCCESS")
            true
        }.getOrElse { e ->
            log("Failed to connect to SQLite: ${e.message}", "ERROR", throwable = e)
            false
        }

    fun disconnect() {
        if (!::dataSource.isInitialized || !connected) return

        runCatching {
            TransactionManager.currentOrNull()?.let { tm ->
                transaction {
                    tm.commit()
                }
                TransactionManager.closeAndUnregister(tm.db)
            }
            dataSource.close()
            connected = false
            log("Disconnected from database successfully", "INFO")
        }.onFailure { e ->
            log("Error during database shutdown: ${e.message}", "WARNING")
        }
    }

    fun getVoteCount(): Int =
        runCatching {
            transaction {
                VoteCountTable.selectAll().firstOrNull()?.get(VoteCountTable.count) ?: 0
            }
        }.getOrElse { e ->
            log("Failed to get vote count: ${e.message}", "ERROR")
            0
        }

    fun updateVoteCount(newCount: Int) {
        runCatching {
            transaction {
                VoteCountTable.update({ VoteCountTable.id eq 1 }) {
                    it[count] = newCount.coerceAtLeast(0)
                }
            }
        }.onFailure { e ->
            log("Failed to update vote count to $newCount: ${e.message}", "ERROR", throwable = e)
        }
    }

    private fun createHikariConfig(dbFile: File) =
        HikariConfig().apply {
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            maximumPoolSize = 5
            minimumIdle = 2
            connectionTimeout = 10000
            idleTimeout = 300000
            maxLifetime = 600000
            addDataSourceProperty("journal_mode", "WAL")
            addDataSourceProperty("synchronous", "NORMAL")
            addDataSourceProperty("foreign_keys", "ON")
            poolName = "DaisyVotePluginPool"
            connectionTestQuery = "SELECT 1"
        }
}
