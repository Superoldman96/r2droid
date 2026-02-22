package top.wsdx233.r2droid.core.data.db

import androidx.paging.PagingSource
import androidx.room.*

@Entity(tableName = "imports")
data class ImportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ordinal: Int,
    val type: String,
    val plt: Long
)

@Dao
interface ImportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ImportEntity>)

    @Query("SELECT * FROM imports")
    fun getPagingSource(): PagingSource<Int, ImportEntity>

    @Query("SELECT * FROM imports WHERE name LIKE '%' || :query || '%'")
    fun search(query: String): PagingSource<Int, ImportEntity>

    @Query("DELETE FROM imports")
    suspend fun clearAll()
}
