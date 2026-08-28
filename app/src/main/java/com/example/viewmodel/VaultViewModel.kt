package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.VaultAlbumEntity
import com.example.data.database.VaultContactEntity
import com.example.data.database.VaultDocFolderEntity
import com.example.data.database.VaultDocumentEntity
import com.example.data.database.VaultMediaEntity
import com.example.data.repository.VaultRepository
import com.example.data.security.VaultSecurityManager
import com.example.data.storage.StorageBreakdown
import com.example.data.storage.VaultStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    NEWEST,
    OLDEST,
    NAME,
    SIZE
}

enum class MediaFilter {
    ALL,
    PHOTOS,
    VIDEOS,
    FAVORITES
}

enum class DocSortOption(val displayName: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_DESC("Largest First"),
    SIZE_ASC("Smallest First"),
    TYPE("File Type")
}

enum class DocCategoryFilter(val displayName: String, val categoryKey: String) {
    ALL("All Files", "ALL"),
    DOCUMENT("Documents", "DOCUMENT"),
    PDF("PDFs", "PDF"),
    SPREADSHEET("Spreadsheets", "SPREADSHEET"),
    PRESENTATION("Presentations", "PRESENTATION"),
    TEXT("Text & Code", "TEXT"),
    ARCHIVE("Archives", "ARCHIVE"),
    AUDIO("Audio", "AUDIO"),
    VIDEO("Videos", "VIDEO"),
    IMAGE("Images", "IMAGE"),
    APK("APKs", "APK"),
    OTHER("Other", "OTHER")
}

enum class DocViewMode {
    LIST,
    GRID
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = VaultStorageManager(application)
    private val repository = VaultRepository(
        AppDatabase.getDatabase(application).vaultDao(),
        storageManager
    )
    val securityManager = VaultSecurityManager(application)

    // Security states
    val isVaultSetUp: StateFlow<Boolean> = securityManager.isVaultSetUpFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val isVaultUnlocked: StateFlow<Boolean> = securityManager.isVaultUnlocked

