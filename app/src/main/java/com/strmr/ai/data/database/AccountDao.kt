package com.strmr.ai.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE accountType = :accountType")
    suspend fun getAccount(accountType: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE accountType = :accountType")
    fun getAccountFlow(accountType: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE accountType = :accountType")
    suspend fun deleteAccount(accountType: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("UPDATE accounts SET lastSyncTimestamp = :timestamp WHERE accountType = :accountType")
    suspend fun updateLastSync(accountType: String, timestamp: Long)
} 