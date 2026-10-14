package app.pulse.core.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class OutboxWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}
