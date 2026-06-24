package com.laralnet.agroai.aimodel.infrastructure.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.infrastructure.persistence.AIModelEntity
import com.laralnet.agroai.aimodel.infrastructure.persistence.PromptTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIModelDao {

    @Query("SELECT * FROM ai_models")
    fun observeAll(): Flow<List<AIModelEntity>>

    @Query("SELECT * FROM ai_models WHERE id = :modelId LIMIT 1")
    suspend fun findById(modelId: String): AIModelEntity?

    @Query("SELECT * FROM ai_models WHERE variant = :variant LIMIT 1")
    suspend fun findByVariant(variant: ModelVariant): AIModelEntity?

    @Query("SELECT * FROM ai_models WHERE isActive = 1 LIMIT 1")
    suspend fun findActive(): AIModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: AIModelEntity)

    @Query("UPDATE ai_models SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE ai_models SET isActive = 1 WHERE id = :modelId")
    suspend fun setActive(modelId: String)

    @Query("DELETE FROM ai_models WHERE id = :modelId")
    suspend fun delete(modelId: String)

    @Query("SELECT * FROM prompt_templates WHERE name = :name LIMIT 1")
    suspend fun findPromptTemplate(name: String): PromptTemplateEntity?

    @Query("SELECT * FROM prompt_templates WHERE id = :id LIMIT 1")
    suspend fun findPromptTemplateById(id: String): PromptTemplateEntity?

    @Query("SELECT * FROM prompt_templates")
    fun observePromptTemplates(): Flow<List<PromptTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromptTemplate(template: PromptTemplateEntity)
}