    val isBiometricEnabled: StateFlow<Boolean> = securityManager.isBiometricEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val isScreenProtectionEnabled: StateFlow<Boolean> = securityManager.isScreenProtectionEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val isAutoLockOnBackground: StateFlow<Boolean> = securityManager.isAutoLockOnBackgroundFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val autoLockTimeout: StateFlow<Int> = securityManager.autoLockTimeoutFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val securityQuestion: StateFlow<String> = securityManager.securityQuestionFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "What is your secret passkey question?"
    )

    // Raw flows
    val allActiveMedia: StateFlow<List<VaultMediaEntity>> = repository.getAllActiveMedia().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val trashMedia: StateFlow<List<VaultMediaEntity>> = repository.getRecentlyDeletedMedia().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val albums: StateFlow<List<VaultAlbumEntity>> = repository.getAllAlbums().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allContacts: StateFlow<List<VaultContactEntity>> = repository.getAllContacts().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Document flows
    val allActiveDocuments: StateFlow<List<VaultDocumentEntity>> = repository.getAllActiveDocuments().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val trashDocuments: StateFlow<List<VaultDocumentEntity>> = repository.getRecentlyDeletedDocuments().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val docFolders: StateFlow<List<VaultDocFolderEntity>> = repository.getAllDocFolders().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Filter & Search states for Media
    val searchQuery = MutableStateFlow("")
    val selectedAlbum = MutableStateFlow<String?>(null)
    val sortOption = MutableStateFlow(SortOption.NEWEST)
    val mediaFilter = MutableStateFlow(MediaFilter.ALL)
    val selectedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode: StateFlow<Boolean> = selectedMediaIds.mapStateFlow { it.isNotEmpty() }

    // Document filter, search, view mode states
    val docSearchQuery = MutableStateFlow("")
    val selectedDocFolder = MutableStateFlow<String?>(null)
    val selectedDocCategory = MutableStateFlow(DocCategoryFilter.ALL)
    val docSortOption = MutableStateFlow(DocSortOption.NEWEST)
    val docViewMode = MutableStateFlow(DocViewMode.LIST)
    val selectedDocIds = MutableStateFlow<Set<Long>>(emptySet())
    val isDocSelectionMode: StateFlow<Boolean> = selectedDocIds.mapStateFlow { it.isNotEmpty() }

    // Loading & Feedback states
    val isImporting = MutableStateFlow(false)
    val importProgress = MutableStateFlow("")
    val toastMessage = MutableStateFlow<String?>(null)

    // Storage stats
    private val _storageBreakdown = MutableStateFlow(StorageBreakdown())
    val storageBreakdown: StateFlow<StorageBreakdown> = _storageBreakdown.asStateFlow()

    init {
        refreshStorageBreakdown()
        viewModelScope.launch {
            repository.cleanOldTrash()
            ensureDefaultDocFolders()
        }
    }

    private suspend fun ensureDefaultDocFolders() {
        val defaultFolders = listOf("Documents", "Work", "Personal", "Financial", "Receipts")
        defaultFolders.forEach { folder ->
            repository.createDocFolder(folder)
        }
    }

    // Filtered & Sorted Media Stream
    val displayedMedia: StateFlow<List<VaultMediaEntity>> = combine(
        allActiveMedia,
        searchQuery,
        selectedAlbum,
        sortOption,
        mediaFilter
    ) { mediaList, query, album, sort, filter ->
        var list = mediaList

        if (album != null) {
            list = list.filter { it.albumName == album }
        }

        list = when (filter) {
            MediaFilter.ALL -> list
            MediaFilter.PHOTOS -> list.filter { it.mediaType == "PHOTO" }
            MediaFilter.VIDEOS -> list.filter { it.mediaType == "VIDEO" }
            MediaFilter.FAVORITES -> list.filter { it.isFavorite }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.fileName.lowercase().contains(q) || it.albumName.lowercase().contains(q)
            }
        }

        when (sort) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdTimestamp }
            SortOption.OLDEST -> list.sortedBy { it.createdTimestamp }
            SortOption.NAME -> list.sortedBy { it.fileName.lowercase() }
            SortOption.SIZE -> list.sortedByDescending { it.fileSize }
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Filtered & Sorted Document Stream
    val displayedDocuments: StateFlow<List<VaultDocumentEntity>> = combine(
        allActiveDocuments,
        docSearchQuery,
        selectedDocFolder,
        selectedDocCategory,
        docSortOption
    ) { docs, query, folder, category, sort ->
        var list = docs

        // Folder filter
        if (folder != null) {
            list = list.filter { it.folderName.equals(folder, ignoreCase = true) }
        }

        // Category filter
        if (category != DocCategoryFilter.ALL) {
            list = list.filter { it.category.equals(category.categoryKey, ignoreCase = true) }
        }

        // Search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.fileName.lowercase().contains(q) ||
                        it.fileExtension.lowercase().contains(q) ||
                        it.folderName.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            }
        }

        // Sorting
        when (sort) {
            DocSortOption.NEWEST -> list.sortedByDescending { it.createdTimestamp }
            DocSortOption.OLDEST -> list.sortedBy { it.createdTimestamp }
            DocSortOption.NAME_ASC -> list.sortedBy { it.fileName.lowercase() }
            DocSortOption.NAME_DESC -> list.sortedByDescending { it.fileName.lowercase() }
            DocSortOption.SIZE_DESC -> list.sortedByDescending { it.fileSize }
            DocSortOption.SIZE_ASC -> list.sortedBy { it.fileSize }
            DocSortOption.TYPE -> list.sortedWith(compareBy({ it.category }, { it.fileName.lowercase() }))
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Filtered Contacts
    val displayedContacts: StateFlow<List<VaultContactEntity>> = combine(
        allContacts,
        searchQuery
    ) { contacts, query ->
        if (query.isBlank()) {
            contacts
        } else {
            val q = query.trim().lowercase()
            contacts.filter {
                it.name.lowercase().contains(q) || it.phoneNumber.contains(q) || it.notes.lowercase().contains(q)
            }
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun refreshStorageBreakdown() {
        viewModelScope.launch {
            val photos = allActiveMedia.value.count { it.mediaType == "PHOTO" }
            val videos = allActiveMedia.value.count { it.mediaType == "VIDEO" }
            val docs = allActiveDocuments.value.size
            _storageBreakdown.value = repository.getStorageBreakdown(photos, videos, docs)
        }
    }

    // --- AUTH ACTIONS ---
    suspend fun verifyPin(pin: String): Boolean {
        return securityManager.verifyPin(pin)
    }

    suspend fun setMasterPin(pin: String, question: String = "", answer: String = "") {
        securityManager.setMasterPin(pin, question, answer)
        toastMessage.value = "Master PIN configured securely"
    }

    suspend fun resetPin(answer: String, newPin: String): Boolean {
        val success = securityManager.resetPinWithSecurityAnswer(answer, newPin)
        if (success) {
            toastMessage.value = "PIN reset successfully"
        }
        return success
    }

    fun unlockWithBiometrics() {
        securityManager.forceUnlock()
    }

    fun lockVault() {
        securityManager.lockVault()
        clearSelection()
        clearDocSelection()
        searchQuery.value = ""
        docSearchQuery.value = ""
        selectedAlbum.value = null
        selectedDocFolder.value = null
    }

    fun checkBackgroundTimeout() {
        val timeout = autoLockTimeout.value
        securityManager.checkBackgroundTimeout(timeout)
    }

    fun markUserActivity() {
        securityManager.markUserActivity()
    }

    // --- IMPORT ACTIONS (MEDIA) ---
    fun importMediaUris(uris: List<Uri>, isVideo: Boolean, albumName: String = "Default") {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isImporting.value = true
            var importedCount = 0
            val total = uris.size

            uris.forEachIndexed { index, uri ->
                importProgress.value = "Importing ${index + 1} of $total..."
                val imported = if (isVideo) {
                    repository.importVideo(uri, albumName)
                } else {
                    repository.importPhoto(uri, albumName)
                }
                if (imported != null) importedCount++
            }

            isImporting.value = false
            importProgress.value = ""
            toastMessage.value = "Imported $importedCount item${if (importedCount != 1) "s" else ""} to Vault"
            refreshStorageBreakdown()
        }
    }

    // --- IMPORT ACTIONS (DOCUMENTS & FILES) ---
    fun importDocumentUris(
        uris: List<Uri>,
        folderName: String = "Documents",
        onDuplicateFound: ((Uri, String, () -> Unit, () -> Unit) -> Unit)? = null
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isImporting.value = true
            var importedCount = 0
            val total = uris.size

            uris.forEachIndexed { index, uri ->
                importProgress.value = "Importing file ${index + 1} of $total..."
                val imported = repository.importDocument(uri, folderName)
                if (imported != null) {
                    importedCount++
                }
            }

            isImporting.value = false
            importProgress.value = ""
            toastMessage.value = "Imported $importedCount document${if (importedCount != 1) "s" else ""} to $folderName"
            refreshStorageBreakdown()
        }
    }

    suspend fun getDocumentById(id: Long): VaultDocumentEntity? {
        return repository.getDocumentById(id)
    }

    // --- DOCUMENT MANAGEMENT ---
    fun toggleDocumentFavorite(id: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleDocumentFavorite(id, isFav)
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            repository.softDeleteDocument(id)
            toastMessage.value = "Document moved to Recently Deleted"
            refreshStorageBreakdown()
        }
    }

    fun deleteSelectedDocuments() {
        val ids = selectedDocIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.softDeleteDocumentList(ids)
            clearDocSelection()
            toastMessage.value = "Moved ${ids.size} files to Recently Deleted"
            refreshStorageBreakdown()
        }
    }

    fun restoreDocument(id: Long) {
        viewModelScope.launch {
            repository.restoreDocument(id)
            toastMessage.value = "Restored document"
            refreshStorageBreakdown()
        }
    }

    fun restoreSelectedDocuments() {
        val ids = selectedDocIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.restoreDocumentList(ids)
            clearDocSelection()
            toastMessage.value = "Restored ${ids.size} documents"
            refreshStorageBreakdown()
        }
    }

    fun deleteDocumentPermanently(doc: VaultDocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocumentPermanently(doc)
            toastMessage.value = "File permanently deleted"
            refreshStorageBreakdown()
        }
    }

    fun deleteSelectedDocumentsPermanently() {
        val ids = selectedDocIds.value
        val itemsToDelete = trashDocuments.value.filter { it.id in ids }
        if (itemsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.deleteDocumentListPermanently(itemsToDelete)
            clearDocSelection()
            toastMessage.value = "Permanently deleted ${itemsToDelete.size} files"
            refreshStorageBreakdown()
        }
    }

    fun emptyDocumentTrash() {
        val items = trashDocuments.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.emptyDocumentTrash(items)
            toastMessage.value = "Document trash emptied"
            refreshStorageBreakdown()
        }
    }

    fun moveDocumentToFolder(id: Long, newFolder: String) {
        viewModelScope.launch {
            repository.moveDocumentToFolder(id, newFolder)
            toastMessage.value = "Moved to $newFolder"
        }
    }

    fun moveSelectedDocumentsToFolder(newFolder: String) {
        val ids = selectedDocIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveDocumentListToFolder(ids, newFolder)
            clearDocSelection()
            toastMessage.value = "Moved ${ids.size} files to $newFolder"
        }
    }

    fun exportDocument(doc: VaultDocumentEntity, destinationUri: Uri) {
        viewModelScope.launch {
            val success = repository.exportDocument(doc, destinationUri)
            if (success) {
                toastMessage.value = "Exported '${doc.fileName}' to device"
            } else {
                toastMessage.value = "Export failed"
            }
        }
    }

    // --- DOC FOLDER MANAGEMENT ---
    fun createDocFolder(name: String, colorHex: String = "#00E5FF") {
        viewModelScope.launch {
            val success = repository.createDocFolder(name, colorHex)
            if (success) {
                toastMessage.value = "Folder '$name' created"
            } else {
                toastMessage.value = "Folder already exists or invalid"
            }
        }
    }

    fun renameDocFolder(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renameDocFolder(oldName, newName)
            toastMessage.value = "Folder renamed to '$newName'"
            if (selectedDocFolder.value == oldName) {
                selectedDocFolder.value = newName
            }
        }
    }

    fun deleteDocFolder(folderName: String, deleteDocs: Boolean) {
        val docsInFolder = allActiveDocuments.value.filter { it.folderName == folderName }
        viewModelScope.launch {
            repository.deleteDocFolder(folderName, deleteDocs, docsInFolder)
            if (selectedDocFolder.value == folderName) {
                selectedDocFolder.value = null
            }
            toastMessage.value = "Folder '$folderName' deleted"
            refreshStorageBreakdown()
        }
    }

    // --- DOC SELECTION HELPERS ---
    fun toggleDocSelection(id: Long) {
        val current = selectedDocIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        selectedDocIds.value = current
    }

    fun selectAllDocs(docList: List<VaultDocumentEntity>) {
        selectedDocIds.value = docList.map { it.id }.toSet()
    }

    fun clearDocSelection() {
        selectedDocIds.value = emptySet()
    }

    // --- MEDIA MANAGEMENT ---
    fun toggleFavorite(id: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, isFav)
        }
    }

    fun deleteMedia(id: Long) {
        viewModelScope.launch {
            repository.softDeleteMedia(id)
            toastMessage.value = "Moved to Recently Deleted"
            refreshStorageBreakdown()
        }
    }

    fun deleteSelectedMedia() {
        val ids = selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.softDeleteMediaList(ids)
            clearSelection()
            toastMessage.value = "Moved ${ids.size} items to Recently Deleted"
            refreshStorageBreakdown()
        }
    }

    fun restoreMedia(id: Long) {
        viewModelScope.launch {
            repository.restoreMedia(id)
            toastMessage.value = "Restored to Gallery"
            refreshStorageBreakdown()
        }
    }

    fun restoreSelectedMedia() {
        val ids = selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.restoreMediaList(ids)
            clearSelection()
            toastMessage.value = "Restored ${ids.size} items"
            refreshStorageBreakdown()
        }
    }

    fun deletePermanently(media: VaultMediaEntity) {
        viewModelScope.launch {
            repository.deletePermanently(media)
            toastMessage.value = "Permanently deleted"
            refreshStorageBreakdown()
        }
    }

    fun deleteSelectedPermanently() {
        val ids = selectedMediaIds.value
        val itemsToDelete = trashMedia.value.filter { it.id in ids }
        if (itemsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.deleteListPermanently(itemsToDelete)
            clearSelection()
            toastMessage.value = "Permanently deleted ${itemsToDelete.size} items"
            refreshStorageBreakdown()
        }
    }

    fun emptyTrash() {
        val items = trashMedia.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.emptyTrash(items)
            toastMessage.value = "Trash emptied"
            refreshStorageBreakdown()
        }
    }

    fun emptyAllTrash() {
        emptyTrash()
        emptyDocumentTrash()
    }

    fun moveMediaToAlbum(id: Long, newAlbum: String) {
        viewModelScope.launch {
            repository.moveMediaToAlbum(id, newAlbum)
            toastMessage.value = "Moved to $newAlbum"
        }
    }

    fun moveSelectedMediaToAlbum(newAlbum: String) {
        val ids = selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveMediaListToAlbum(ids, newAlbum)
            clearSelection()
            toastMessage.value = "Moved ${ids.size} items to $newAlbum"
        }
    }

    // --- ALBUM MANAGEMENT ---
    fun createAlbum(name: String) {
        viewModelScope.launch {
            val success = repository.createAlbum(name)
            if (success) {
                toastMessage.value = "Album '$name' created"
            } else {
                toastMessage.value = "Album already exists or invalid"
            }
        }
    }

    fun renameAlbum(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renameAlbum(oldName, newName)
            toastMessage.value = "Album renamed to '$newName'"
            if (selectedAlbum.value == oldName) {
                selectedAlbum.value = newName
            }
        }
    }

    fun deleteAlbum(albumName: String, deleteMedia: Boolean) {
        val mediaInAlbum = allActiveMedia.value.filter { it.albumName == albumName }
        viewModelScope.launch {
            repository.deleteAlbum(albumName, deleteMedia, mediaInAlbum)
            if (selectedAlbum.value == albumName) {
                selectedAlbum.value = null
            }
            toastMessage.value = "Album '$albumName' deleted"
            refreshStorageBreakdown()
        }
    }

    // --- CONTACTS MANAGEMENT ---
    fun addOrUpdateContact(contact: VaultContactEntity) {
        viewModelScope.launch {
            repository.addOrUpdateContact(contact)
            toastMessage.value = if (contact.id == 0L) "Private Contact Saved" else "Contact Updated"
            refreshStorageBreakdown()
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
            toastMessage.value = "Private Contact Deleted"
            refreshStorageBreakdown()
        }
    }

    fun toggleContactFavorite(id: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleContactFavorite(id, isFav)
        }
    }

    suspend fun saveContactAvatar(uri: Uri): String? {
        return repository.saveContactAvatar(uri)
    }

    // --- SELECTION HELPERS ---
    fun toggleSelection(id: Long) {
        val current = selectedMediaIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        selectedMediaIds.value = current
    }

    fun selectAll(mediaList: List<VaultMediaEntity>) {
        selectedMediaIds.value = mediaList.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedMediaIds.value = emptySet()
    }

    // --- SECURITY SETTINGS ---
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityManager.setBiometricEnabled(enabled)
        }
    }

    fun setScreenProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityManager.setScreenProtectionEnabled(enabled)
        }
    }

    fun setAutoLockOnBackground(enabled: Boolean) {
        viewModelScope.launch {
            securityManager.setAutoLockOnBackground(enabled)
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        viewModelScope.launch {
            securityManager.setAutoLockTimeout(seconds)
        }
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun formatBytes(bytes: Long): String = repository.formatBytes(bytes)

    private fun <T, R> StateFlow<T>.mapStateFlow(transform: (T) -> R): StateFlow<R> {
        val initial = transform(this.value)
        val mutable = MutableStateFlow(initial)
        viewModelScope.launch {
            this@mapStateFlow.collect {
                mutable.value = transform(it)
            }
        }
        return mutable.asStateFlow()
    }
}
