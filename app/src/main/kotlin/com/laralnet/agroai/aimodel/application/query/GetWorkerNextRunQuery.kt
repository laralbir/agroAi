package com.laralnet.agroai.aimodel.application.query

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.laralnet.agroai.aimodel.infrastructure.worker.PlantationHealthWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetWorkerNextRunQuery @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Flow<Long?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(PlantationHealthWorker.WORK_NAME)
            .asFlow()
            .map { infos ->
                val info = infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED } ?: return@map null
                runCatching { info.nextScheduleTimeMillis }
                    .getOrNull()
                    ?.takeIf { it != Long.MAX_VALUE && it > System.currentTimeMillis() }
            }
}
