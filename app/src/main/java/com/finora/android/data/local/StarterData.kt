package com.finora.android.data.local

import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import java.util.UUID

object StarterData {

    fun createStarterCategories(profileId: String): List<CategoryEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Food & Dining",
                iconName = "restaurant",
                colorHex = "#046B5E",
                isDefault = true,
                displayOrder = 1,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Transport",
                iconName = "directions_car",
                colorHex = "#004D40",
                isDefault = true,
                displayOrder = 2,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Rent & Housing",
                iconName = "home",
                colorHex = "#203037",
                isDefault = true,
                displayOrder = 3,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Shopping",
                iconName = "shopping_bag",
                colorHex = "#29695B",
                isDefault = true,
                displayOrder = 4,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Bills & Utilities",
                iconName = "receipt_long",
                colorHex = "#36464E",
                isDefault = true,
                displayOrder = 5,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Entertainment",
                iconName = "movie",
                colorHex = "#0F6F62",
                isDefault = true,
                displayOrder = 6,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Health & Healthcare",
                iconName = "health_and_safety",
                colorHex = "#BA1A1A",
                isDefault = true,
                displayOrder = 7,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Education",
                iconName = "school",
                colorHex = "#00342B",
                isDefault = true,
                displayOrder = 8,
                createdAt = now,
                updatedAt = now
            ),
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Other",
                iconName = "more_horiz",
                colorHex = "#707975",
                isDefault = true,
                displayOrder = 9,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun createStarterPaymentMethods(profileId: String): List<PaymentMethodEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            PaymentMethodEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Cash",
                type = "CASH",
                isDefault = true,
                createdAt = now,
                updatedAt = now
            ),
            PaymentMethodEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "UPI",
                type = "UPI",
                isDefault = false,
                createdAt = now,
                updatedAt = now
            ),
            PaymentMethodEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Debit Card",
                type = "DEBIT_CARD",
                isDefault = false,
                createdAt = now,
                updatedAt = now
            ),
            PaymentMethodEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Credit Card",
                type = "CREDIT_CARD",
                isDefault = false,
                createdAt = now,
                updatedAt = now
            ),
            PaymentMethodEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Bank Transfer",
                type = "BANK_TRANSFER",
                isDefault = false,
                createdAt = now,
                updatedAt = now
            ),
            PaymentMethodEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Digital Wallet",
                type = "DIGITAL_WALLET",
                isDefault = false,
                createdAt = now,
                updatedAt = now
            ),
            PaymentMethodEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = "Other",
                type = "OTHER",
                isDefault = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
