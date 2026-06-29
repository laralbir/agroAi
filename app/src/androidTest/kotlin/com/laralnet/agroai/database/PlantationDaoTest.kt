package com.laralnet.agroai.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.database.AppDatabase
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.infrastructure.persistence.dao.PlantationDao
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PlantationDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.plantationDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(
        id: String = "p1",
        name: String = "Huerta Test",
        type: PlantationType = PlantationType.HUERTA
    ) = PlantationEntity(
        id = id,
        name = name,
        type = type,
        latitude = null,
        longitude = null,
        address = "",
        municipality = "Valencia",
        province = "Valencia",
        country = "ES",
        municipalityCode = "",
        areaSqMeters = 100.0,
        notes = "",
        googleAccountEmail = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Test
    fun insertPlantation_thenFindById_returnsIt() = runBlocking {
        dao.insertPlantation(entity("p1", "Mi Huerta"))

        val result = dao.findById("p1")

        assertNotNull(result)
        assertEquals("Mi Huerta", result!!.plantation.name)
    }

    @Test
    fun findById_unknownId_returnsNull() = runBlocking {
        val result = dao.findById("nonexistent")
        assertNull(result)
    }

    @Test
    fun observeAll_emitsInsertedPlantations() = runBlocking {
        dao.insertPlantation(entity("p1", "Huerta"))
        dao.insertPlantation(entity("p2", "Olivar", PlantationType.OLIVAR))

        val list = dao.observeAll().first()

        assertEquals(2, list.size)
    }

    @Test
    fun deletePlantation_removesIt_fromObserveAll() = runBlocking {
        dao.insertPlantation(entity("p1", "Huerta"))
        dao.insertPlantation(entity("p2", "Viñedo", PlantationType.VIÑEDO))

        dao.delete("p1")

        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("Viñedo", list.first().plantation.name)
    }

    @Test
    fun updatePlantation_changesName() = runBlocking {
        dao.insertPlantation(entity("p1", "Original"))
        val updated = entity("p1", "Actualizado")
        dao.insertPlantation(updated)

        val result = dao.findById("p1")
        assertEquals("Actualizado", result!!.plantation.name)
    }

    @Test
    fun insertPlantation_withCoordinates_persistsThemCorrectly() = runBlocking {
        val e = entity("p1").copy(latitude = 39.47, longitude = -0.37)
        dao.insertPlantation(e)

        val result = dao.findById("p1")!!
        assertEquals(39.47, result.plantation.latitude!!, 0.001)
        assertEquals(-0.37, result.plantation.longitude!!, 0.001)
    }

    @Test
    fun observeAll_emptyDb_returnsEmptyList() = runBlocking {
        assertEquals(0, dao.observeAll().first().size)
        dao.insertPlantation(entity("p1"))
        dao.insertPlantation(entity("p2"))
        assertEquals(2, dao.observeAll().first().size)
    }
}
