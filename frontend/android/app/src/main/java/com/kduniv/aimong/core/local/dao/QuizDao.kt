package com.kduniv.aimong.core.local.dao

import androidx.room.*
import com.kduniv.aimong.core.local.entity.QuizMetadataEntity
import com.kduniv.aimong.core.local.entity.QuizQuestionEntity

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_metadata WHERE missionId = :missionId")
    suspend fun getQuizMetadata(missionId: String): QuizMetadataEntity?

    @Query("SELECT * FROM quiz_questions WHERE missionId = :missionId")
    suspend fun getQuizQuestions(missionId: String): List<QuizQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizMetadata(metadata: QuizMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestionEntity>)

    @Transaction
    suspend fun saveQuiz(metadata: QuizMetadataEntity, questions: List<QuizQuestionEntity>) {
        insertQuizMetadata(metadata)
        insertQuizQuestions(questions)
    }

    @Query("DELETE FROM quiz_questions WHERE missionId = :missionId")
    suspend fun deleteQuestionsByMission(missionId: String)
}
