package dev.haas.vakya.data.database

import androidx.room.*

@Entity(tableName = "ai_learning_rules")
data class AiLearningRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val senderDomain: String?,
    val action: String, // ignore / lower priority
    val confidenceAdjustment: Float
)
