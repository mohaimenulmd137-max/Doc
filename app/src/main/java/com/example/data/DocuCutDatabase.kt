package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.AspectRatioPreset
import com.example.model.DocumentaryTheme
import com.example.model.DocumentaryTimeline
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val clientName: String = "",
    val themeName: String = DocumentaryTheme.INVESTIGATIVE_DOSSIER.name,
    val aspectRatioName: String = AspectRatioPreset.RATIO_16_9.name,
    val totalDurationMs: Long = 0L,
    val chapterCount: Int = 1,
    val timelineJson: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "brand_kits")
data class BrandKitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clientName: String,
    val channelTag: String,
    val accentColorHex: Long = 0xFF00D1FF,
    val watermarkText: String,
    val defaultCitationPrefix: String = "Source: ",
    val safeZoneEnabled: Boolean = true,
    val aspectRatioName: String = AspectRatioPreset.RATIO_16_9.name,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "export_jobs")
data class ExportJobEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val projectTitle: String,
    val outputPath: String,
    val chaptersTxtPath: String,
    val resolutionLabel: String,
    val isDraftReview: Boolean,
    val status: String, // RUNNING, COMPLETED, FAILED, CANCELLED
    val progressPercent: Int,
    val completedChunks: Int,
    val totalChunks: Int,
    val createdAtMs: Long = System.currentTimeMillis()
)

class TimelineJsonCodec {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(DocumentaryTimeline::class.java)

    fun toJson(timeline: DocumentaryTimeline): String = adapter.toJson(timeline)

    fun fromJson(json: String): DocumentaryTimeline =
        adapter.fromJson(json) ?: DocumentaryTimeline()
}

class RoomTypeConverters {
    private val codec = TimelineJsonCodec()

    @TypeConverter
    fun fromTimeline(timeline: DocumentaryTimeline): String = codec.toJson(timeline)

    @TypeConverter
    fun toTimeline(json: String): DocumentaryTimeline = codec.fromJson(json)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMs DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeProjectById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface BrandKitDao {
    @Query("SELECT * FROM brand_kits ORDER BY updatedAtMs DESC")
    fun observeAllBrandKits(): Flow<List<BrandKitEntity>>

    @Query("SELECT * FROM brand_kits WHERE id = :id LIMIT 1")
    suspend fun getBrandKitById(id: String): BrandKitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBrandKit(kit: BrandKitEntity)

    @Query("DELETE FROM brand_kits WHERE id = :id")
    suspend fun deleteBrandKitById(id: String)
}

@Dao
interface ExportJobDao {
    @Query("SELECT * FROM export_jobs ORDER BY createdAtMs DESC")
    fun observeExportJobs(): Flow<List<ExportJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: ExportJobEntity)

    @Query("DELETE FROM export_jobs WHERE id = :id")
    suspend fun deleteJob(id: String)
}

@Database(
    entities = [ProjectEntity::class, BrandKitEntity::class, ExportJobEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class DocuCutDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun brandKitDao(): BrandKitDao
    abstract fun exportJobDao(): ExportJobDao

    companion object {
        @Volatile
        private var INSTANCE: DocuCutDatabase? = null

        fun getInstance(context: Context): DocuCutDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DocuCutDatabase::class.java,
                    "docucut_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class DocuCutRepository(private val db: DocuCutDatabase) {
    private val codec = TimelineJsonCodec()

    val allProjects: Flow<List<ProjectEntity>> = db.projectDao().observeAllProjects()
    val allBrandKits: Flow<List<BrandKitEntity>> = db.brandKitDao().observeAllBrandKits()
    val exportJobs: Flow<List<ExportJobEntity>> = db.exportJobDao().observeExportJobs()

    fun observeProject(id: String): Flow<ProjectEntity?> = db.projectDao().observeProjectById(id)

    suspend fun getProject(id: String): ProjectEntity? = db.projectDao().getProjectById(id)

    suspend fun saveTimeline(
        projectId: String,
        title: String,
        clientName: String,
        timeline: DocumentaryTimeline
    ) {
        val entity = ProjectEntity(
            id = projectId,
            title = title,
            clientName = clientName,
            themeName = timeline.theme.name,
            aspectRatioName = timeline.aspectRatio.name,
            totalDurationMs = timeline.totalDurationMs,
            chapterCount = timeline.chapters.size.coerceAtLeast(1),
            timelineJson = codec.toJson(timeline),
            updatedAtMs = System.currentTimeMillis()
        )
        db.projectDao().upsertProject(entity)
    }

    fun decodeTimeline(json: String): DocumentaryTimeline = codec.fromJson(json)

    fun encodeTimeline(timeline: DocumentaryTimeline): String = codec.toJson(timeline)

    suspend fun deleteProject(id: String) = db.projectDao().deleteProjectById(id)

    suspend fun getBrandKit(id: String): BrandKitEntity? = db.brandKitDao().getBrandKitById(id)

    suspend fun saveBrandKit(kit: BrandKitEntity) = db.brandKitDao().upsertBrandKit(kit)

    suspend fun deleteBrandKit(id: String) = db.brandKitDao().deleteBrandKitById(id)

    suspend fun saveExportJob(job: ExportJobEntity) = db.exportJobDao().upsertJob(job)
}
