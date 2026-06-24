package com.laralnet.agroai.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.database.AppDatabase
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationEntity
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.treatment.infrastructure.persistence.dao.TreatmentDao
import com.laralnet.agroai.treatment.infrastructure.persistence.entity.TreatmentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TreatmentDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var treatmentDao: TreatmentDao

    private val now = Instant.now()
    private val plantationId = "plantation-1"

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        treatmentDao = db.treatmentDao()

        val plantation = PlantationEntity(
            id = plantationId, name = "Test Plantation",
            type = PlantationType.HUERTA,
            latitude = null, longitude = null,
            address = "", municipality = "Test", province = "Test",
            country = "ES", municipalityCode = "",
            areaSqMeters = 100.0, notes = "",
            googleAccountEmail = null,
            createdAt = now.toEpochMilli(), updatedAt = now.toEpochMilli()
        )
        runBlocking { db.plantationDao().insertPlantation(plantation) }
    }

    @After
    fun tearDown() = db.close()

    private fun treatmentEntity(
        id: String = "t1",
        status: TreatmentStatus = TreatmentStatus.PENDING,
        scheduledDate: Long = now.plusSeconds(3600).toEpochMilli()
    ) = TreatmentEntity(
        id = id,
        plantationId = plantationId,
        type = TreatmentType.IRRIGATION,
        status = status,
        title = "Riego programado",
        description = "Riego por goteo",
        scheduledDate = scheduledDate,
        completedDate = null,
        calendarEventId = null,
        aiAnalysisId = null,
        createdAt = now.toEpochMilli()
    )

    @Test
    fun insertAndObserveByPlantation_returnsAll() = runBlocking {
        treatmentDao.insertTreatment(treatmentEntity("t1"))
        treatmentDao.insertTreatment(treatmentEntity("t2"))

        val list = treatmentDao.observeByPlantation(plantationId).first()
        assertEquals(2, list.size)
    }

    @Test
    fun observeUpcoming_returnsOnlyPendingFutureTreatments() = runBlocking {
        val future = now.plusSeconds(3600).toEpochMilli()
        val past = now.minusSeconds(3600).toEpochMilli()

        treatmentDao.insertTreatment(treatmentEntity("t1", TreatmentStatus.PENDING, future))
        treatmentDao.insertTreatment(treatmentEntity("t2", TreatmentStatus.COMPLETED, future))
        treatmentDao.insertTreatment(treatmentEntity("t3", TreatmentStatus.PENDING, past))

        val upcoming = treatmentDao.observeUpcoming(now.toEpochMilli()).first()
        assertEquals(1, upcoming.size)
        assertEquals("t1", upcoming.first().id)
    }

    @Test
    fun deleteTreatment_removesFromDb() = runBlocking {
        treatmentDao.insertTreatment(treatmentEntity("t1"))
        treatmentDao.deleteTreatment("t1")

        val list = treatmentDao.observeByPlantation(plantationId).first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun updateStatus_changesStatusCorrectly() = runBlocking {
        treatmentDao.insertTreatment(treatmentEntity("t1"))
        treatmentDao.updateStatus("t1", TreatmentStatus.COMPLETED, now.toEpochMilli())

        val list = treatmentDao.observeByPlantation(plantationId).first()
        assertEquals(TreatmentStatus.COMPLETED, list.first().status)
    }
}
