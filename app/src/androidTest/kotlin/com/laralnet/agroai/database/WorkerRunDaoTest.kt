package com.laralnet.agroai.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.aimodel.infrastructure.persistence.dao.WorkerRunDao
import com.laralnet.agroai.aimodel.infrastructure.persistence.entity.WorkerRunEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WorkerRunDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WorkerRunDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.workerRunDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(
        id: String = "run-1",
        plantationId: String? = "p1",
        actionsCreated: Int = 3,
        timestamp: Long = Instant.now().toEpochMilli()
    ) = WorkerRunEntity(
        id = id,
        timestamp = timestamp,
        plantationId = plantationId,
        plantationName = plantationId?.let { "Plantation $it" },
        actionsCreated = actionsCreated,
        summary = "## Test summary\n$actionsCreated actions",
        durationMs = 1000L
    )

    @Test
    fun `insert and findById returns run`() = runBlocking {
        dao.insert(entity("run-1"))

        val result = dao.findById("run-1")

        assertNotNull(result)
        assertEquals("run-1", result!!.id)
        assertEquals(3, result.actionsCreated)
    }

    @Test
    fun `findById unknown id returns null`() = runBlocking {
        val result = dao.findById("nonexistent")
        assertNull(result)
    }

    @Test
    fun `observeAll returns all inserted runs`() = runBlocking {
        dao.insert(entity("run-1"))
        dao.insert(entity("run-2", plantationId = "p2", actionsCreated = 0))

        val list = dao.observeAll().first()

        assertEquals(2, list.size)
    }

    @Test
    fun `observeAll returns runs ordered by timestamp desc`() = runBlocking {
        val now = Instant.now().toEpochMilli()
        dao.insert(entity("old", timestamp = now - 10_000))
        dao.insert(entity("recent", timestamp = now))

        val list = dao.observeAll().first()

        assertEquals("recent", list[0].id)
        assertEquals("old", list[1].id)
    }

    @Test
    fun `observeByPlantation returns only matching runs`() = runBlocking {
        dao.insert(entity("run-p1", plantationId = "p1"))
        dao.insert(entity("run-p2", plantationId = "p2"))

        val list = dao.observeByPlantation("p1").first()

        assertEquals(1, list.size)
        assertEquals("p1", list[0].plantationId)
    }

    @Test
    fun `deleteOlderThan removes stale runs`() = runBlocking {
        val cutoff = Instant.now()
        val oldTimestamp = cutoff.minusSeconds(3600).toEpochMilli()
        val recentTimestamp = cutoff.plusSeconds(60).toEpochMilli()

        dao.insert(entity("old", timestamp = oldTimestamp))
        dao.insert(entity("recent", timestamp = recentTimestamp))

        dao.deleteOlderThan(cutoff.toEpochMilli())

        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("recent", list[0].id)
    }

    @Test
    fun `insert replaces on conflict`() = runBlocking {
        dao.insert(entity("run-1", actionsCreated = 1))
        dao.insert(entity("run-1", actionsCreated = 5))

        val result = dao.findById("run-1")!!
        assertEquals(5, result.actionsCreated)
    }

    @Test
    fun `run with null plantationId is stored correctly`() = runBlocking {
        dao.insert(entity("run-null", plantationId = null))

        val result = dao.findById("run-null")
        assertNotNull(result)
        assertNull(result!!.plantationId)
    }
}
