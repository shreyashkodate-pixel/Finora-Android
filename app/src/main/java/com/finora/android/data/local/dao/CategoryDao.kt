package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY displayOrder ASC, name ASC")
    fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY displayOrder ASC, name ASC")
    suspend fun getCategories(profileId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getCategoryById(id: String, profileId: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId AND profileId = :profileId")
    suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
