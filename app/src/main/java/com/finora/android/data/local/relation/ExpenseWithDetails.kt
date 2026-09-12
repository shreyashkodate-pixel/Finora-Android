package com.finora.android.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity

/**
 * Composite relational model combining an Expense with its Category and PaymentMethod.
 */
data class ExpenseWithDetails(
    @Embedded
    val expense: ExpenseEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity,

    @Relation(
        parentColumn = "paymentMethodId",
        entityColumn = "id"
    )
    val paymentMethod: PaymentMethodEntity? = null
)
