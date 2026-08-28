package com.example.ui.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.VaultMediaEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.MediaFilter
import com.example.viewmodel.VaultViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    viewModel: VaultViewModel,
    onNavigateToPhotos: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToMediaViewer: (Long) -> Unit,
    onLockVault: () -> Unit
) {
    val allMedia by viewModel.allActiveMedia.collectAsState()
    val allDocs by viewModel.allActiveDocuments.collectAsState()
    val trashMedia by viewModel.trashMedia.collectAsState()
    val trashDocs by viewModel.trashDocuments.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val contacts by viewModel.allContacts.collectAsState()
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    val photoCount = allMedia.count { it.mediaType == "PHOTO" }
    val videoCount = allMedia.count { it.mediaType == "VIDEO" }
    val docCount = allDocs.size
    val favoriteCount = allMedia.count { it.isFavorite } + allDocs.count { it.isFavorite }
    val totalTrashCount = trashMedia.size + trashDocs.size

    // Multi-Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris, isVideo = false)
        }
    }

    // Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris, isVideo = true)
        }
    }

    // Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importDocumentUris(uris, folderName = "Documents")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(CyanAccent, PurpleAccent))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private Vault",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Private Vault",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Encrypted On-Device Engine",
                                fontSize = 11.sp,
                                color = CyanAccent
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = onLockVault,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = CyanAccent
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_lock_vault")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Import Progress Banner if active
            if (isImporting) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = importProgress,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = CyanAccent,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }

            // Quick Import Buttons (Photos, Videos, Documents)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_add_photos")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Photos",
                            tint = Color(0xFF0A0C10),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Photos",
                            color = Color(0xFF0A0C10),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_add_videos")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Add Videos",
                            tint = PurpleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Videos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            docPickerLauncher.launch(
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_add_documents")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Add Files",
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Main Vault Dashboard Grid
            item {
                Text(
                    text = "Vault Categories",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Documents & Photos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VaultCategoryCard(
                            title = "Documents & Files",
                            count = "$docCount files",
                            icon = Icons.Default.Description,
                            iconColor = Color(0xFFFFD600),
                            onClick = onNavigateToDocuments,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_documents")
                        )
                        VaultCategoryCard(
                            title = "Photos",
                            count = "$photoCount photos",
                            icon = Icons.Default.Image,
                            iconColor = CyanAccent,
                            onClick = onNavigateToPhotos,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_photos")
                        )
                    }

                    // Row 2: Videos & Contacts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VaultCategoryCard(
                            title = "Videos",
                            count = "$videoCount videos",
                            icon = Icons.Default.VideoLibrary,
                            iconColor = PurpleAccent,
                            onClick = onNavigateToVideos,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_videos")
                        )
                        VaultCategoryCard(
                            title = "Private Contacts",
                            count = "${contacts.size} contacts",
                            icon = Icons.Default.Contacts,
                            iconColor = GreenAccent,
                            onClick = onNavigateToContacts,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_contacts")
                        )
                    }

                    // Row 3: Albums & Favorites
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VaultCategoryCard(
                            title = "Media Albums",
                            count = "${albums.size} albums",
                            icon = Icons.Default.Folder,
                            iconColor = OrangeAccent,
                            onClick = onNavigateToAlbums,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_albums")
                        )
                        VaultCategoryCard(
                            title = "Favorites",
                            count = "$favoriteCount starred",
                            icon = Icons.Default.Star,
                            iconColor = Color(0xFFFF5252),
                            onClick = onNavigateToFavorites,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_favorites")
                        )
                    }

                    // Row 4: Recently Deleted & Security
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VaultCategoryCard(
                            title = "Recently Deleted",
                            count = "$totalTrashCount items",
                            icon = Icons.Default.DeleteSweep,
                            iconColor = Color(0xFFFF5252),
                            onClick = onNavigateToTrash,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_trash")
                        )
                        VaultCategoryCard(
                            title = "Security & Stats",
                            count = "Encrypted",
                            icon = Icons.Default.Security,
                            iconColor = CyanAccent,
                            onClick = onNavigateToSecurity,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_security")
                        )
                    }
                }
            }

            // Storage Breakdown Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vault Storage Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = viewModel.formatBytes(storageBreakdown.totalVaultBytes),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CyanAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StorageStat(
                                label = "Files/Docs",
                                size = viewModel.formatBytes(storageBreakdown.documentsBytes),
                                dotColor = Color(0xFFFFD600)
                            )
                            StorageStat(
                                label = "Photos",
                                size = viewModel.formatBytes(storageBreakdown.photosBytes),
                                dotColor = CyanAccent
                            )
                            StorageStat(
                                label = "Videos",
                                size = viewModel.formatBytes(storageBreakdown.videosBytes),
                                dotColor = PurpleAccent
                            )
                            StorageStat(
                                label = "Free Space",
                                size = viewModel.formatBytes(storageBreakdown.deviceAvailableBytes),
                                dotColor = GreenAccent
                            )
                        }
                    }
                }
            }

            // Recent Private Media Preview
            if (allMedia.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Private Files",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allMedia.take(8), key = { it.id }) { media ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onNavigateToMediaViewer(media.id) }
                            ) {
                                AsyncImage(
                                    model = File(media.thumbnailPath ?: media.filePath),
                                    contentDescription = media.fileName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (media.mediaType == "VIDEO") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun VaultCategoryCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = count,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StorageStat(
    label: String,
    size: String,
    dotColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = size,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
