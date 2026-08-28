package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_albums")
data class VaultAlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val coverPath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)
