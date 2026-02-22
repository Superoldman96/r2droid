package top.wsdx233.r2droid.core.data.db

import androidx.paging.PagingSource
import androidx.room.*

@Entity(tableName = "functions")
data class FunctionEntity(
    @PrimaryKey val addr: Long,
    val name: String,
    val size: Long,
    val nbbs: Int,
    val signature: String
)

@Dao
interface FunctionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FunctionEntity>)

    @Query("SELECT * FROM functions")
    fun getPagingSource(): PagingSource<Int, FunctionEntity>

    @Query("SELECT * FROM functions WHERE name LIKE '%' || :query || '%'")
    fun search(query: String): PagingSource<Int, FunctionEntity>

    @Query("DELETE FROM functions")
    suspend fun clearAll()
}
