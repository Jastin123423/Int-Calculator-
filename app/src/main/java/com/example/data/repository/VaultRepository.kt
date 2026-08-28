package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.database.VaultAlbumEntity
import com.example.data.database.VaultContactEntity
import com.example.data.database.VaultDao
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

    suspend fun cleanOldTrash() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val oldItems = vaultDao.getOldTrashItems(thirtyDaysAgo)
        oldItems.forEach { media ->
            storageManager.deletePhysicalFile(media.filePath, media.thumbnailPath)
            vaultDao.deleteMediaPermanently(media.id)
        }
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
    suspend fun getStorageBreakdown(photoCount: Int, videoCount: Int): StorageBreakdown {
        return storageManager.getStorageBreakdown(photoCount, videoCount)
    }

    fun formatBytes(bytes: Long): String = storageManager.formatBytes(bytes)
}
