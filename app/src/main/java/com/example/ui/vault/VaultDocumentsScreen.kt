package com.example.ui.vault

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.database.VaultDocFolderEntity
import com.example.data.database.VaultDocumentEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.DocCategoryFilter
import com.example.viewmodel.DocSortOption
import com.example.viewmodel.DocViewMode
import com.example.viewmodel.VaultViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDocumentsScreen(
    viewModel: VaultViewModel,
    initialFolder: String? = null,
    onNavigateBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onLockVault: () -> Unit
) {
    val context = LocalContext.current
    if (initialFolder != null) {
        viewModel.selectedDocFolder.value = initialFolder
    }

    val documents by viewModel.displayedDocuments.collectAsState()
    val allActiveDocs by viewModel.allActiveDocuments.collectAsState()
    val folders by viewModel.docFolders.collectAsState()
    val selectedFolder by viewModel.selectedDocFolder.collectAsState()
    val selectedCategory by viewModel.selectedDocCategory.collectAsState()
    val searchQuery by viewModel.docSearchQuery.collectAsState()
    val sortOption by viewModel.docSortOption.collectAsState()
    val viewMode by viewModel.docViewMode.collectAsState()
    val selectedIds by viewModel.selectedDocIds.collectAsState()
    val isSelectionMode by viewModel.isDocSelectionMode.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteFolderDialog by remember { mutableStateOf<String?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var documentForDetails by remember { mutableStateOf<VaultDocumentEntity?>(null) }
    var documentToExport by remember { mutableStateOf<VaultDocumentEntity?>(null) }

    // Export file launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && documentToExport != null) {
            viewModel.exportDocument(documentToExport!!, uri)
            documentToExport = null
        }
    }

    // Document Picker launcher for multiple files
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val targetFolder = selectedFolder ?: "Documents"
            viewModel.importDocumentUris(uris, folderName = targetFolder)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = "${selectedIds.size} Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    } else if (showSearchField) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.docSearchQuery.value = it },
                            placeholder = { Text("Search files, types, folders...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .testTag("doc_search_input")
                        )
                    } else {
                        Text(
                            text = selectedFolder?.let { "Folder: $it" } ?: "Private Documents",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.clearDocSelection()
                            } else if (showSearchField) {
                                showSearchField = false
                                viewModel.docSearchQuery.value = ""
                            } else if (selectedFolder != null) {
                                viewModel.selectedDocFolder.value = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode || showSearchField) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.selectAllDocs(documents) }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All"
                            )
                        }
                    } else {
                        IconButton(onClick = { showSearchField = !showSearchField }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.docViewMode.value = if (viewMode == DocViewMode.LIST) DocViewMode.GRID else DocViewMode.LIST
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == DocViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "Toggle View Mode"
                            )
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort"
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DocSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (sortOption == option) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = CyanAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                Text(option.displayName)
                                            }
                                        },
                                        onClick = {
                                            viewModel.docSortOption.value = option
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onLockVault) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = CyanAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showMoveDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = "Move",
                                    tint = CyanAccent
                                )
                                Text("Move", fontSize = 10.sp, color = CyanAccent)
                            }
                        }

                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF5252)
                                )
                                Text("Delete", fontSize = 10.sp, color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { showCreateFolderDialog = true },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("fab_create_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder"
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "*/*",
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-powerpoint",
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                    "text/plain",
                                    "text/csv",
                                    "application/json",
                                    "application/zip",
                                    "application/x-rar-compressed",
                                    "application/vnd.android.package-archive"
                                )
                            )
                        },
                        containerColor = CyanAccent,
                        contentColor = Color(0xFF0A0C10),
                        modifier = Modifier.testTag("fab_import_document")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import Files"
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Folders Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFolder == null,
                    onClick = { viewModel.selectedDocFolder.value = null },
                    label = { Text("All Folders (${allActiveDocs.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Workspaces,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                        selectedLabelColor = CyanAccent
                    )
                )

                folders.forEach { folder ->
                    val isSelected = selectedFolder.equals(folder.name, ignoreCase = true)
                    val count = allActiveDocs.count { it.folderName.equals(folder.name, ignoreCase = true) }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectedDocFolder.value = if (isSelected) null else folder.name
                        },
                        label = { Text("${folder.name} ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isSelected) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (folder.name !in listOf("Documents", "Work", "Personal")) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Folder Options",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showRenameFolderDialog = folder.name }
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                            selectedLabelColor = CyanAccent
                        )
                    )
                }

                TextButton(onClick = { showCreateFolderDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Folder", fontSize = 12.sp)
                }
            }

            // Categories Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocCategoryFilter.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    val count = if (category == DocCategoryFilter.ALL) {
                        allActiveDocs.size
                    } else {
                        allActiveDocs.count { it.category.equals(category.categoryKey, ignoreCase = true) }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedDocCategory.value = category },
                        label = { Text("${category.displayName} ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(category.categoryKey),
                                contentDescription = null,
                                tint = getCategoryColor(category.categoryKey),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getCategoryColor(category.categoryKey).copy(alpha = 0.15f),
                            selectedLabelColor = getCategoryColor(category.categoryKey)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotBlank()) "No files found for \"$searchQuery\"" else "No private documents yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Import PDF, DOCX, XLSX, TXT, code files, archives, and APKs to store them in your private on-device vault.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "*/*",
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "text/plain",
                                        "application/zip"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = Color(0xFF0A0C10)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Import Files to Vault",
                                color = Color(0xFF0A0C10),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (viewMode == DocViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentListItem(
                            doc = doc,
                            isSelected = selectedIds.contains(doc.id),
                            isSelectionMode = isSelectionMode,
                            onItemClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleDocSelection(doc.id)
                                } else {
                                    onOpenDocument(doc.id)
                                }
                            },
                            onItemLongClick = {
                                viewModel.toggleDocSelection(doc.id)
                            },
                            onToggleFavorite = {
                                viewModel.toggleDocumentFavorite(doc.id, !doc.isFavorite)
                            },
                            onOpenWith = {
                                openWithExternalApp(context, doc)
                            },
                            onExport = {
                                documentToExport = doc
                                exportLauncher.launch(doc.fileName)
                            },
                            onMoveToFolder = {
                                viewModel.selectedDocIds.value = setOf(doc.id)
                                showMoveDialog = true
                            },
                            onDelete = {
                                viewModel.deleteDocument(doc.id)
                            },
                            onShowDetails = {
                                documentForDetails = doc
                            },
                            formatBytes = { viewModel.formatBytes(it) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentGridItem(
                            doc = doc,
                            isSelected = selectedIds.contains(doc.id),
                            isSelectionMode = isSelectionMode,
                            onItemClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleDocSelection(doc.id)
                                } else {
                                    onOpenDocument(doc.id)
                                }
                            },
                            onItemLongClick = {
                                viewModel.toggleDocSelection(doc.id)
                            },
                            onToggleFavorite = {
                                viewModel.toggleDocumentFavorite(doc.id, !doc.isFavorite)
                            },
                            onOpenWith = {
                                openWithExternalApp(context, doc)
                            },
                            onExport = {
                                documentToExport = doc
                                exportLauncher.launch(doc.fileName)
                            },
                            onDelete = {
                                viewModel.deleteDocument(doc.id)
                            },
                            onShowDetails = {
                                documentForDetails = doc
                            },
                            formatBytes = { viewModel.formatBytes(it) }
                        )
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Document Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g. Invoices, Contracts, Work") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createDocFolder(newFolderName)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Create", color = Color(0xFF0A0C10), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Folder Dialog
    if (showRenameFolderDialog != null) {
        val folderToRename = showRenameFolderDialog!!
        var updatedName by remember { mutableStateOf(folderToRename) }
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = null },
            title = { Text("Manage Folder: $folderToRename", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        label = { Text("New Folder Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            showDeleteFolderDialog = folderToRename
                            showRenameFolderDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Folder", color = Color(0xFFFF5252))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (updatedName.isNotBlank() && updatedName != folderToRename) {
                            viewModel.renameDocFolder(folderToRename, updatedName)
                        }
                        showRenameFolderDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Save", color = Color(0xFF0A0C10), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Folder Dialog
    if (showDeleteFolderDialog != null) {
        val folderToDelete = showDeleteFolderDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = null },
            title = { Text("Delete Folder '$folderToDelete'?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Do you want to keep the documents in 'Documents' or delete all files in this folder permanently?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocFolder(folderToDelete, deleteDocs = false)
                        showDeleteFolderDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Keep Files (Move to Default)", color = Color(0xFF0A0C10))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocFolder(folderToDelete, deleteDocs = true)
                        showDeleteFolderDialog = null
                    }
                ) {
                    Text("Delete All Files", color = Color(0xFFFF5252))
                }
            }
        )
    }

    // Move to Folder Dialog
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to Folder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select destination folder:", fontSize = 13.sp)
                    folders.forEach { folder ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveSelectedDocumentsToFolder(folder.name)
                                    showMoveDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = folder.name,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Move to Recently Deleted?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Selected ${selectedIds.size} files will be moved to Recently Deleted. You can restore them within 30 days.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedDocuments()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Move to Trash", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Document Details Dialog
    if (documentForDetails != null) {
        val doc = documentForDetails!!
        val dateFormatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(doc.createdTimestamp))
        val modFormatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(doc.modifiedTimestamp))

        AlertDialog(
            onDismissRequest = { documentForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("File Details", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("File Name", doc.fileName)
                    DetailRow("Folder", doc.folderName)
                    DetailRow("Category", doc.category)
                    DetailRow("File Type", doc.fileExtension.uppercase())
                    DetailRow("MIME Type", doc.mimeType)
                    DetailRow("File Size", viewModel.formatBytes(doc.fileSize))
                    if (doc.pageCount > 0) {
                        DetailRow("Pages", "${doc.pageCount} pages")
                    }
                    DetailRow("Date Added", dateFormatted)
                    DetailRow("Last Modified", modFormatted)
                    DetailRow("Encrypted Storage", "Private Internal App Sandbox (Zero Cloud Upload)")
                }
            },
            confirmButton = {
                Button(
                    onClick = { documentForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Close", color = Color(0xFF0A0C10))
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DocumentListItem(
    doc: VaultDocumentEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenWith: () -> Unit,
    onExport: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    formatBytes: (Long) -> String
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(doc.createdTimestamp))
    val categoryColor = getCategoryColor(doc.category)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyanAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("doc_item_${doc.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection or Category Icon
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Select",
                    tint = if (isSelected) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(doc.category),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Name & Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = doc.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (doc.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Extension Badge
                    Surface(
                        color = categoryColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = doc.fileExtension.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = "• ${formatBytes(doc.fileSize)} • $dateStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Folder badge
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = doc.folderName,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Action 3-dots
            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open / Preview") },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onItemClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open With Other App") },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenWith()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to Device") },
                            leadingIcon = { Icon(Icons.Default.IosShare, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onExport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Folder") },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMoveToFolder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (doc.isFavorite) "Remove Favorite" else "Add to Favorites") },
                            leadingIcon = {
                                Icon(
                                    if (doc.isFavorite) Icons.Default.StarBorder else Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (doc.isFavorite) MaterialTheme.colorScheme.onSurface else Color(0xFFFFD600)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("File Details") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShowDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentGridItem(
    doc: VaultDocumentEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenWith: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    formatBytes: (Long) -> String
) {
    var showMenu by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(doc.category)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyanAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("doc_grid_${doc.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = categoryColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = doc.fileExtension.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Select",
                        tint = if (isSelected) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (doc.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(doc.category),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = doc.fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${formatBytes(doc.fileSize)} • ${doc.folderName}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.uppercase()) {
        "PDF" -> Icons.Default.PictureAsPdf
        "DOCUMENT" -> Icons.Default.Description
        "SPREADSHEET" -> Icons.Default.TableChart
        "PRESENTATION" -> Icons.Default.Workspaces
        "TEXT" -> Icons.Default.Code
        "ARCHIVE" -> Icons.Default.Archive
        "AUDIO" -> Icons.Default.MusicNote
        "VIDEO" -> Icons.Default.Videocam
        "IMAGE" -> Icons.Default.Image
        "APK" -> Icons.Default.Android
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.uppercase()) {
        "PDF" -> Color(0xFFFF5252) // Red/Coral
        "DOCUMENT" -> Color(0xFF448AFF) // Blue
        "SPREADSHEET" -> Color(0xFF00E676) // Emerald Green
        "PRESENTATION" -> Color(0xFFFFAB00) // Amber / Orange
        "TEXT" -> CyanAccent // Cyan
        "ARCHIVE" -> PurpleAccent // Purple
        "AUDIO" -> Color(0xFFFF4081) // Pink
        "VIDEO" -> Color(0xFF7C4DFF) // Deep Purple
        "IMAGE" -> Color(0xFF40C4FF) // Sky Blue
        "APK" -> Color(0xFF76FF03) // Lime Green
        else -> Color(0xFF90A4AE) // Slate Grey
    }
}

fun openWithExternalApp(context: android.content.Context, doc: VaultDocumentEntity) {
    try {
        val file = File(doc.filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, doc.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Open '${doc.fileName}' with")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
