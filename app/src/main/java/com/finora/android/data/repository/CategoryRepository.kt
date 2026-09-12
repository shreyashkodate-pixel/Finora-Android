package com.finora.android.data.repository

import com.finora.android.data.local.dao.CategoryDao
import com.finora.android.data.local.dao.ExpenseDao
import com.finora.android.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface CategoryRepository {
    fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>>
    suspend fun getCategories(profileId: String): List<CategoryEntity>
    suspend fun getCategoryById(id: String, profileId: String): CategoryEntity?
    suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int
    suspend fun createCategory(
        profileId: String,
        name: String,
        iconName: String,
        colorHex: String
    ): CategoryEntity
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity, reassignToCategoryId: String? = null)
}

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao
) : CategoryRepository {

    override fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesFlow(profileId)

    override suspend fun getCategories(profileId: String): List<CategoryEntity> =
        categoryDao.getCategories(profileId)

    override suspend fun getCategoryById(id: String, profileId: String): CategoryEntity? =
        categoryDao.getCategoryById(id, profileId)

    override suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int =
        categoryDao.countExpensesForCategory(categoryId, profileId)

    override suspend fun createCategory(
        profileId: String,
        name: String,
        iconName: String,
        colorHex: String
    ): CategoryEntity {
        val now = System.currentTimeMillis()
        val category = CategoryEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = name.trim(),
            iconName = iconName,
            colorHex = colorHex,
            isDefault = false,
            createdAt = now,
            updatedAt = now
        )
        categoryDao.insertCategory(category)
        return category
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Enforces SRS FR-CAT-V1.0-005: Requires expense reassignment if deleting a category with expenses.
     */
    override suspend fun deleteCategory(category: CategoryEntity, reassignToCategoryId: String?) {
        val expenseCount = categoryDao.countExpensesForCategory(category.id, category.profileId)
        if (expenseCount > 0) {
            requireNotNull(reassignToCategoryId) {
                "Cannot delete category with $expenseCount active expenses without specifying a reassignment category."
            }
            expenseDao.reassignCategory(category.id, reassignToCategoryId, category.profileId)
        }
        categoryDao.deleteCategory(category)
    }
}
