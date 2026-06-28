package com.laralnet.agroai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.laralnet.agroai.aimodel.infrastructure.worker.PlantationHealthWorker
import com.laralnet.agroai.weather.infrastructure.worker.WeatherRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration as OsmConfiguration
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class AgroAIApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid()
        WeatherRefreshWorker.schedule(this)
        PlantationHealthWorker.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun configureOsmdroid() {
        OsmConfiguration.getInstance().apply {
            // Required by OSM tile usage policy
            userAgentValue = "AgroAI/0.1.0 (laralbir@gmail.com)"
            // Use app internal cache — no external storage permission needed
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}
