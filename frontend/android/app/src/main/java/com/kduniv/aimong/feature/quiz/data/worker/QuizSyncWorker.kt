package com.kduniv.aimong.feature.quiz.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kduniv.aimong.core.local.dao.OfflineMissionQueueDao
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.feature.quiz.data.model.QuizAnswer
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class QuizSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: AimongApiService,
    private val offlineDao: OfflineMissionQueueDao,
    private val gson: Gson
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val unsyncedMissions = offlineDao.getUnsyncedMissions()
        if (unsyncedMissions.isEmpty()) return@withContext Result.success()

        var hasError = false
        unsyncedMissions.forEach { entity ->
            try {
                val answersType = object : TypeToken<List<QuizAnswer>>() {}.type
                val answers: List<QuizAnswer> = gson.fromJson(entity.answersJson, answersType)
                val request = QuizSubmitRequest(entity.quizAttemptId, answers)
                
                val response = apiService.submitQuiz(entity.missionId, request)
                if (response.success) {
                    offlineDao.markAsSynced(entity.id)
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                hasError = true
            }
        }

        if (hasError) Result.retry() else Result.success()
    }
}
