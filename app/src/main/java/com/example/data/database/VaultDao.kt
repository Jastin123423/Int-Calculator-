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

    // --- DOCUMENT QUERIES ---

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 0 ORDER BY createdTimestamp DESC")
    fun getAllActiveDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 0 AND folderName = :folderName ORDER BY createdTimestamp DESC")
    fun getDocumentsByFolder(folderName: String): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 0 AND category = :category ORDER BY createdTimestamp DESC")
    fun getDocumentsByCategory(category: String): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY createdTimestamp DESC")
    fun getFavoriteDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getRecentlyDeletedDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 0 AND (fileName LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%' OR fileExtension LIKE '%' || :query || '%') ORDER BY createdTimestamp DESC")
    fun searchDocuments(query: String): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): VaultDocumentEntity?

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 0 AND fileName = :fileName AND folderName = :folderName AND fileSize = :fileSize LIMIT 1")
    suspend fun findDuplicateDocument(fileName: String, folderName: String, fileSize: Long): VaultDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: VaultDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentList(docs: List<VaultDocumentEntity>): List<Long>

    @Update
    suspend fun updateDocument(doc: VaultDocumentEntity)

    @Query("UPDATE vault_documents SET isDeleted = 1, deletedTimestamp = :deletedTimestamp WHERE id = :id")
    suspend fun softDeleteDocument(id: Long, deletedTimestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_documents SET isDeleted = 1, deletedTimestamp = :deletedTimestamp WHERE id IN (:ids)")
    suspend fun softDeleteDocumentList(ids: List<Long>, deletedTimestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_documents SET isDeleted = 0, deletedTimestamp = 0 WHERE id = :id")
    suspend fun restoreDocument(id: Long)

    @Query("UPDATE vault_documents SET isDeleted = 0, deletedTimestamp = 0 WHERE id IN (:ids)")
    suspend fun restoreDocumentList(ids: List<Long>)

    @Query("UPDATE vault_documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setDocumentFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE vault_documents SET folderName = :newFolder, modifiedTimestamp = :modifiedTime WHERE id = :id")
    suspend fun moveDocumentToFolder(id: Long, newFolder: String, modifiedTime: Long = System.currentTimeMillis())

    @Query("UPDATE vault_documents SET folderName = :newFolder, modifiedTimestamp = :modifiedTime WHERE id IN (:ids)")
    suspend fun moveDocumentListToFolder(ids: List<Long>, newFolder: String, modifiedTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM vault_documents WHERE id = :id")
    suspend fun deleteDocumentPermanently(id: Long)

    @Query("DELETE FROM vault_documents WHERE id IN (:ids)")
    suspend fun deleteDocumentListPermanently(ids: List<Long>)

    @Query("DELETE FROM vault_documents WHERE isDeleted = 1")
    suspend fun emptyDocumentTrash()

    @Query("SELECT * FROM vault_documents WHERE isDeleted = 1 AND deletedTimestamp < :threshold")
    suspend fun getOldTrashDocuments(threshold: Long): List<VaultDocumentEntity>

    @Query("SELECT SUM(fileSize) FROM vault_documents WHERE isDeleted = 0")
    fun getTotalDocumentStorage(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM vault_documents WHERE isDeleted = 0")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_documents WHERE isDeleted = 1")
    fun getDocTrashCount(): Flow<Int>

    // --- DOC FOLDER QUERIES ---

    @Query("SELECT * FROM vault_doc_folders ORDER BY createdTimestamp ASC")
    fun getAllDocFolders(): Flow<List<VaultDocFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocFolder(folder: VaultDocFolderEntity): Long

    @Update
    suspend fun updateDocFolder(folder: VaultDocFolderEntity)

    @Query("DELETE FROM vault_doc_folders WHERE name = :name")
    suspend fun deleteDocFolderByName(name: String)

    @Query("UPDATE vault_documents SET folderName = :newName, modifiedTimestamp = :modifiedTime WHERE folderName = :oldName")
    suspend fun renameFolderInDocuments(oldName: String, newName: String, modifiedTime: Long = System.currentTimeMillis())

    @Query("UPDATE vault_documents SET folderName = 'Documents', modifiedTimestamp = :modifiedTime WHERE folderName = :folderName")
    suspend fun resetDocFolderToDefault(folderName: String, modifiedTime: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM vault_documents WHERE folderName = :folderName AND isDeleted = 0")
    fun getDocCountInFolder(folderName: String): Flow<Int>
}
