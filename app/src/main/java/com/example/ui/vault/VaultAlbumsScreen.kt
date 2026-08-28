package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.VaultAlbumEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.VaultViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAlbumsScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onLockVault: () -> Unit
) {
    val albums by viewModel.albums.collectAsState()
    val allMedia by viewModel.allActiveMedia.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    var albumToRename by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var albumToDelete by remember { mutableStateOf<String?>(null) }
    var deleteMediaInside by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Private Albums",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLockVault) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = CyanAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newAlbumName = ""
                    showCreateDialog = true
                },
                containerColor = CyanAccent,
                contentColor = Color(0xFF0A0C10),
                modifier = Modifier.testTag("fab_create_album")
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Create Album")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Prepare list of albums including "Default" album
        val displayAlbums = remember(albums, allMedia) {
            val names = mutableListOf("Default")
            albums.forEach { if (!names.contains(it.name)) names.add(it.name) }
            names
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(displayAlbums) { albumName ->
                val mediaInAlbum = allMedia.filter { it.albumName == albumName }
                val coverMedia = mediaInAlbum.firstOrNull()

                var menuExpanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAlbum(albumName) }
                        .testTag("album_card_$albumName")
                ) {
                    Column {
                        // Album Cover Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.2f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (coverMedia != null) {
                                AsyncImage(
                                    model = File(coverMedia.thumbnailPath ?: coverMedia.filePath),
                                    contentDescription = albumName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = CyanAccent.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            // Overflow menu button for custom albums
                            if (albumName != "Default") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename Album") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                albumToRename = albumName
                                                renameInput = albumName
                                                menuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Album", color = Color(0xFFFF5252)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                                            onClick = {
                                                albumToDelete = albumName
                                                deleteMediaInside = false
                                                menuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Album info
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = albumName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${mediaInAlbum.size} items",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Album Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Private Album", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("Album Name") },
                    placeholder = { Text("e.g. Family, Work, Receipts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAlbumName.isNotBlank()) {
                            viewModel.createAlbum(newAlbumName.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Create", color = Color(0xFF0A0C10), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Album Dialog
    if (albumToRename != null) {
        AlertDialog(
            onDismissRequest = { albumToRename = null },
            title = { Text("Rename Album", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank() && albumToRename != null) {
                            viewModel.renameAlbum(albumToRename!!, renameInput.trim())
                            albumToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Rename", color = Color(0xFF0A0C10), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Album Dialog (Section 8: confirm whether to delete media or keep media in Default)
    if (albumToDelete != null) {
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text("Delete Album '${albumToDelete}'?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose what happens to the files inside this album:", fontSize = 13.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteMediaInside = false }
                    ) {
                        RadioButton(
                            selected = !deleteMediaInside,
                            onClick = { deleteMediaInside = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Keep files (move to Default album)", fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteMediaInside = true }
                    ) {
                        RadioButton(
                            selected = deleteMediaInside,
                            onClick = { deleteMediaInside = true }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete all files inside permanently", fontSize = 13.sp, color = Color(0xFFFF5252))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (albumToDelete != null) {
                            viewModel.deleteAlbum(albumToDelete!!, deleteMediaInside)
                            albumToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete Album", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
