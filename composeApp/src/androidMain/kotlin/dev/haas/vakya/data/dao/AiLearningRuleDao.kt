package dev.haas.vakya.data.dao

import androidx.room.*
import dev.haas.vakya.data.database.AiLearningRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiLearningRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AiLearningRuleEntity): Long

    @Update
    suspend fun updateRule(rule: AiLearningRuleEntity)

    @Delete
    suspend fun deleteRule(rule: AiLearningRuleEntity)

    @Query("SELECT * FROM ai_learning_rules")
    fun getAllRules(): Flow<List<AiLearningRuleEntity>>

    @Query("SELECT * FROM ai_learning_rules")
    suspend fun getAllRulesList(): List<AiLearningRuleEntity>
}
