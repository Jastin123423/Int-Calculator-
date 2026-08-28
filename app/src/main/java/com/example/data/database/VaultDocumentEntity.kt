package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_documents")
data class VaultDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileExtension: String,
    val mimeType: String,
    val category: String, // PDF, DOCUMENT, SPREADSHEET, PRESENTATION, TEXT, ARCHIVE, AUDIO, VIDEO, IMAGE, APK, OTHER
    val fileSize: Long,
    val folderName: String = "Documents",
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0L,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val modifiedTimestamp: Long = System.currentTimeMillis(),
    val pageCount: Int = 0
)

@Entity(tableName = "vault_doc_folders")
data class VaultDocFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#00E5FF",
    val iconName: String = "folder",
    val createdTimestamp: Long = System.currentTimeMillis()
)
