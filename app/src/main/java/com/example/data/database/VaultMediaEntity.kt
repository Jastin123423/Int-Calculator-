package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val fileSize: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val albumName: String = "Default",
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0L,
    val createdTimestamp: Long = System.currentTimeMillis()
)
