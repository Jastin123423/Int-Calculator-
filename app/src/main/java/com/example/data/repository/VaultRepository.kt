package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.VaultAlbumEntity
import com.example.data.database.VaultContactEntity
import com.example.data.database.VaultDao
import com.example.data.database.VaultDocFolderEntity
import com.example.data.database.VaultDocumentEntity
import com.example.data.database.VaultMediaEntity
import com.example.data.storage.StorageBreakdown
import com.example.data.storage.VaultStorageManager
import kotlinx.coroutines.flow.Flow

class VaultRepository(
    private val vaultDao: VaultDao,
    private val storageManager: VaultStorageManager
) {

    // --- MEDIA ---
    fun getAllActiveMedia(): Flow<List<VaultMediaEntity>> = vaultDao.getAllActiveMedia()
    fun getMediaByType(type: String): Flow<List<VaultMediaEntity>> = vaultDao.getMediaByType(type)
    fun getFavoriteMedia(): Flow<List<VaultMediaEntity>> = vaultDao.getFavoriteMedia()
    fun getRecentlyDeletedMedia(): Flow<List<VaultMediaEntity>> = vaultDao.getRecentlyDeletedMedia()
    fun getMediaByAlbum(album: String): Flow<List<VaultMediaEntity>> = vaultDao.getMediaByAlbum(album)
    fun searchMedia(query: String): Flow<List<VaultMediaEntity>> = vaultDao.searchMedia(query)

    fun getPhotoCount(): Flow<Int> = vaultDao.getPhotoCount()
    fun getVideoCount(): Flow<Int> = vaultDao.getVideoCount()
    fun getTrashCount(): Flow<Int> = vaultDao.getTrashCount()

    suspend fun getMediaById(id: Long): VaultMediaEntity? = vaultDao.getMediaById(id)

    suspend fun importPhoto(uri: Uri, albumName: String = "Default"): VaultMediaEntity? {
        val entity = storageManager.importPhoto(uri, albumName)
        if (entity != null) {
            val id = vaultDao.insertMedia(entity)
            return entity.copy(id = id)
        }
        return null
    }

    suspend fun importVideo(uri: Uri, albumName: String = "Default"): VaultMediaEntity? {
        val entity = storageManager.importVideo(uri, albumName)
        if (entity != null) {
            val id = vaultDao.insertMedia(entity)
            return entity.copy(id = id)
        }
        return null
    }

    suspend fun softDeleteMedia(id: Long) {
        vaultDao.softDeleteMedia(id, System.currentTimeMillis())
    }

    suspend fun softDeleteMediaList(ids: List<Long>) {
        vaultDao.softDeleteMediaList(ids, System.currentTimeMillis())
    }

    suspend fun restoreMedia(id: Long) {
        vaultDao.restoreMedia(id)
    }

    suspend fun restoreMediaList(ids: List<Long>) {
        vaultDao.restoreMediaList(ids)
    }

    suspend fun toggleFavorite(id: Long, isFav: Boolean) {
        vaultDao.setFavorite(id, isFav)
    }

    suspend fun moveMediaToAlbum(id: Long, album: String) {
        vaultDao.moveMediaToAlbum(id, album)
    }

    suspend fun moveMediaListToAlbum(ids: List<Long>, album: String) {
        vaultDao.moveMediaListToAlbum(ids, album)
    }

    suspend fun deletePermanently(media: VaultMediaEntity) {
        storageManager.deletePhysicalFile(media.filePath, media.thumbnailPath)
        vaultDao.deleteMediaPermanently(media.id)
    }

    suspend fun deleteListPermanently(mediaList: List<VaultMediaEntity>) {
        mediaList.forEach { media ->
            storageManager.deletePhysicalFile(media.filePath, media.thumbnailPath)
        }
        vaultDao.deleteMediaListPermanently(mediaList.map { it.id })
    }

    suspend fun emptyTrash(trashList: List<VaultMediaEntity>) {
        trashList.forEach { media ->
            storageManager.deletePhysicalFile(media.filePath, media.thumbnailPath)
        }
        vaultDao.emptyTrash()
    }

    // --- DOCUMENTS & FILES ---

    fun getAllActiveDocuments(): Flow<List<VaultDocumentEntity>> = vaultDao.getAllActiveDocuments()
    fun getDocumentsByFolder(folderName: String): Flow<List<VaultDocumentEntity>> = vaultDao.getDocumentsByFolder(folderName)
    fun getDocumentsByCategory(category: String): Flow<List<VaultDocumentEntity>> = vaultDao.getDocumentsByCategory(category)
    fun getFavoriteDocuments(): Flow<List<VaultDocumentEntity>> = vaultDao.getFavoriteDocuments()
    fun getRecentlyDeletedDocuments(): Flow<List<VaultDocumentEntity>> = vaultDao.getRecentlyDeletedDocuments()
    fun searchDocuments(query: String): Flow<List<VaultDocumentEntity>> = vaultDao.searchDocuments(query)

    fun getDocumentCount(): Flow<Int> = vaultDao.getDocumentCount()
    fun getDocTrashCount(): Flow<Int> = vaultDao.getDocTrashCount()

    suspend fun getDocumentById(id: Long): VaultDocumentEntity? = vaultDao.getDocumentById(id)

    suspend fun checkDuplicateDocument(fileName: String, folderName: String, fileSize: Long): VaultDocumentEntity? {
        return vaultDao.findDuplicateDocument(fileName, folderName, fileSize)
    }

    suspend fun importDocument(
        uri: Uri,
        folderName: String = "Documents",
        customFileName: String? = null
    ): VaultDocumentEntity? {
        val entity = storageManager.importDocument(uri, folderName, customFileName)
        if (entity != null) {
            val id = vaultDao.insertDocument(entity)
            return entity.copy(id = id)
        }
        return null
    }

    suspend fun exportDocument(doc: VaultDocumentEntity, destinationUri: Uri): Boolean {
        return storageManager.exportDocument(doc, destinationUri)
    }

    suspend fun softDeleteDocument(id: Long) {
        vaultDao.softDeleteDocument(id, System.currentTimeMillis())
    }

    suspend fun softDeleteDocumentList(ids: List<Long>) {
        vaultDao.softDeleteDocumentList(ids, System.currentTimeMillis())
    }

    suspend fun restoreDocument(id: Long) {
        vaultDao.restoreDocument(id)
    }

    suspend fun restoreDocumentList(ids: List<Long>) {
        vaultDao.restoreDocumentList(ids)
    }

    suspend fun toggleDocumentFavorite(id: Long, isFav: Boolean) {
        vaultDao.setDocumentFavorite(id, isFav)
    }

    suspend fun moveDocumentToFolder(id: Long, newFolder: String) {
        vaultDao.moveDocumentToFolder(id, newFolder)
    }

    suspend fun moveDocumentListToFolder(ids: List<Long>, newFolder: String) {
        vaultDao.moveDocumentListToFolder(ids, newFolder)
    }

    suspend fun deleteDocumentPermanently(doc: VaultDocumentEntity) {
        storageManager.deletePhysicalFile(doc.filePath)
        vaultDao.deleteDocumentPermanently(doc.id)
    }

    suspend fun deleteDocumentListPermanently(docs: List<VaultDocumentEntity>) {
        docs.forEach { doc ->
            storageManager.deletePhysicalFile(doc.filePath)
        }
        vaultDao.deleteDocumentListPermanently(docs.map { it.id })
    }

    suspend fun emptyDocumentTrash(trashDocs: List<VaultDocumentEntity>) {
        trashDocs.forEach { doc ->
            storageManager.deletePhysicalFile(doc.filePath)
        }
        vaultDao.emptyDocumentTrash()
    }

    suspend fun cleanOldTrash() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val oldItems = vaultDao.getOldTrashItems(thirtyDaysAgo)
        oldItems.forEach { media ->
            storageManager.deletePhysicalFile(media.filePath, media.thumbnailPath)
            vaultDao.deleteMediaPermanently(media.id)
        }
        val oldDocs = vaultDao.getOldTrashDocuments(thirtyDaysAgo)
        oldDocs.forEach { doc ->
            storageManager.deletePhysicalFile(doc.filePath)
            vaultDao.deleteDocumentPermanently(doc.id)
        }
    }

    // --- DOC FOLDERS ---
    fun getAllDocFolders(): Flow<List<VaultDocFolderEntity>> = vaultDao.getAllDocFolders()

    suspend fun createDocFolder(name: String, colorHex: String = "#00E5FF"): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val entity = VaultDocFolderEntity(name = trimmed, colorHex = colorHex)
        val id = vaultDao.insertDocFolder(entity)
        return id > 0
    }

    suspend fun renameDocFolder(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == oldName) return
        vaultDao.deleteDocFolderByName(oldName)
        vaultDao.insertDocFolder(VaultDocFolderEntity(name = trimmed))
        vaultDao.renameFolderInDocuments(oldName, trimmed)
    }

    suspend fun deleteDocFolder(folderName: String, deleteContainedDocs: Boolean, docsInFolder: List<VaultDocumentEntity>) {
        if (deleteContainedDocs) {
            deleteDocumentListPermanently(docsInFolder)
        } else {
            vaultDao.resetDocFolderToDefault(folderName)
        }
        vaultDao.deleteDocFolderByName(folderName)
    }

    // --- ALBUMS ---
    fun getAllAlbums(): Flow<List<VaultAlbumEntity>> = vaultDao.getAllAlbums()

    suspend fun createAlbum(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val entity = VaultAlbumEntity(name = trimmed)
        val id = vaultDao.insertAlbum(entity)
        return id > 0
    }

    suspend fun renameAlbum(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == oldName) return
        vaultDao.deleteAlbumByName(oldName)
        vaultDao.insertAlbum(VaultAlbumEntity(name = trimmed))
        vaultDao.renameAlbumInMedia(oldName, trimmed)
    }

    suspend fun deleteAlbum(albumName: String, deleteContainedMedia: Boolean, mediaInAlbum: List<VaultMediaEntity>) {
        if (deleteContainedMedia) {
            deleteListPermanently(mediaInAlbum)
        } else {
            vaultDao.resetAlbumMediaToDefault(albumName)
        }
        vaultDao.deleteAlbumByName(albumName)
    }

    // --- CONTACTS ---
    fun getAllContacts(): Flow<List<VaultContactEntity>> = vaultDao.getAllContacts()
    fun searchContacts(query: String): Flow<List<VaultContactEntity>> = vaultDao.searchContacts(query)
    fun getContactCount(): Flow<Int> = vaultDao.getContactCount()

    suspend fun addOrUpdateContact(contact: VaultContactEntity) {
        if (contact.id == 0L) {
            vaultDao.insertContact(contact)
        } else {
            vaultDao.updateContact(contact)
        }
    }

    suspend fun deleteContact(id: Long) {
        vaultDao.deleteContact(id)
    }

    suspend fun toggleContactFavorite(id: Long, isFav: Boolean) {
        vaultDao.setContactFavorite(id, isFav)
    }

    suspend fun saveContactAvatar(uri: Uri): String? {
        return storageManager.saveContactAvatar(uri)
    }

    // --- STORAGE ---
    suspend fun getStorageBreakdown(photoCount: Int, videoCount: Int, docCount: Int): StorageBreakdown {
        return storageManager.getStorageBreakdown(photoCount, videoCount, docCount)
    }

    fun formatBytes(bytes: Long): String = storageManager.formatBytes(bytes)
    fun queryFileName(uri: Uri): String? = storageManager.queryFileName(uri)
}
