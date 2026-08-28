package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_contacts")
data class VaultContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val notes: String = "",
    val avatarPath: String? = null,
    val isFavorite: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
