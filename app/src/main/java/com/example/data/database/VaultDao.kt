package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    // --- MEDIA QUERIES ---

    @Query("SELECT * FROM vault_media WHERE isDeleted = 0 ORDER BY createdTimestamp DESC")
    fun getAllActiveMedia(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isDeleted = 0 AND mediaType = :type ORDER BY createdTimestamp DESC")
    fun getMediaByType(type: String): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY createdTimestamp DESC")
    fun getFavoriteMedia(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getRecentlyDeletedMedia(): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isDeleted = 0 AND albumName = :albumName ORDER BY createdTimestamp DESC")
    fun getMediaByAlbum(albumName: String): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isDeleted = 0 AND (fileName LIKE '%' || :query || '%' OR albumName LIKE '%' || :query || '%') ORDER BY createdTimestamp DESC")
    fun searchMedia(query: String): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): VaultMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: VaultMediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<VaultMediaEntity>): List<Long>

    @Update
    suspend fun updateMedia(media: VaultMediaEntity)

    @Query("UPDATE vault_media SET isDeleted = 1, deletedTimestamp = :deletedTimestamp WHERE id = :id")
    suspend fun softDeleteMedia(id: Long, deletedTimestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_media SET isDeleted = 1, deletedTimestamp = :deletedTimestamp WHERE id IN (:ids)")
    suspend fun softDeleteMediaList(ids: List<Long>, deletedTimestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_media SET isDeleted = 0, deletedTimestamp = 0 WHERE id = :id")
    suspend fun restoreMedia(id: Long)

    @Query("UPDATE vault_media SET isDeleted = 0, deletedTimestamp = 0 WHERE id IN (:ids)")
    suspend fun restoreMediaList(ids: List<Long>)

    @Query("UPDATE vault_media SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE vault_media SET albumName = :newAlbum WHERE id = :id")
    suspend fun moveMediaToAlbum(id: Long, newAlbum: String)

    @Query("UPDATE vault_media SET albumName = :newAlbum WHERE id IN (:ids)")
    suspend fun moveMediaListToAlbum(ids: List<Long>, newAlbum: String)

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteMediaPermanently(id: Long)

    @Query("DELETE FROM vault_media WHERE id IN (:ids)")
    suspend fun deleteMediaListPermanently(ids: List<Long>)

    @Query("DELETE FROM vault_media WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM vault_media WHERE isDeleted = 1 AND deletedTimestamp < :threshold")
    suspend fun getOldTrashItems(threshold: Long): List<VaultMediaEntity>

    @Query("SELECT SUM(fileSize) FROM vault_media WHERE isDeleted = 0 AND mediaType = 'PHOTO'")
    fun getTotalPhotoStorage(): Flow<Long?>

    @Query("SELECT SUM(fileSize) FROM vault_media WHERE isDeleted = 0 AND mediaType = 'VIDEO'")
    fun getTotalVideoStorage(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM vault_media WHERE isDeleted = 0 AND mediaType = 'PHOTO'")
    fun getPhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_media WHERE isDeleted = 0 AND mediaType = 'VIDEO'")
    fun getVideoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_media WHERE isDeleted = 1")
    fun getTrashCount(): Flow<Int>

    // --- ALBUM QUERIES ---

    @Query("SELECT * FROM vault_albums ORDER BY createdTimestamp ASC")
    fun getAllAlbums(): Flow<List<VaultAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: VaultAlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: VaultAlbumEntity)

    @Query("DELETE FROM vault_albums WHERE name = :name")
    suspend fun deleteAlbumByName(name: String)

    @Query("UPDATE vault_media SET albumName = :newName WHERE albumName = :oldName")
    suspend fun renameAlbumInMedia(oldName: String, newName: String)

    @Query("UPDATE vault_media SET albumName = 'Default' WHERE albumName = :albumName")
    suspend fun resetAlbumMediaToDefault(albumName: String)

    @Query("SELECT COUNT(*) FROM vault_media WHERE albumName = :albumName AND isDeleted = 0")
    fun getMediaCountInAlbum(albumName: String): Flow<Int>

    // --- CONTACT QUERIES ---

    @Query("SELECT * FROM vault_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<VaultContactEntity>>

    @Query("SELECT * FROM vault_contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchContacts(query: String): Flow<List<VaultContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: VaultContactEntity): Long

    @Update
    suspend fun updateContact(contact: VaultContactEntity)

    @Query("DELETE FROM vault_contacts WHERE id = :id")
    suspend fun deleteContact(id: Long)

    @Query("UPDATE vault_contacts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setContactFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM vault_contacts")
    fun getContactCount(): Flow<Int>
}
