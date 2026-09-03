package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getDownloadFlow(id: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus)

    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, speed = :speed, etaSeconds = :etaSeconds WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long)

    @Query("UPDATE downloads SET completedSegments = :completed, progress = :progress, speed = :speed, etaSeconds = :etaSeconds, downloadedBytes = :downloadedBytes WHERE id = :id")
    suspend fun updateHlsProgress(id: String, completed: Int, progress: Float, speed: Long, etaSeconds: Long, downloadedBytes: Long)

    @Query("UPDATE downloads SET status = :status, completedAt = :completedAt, filePath = :filePath, progress = 1.0 WHERE id = :id")
    suspend fun markCompleted(id: String, status: DownloadStatus = DownloadStatus.COMPLETED, completedAt: Long = System.currentTimeMillis(), filePath: String)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: String, status: DownloadStatus = DownloadStatus.FAILED, error: String)

    @Query("UPDATE downloads SET status = 'PAUSED' WHERE status = 'DOWNLOADING' OR status = 'ANALYZING' OR status = 'QUEUED'")
    suspend fun resetInterruptedDownloads()

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()

    // Search history queries
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = try {
        DownloadStatus.valueOf(value)
    } catch (e: Exception) {
        DownloadStatus.FAILED
    }
}

@Database(
    entities = [DownloadEntity::class, SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
