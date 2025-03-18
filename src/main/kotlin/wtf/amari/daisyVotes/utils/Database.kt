package wtf.amari.daisyVotes.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import wtf.amari.daisyVotes.DaisyVotes
import wtf.amari.daisyVotes.utils.TextUtils.log
import java.io.File

object Database {
    private lateinit var dataSource: HikariDataSource
    private var connected = false
    private val databaseDispatcher = Dispatchers.IO.limitedParallelism(5)

    fun connect(pluginFolder: String): Boolean =
        try {
            val dbFile =
                File(pluginFolder, "database.db").apply {
                    if (!exists()) parentFile.mkdirs()
                }

            val config =
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

            dataSource = HikariDataSource(config)
            org.jetbrains.exposed.sql.Database
                .connect(dataSource)

            transaction {
                SchemaUtils.createMissingTablesAndColumns(VoteCountTable)
            }

            initializeVoteCount()
            connected = true
            log("Connected to SQLite database with HikariCP and Exposed", "SUCCESS")
            true
        } catch (e: Exception) {
            log("Failed to connect to SQLite: ${e.message}", "ERROR", throwable = e)
            false
        }

    fun disconnect() {
        if (::dataSource.isInitialized) {
            try {
                transaction {
                    TransactionManager.current().commit()
                    TransactionManager.closeAndUnregister(TransactionManager.current().db)
                }
            } catch (e: Exception) {
                DaisyVotes.instance.logger.warning("Error committing transaction before shutdown: ${e.message}")
            } finally {
                connected = false
                dataSource.close()
            }
        }
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(databaseDispatcher) {
            transaction { block() }
        }

    object VoteCountTable : Table("vote_count") {
        val id = integer("id").autoIncrement()
        val count = integer("count").default(0)
        override val primaryKey = PrimaryKey(id)
    }

    private fun initializeVoteCount() {
        transaction {
            if (VoteCountTable.selectAll().count() == 0L) {
                VoteCountTable.insert {
                    it[count] = 0
                }
            }
        }
    }

    fun getVoteCount(): Int =
        transaction {
            VoteCountTable.selectAll().firstOrNull()?.get(VoteCountTable.count) ?: 0
        }

    fun updateVoteCount(newCount: Int) {
        transaction {
            VoteCountTable.update({ VoteCountTable.id eq 1 }) {
                it[count] = newCount
            }
        }
    }
}
