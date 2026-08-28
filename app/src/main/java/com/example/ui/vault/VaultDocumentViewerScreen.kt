package com.example.ui.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.VaultDocumentEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PurpleAccent
import com.example.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDocumentViewerScreen(
    viewModel: VaultViewModel,
    documentId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allActiveDocs by viewModel.allActiveDocuments.collectAsState()
    val doc = allActiveDocs.find { it.id == documentId }

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null && doc != null) {
            viewModel.exportDocument(doc, uri)
        }
    }

    if (doc == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Document not found", color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = doc.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${viewModel.formatBytes(doc.fileSize)} • ${doc.folderName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    IconButton(
                        onClick = {
                            viewModel.toggleDocumentFavorite(doc.id, !doc.isFavorite)
                        }
                    ) {
                        Icon(
                            imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (doc.isFavorite) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { openWithExternalApp(context, doc) }) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open With",
                            tint = CyanAccent
                        )
                    }

                    IconButton(onClick = { exportLauncher.launch(doc.fileName) }) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Export"
                        )
                    }

                    IconButton(onClick = { showDetailsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info"
                        )
                    }

                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (doc.category.uppercase()) {
                "PDF" -> PdfViewer(doc = doc)
                "TEXT" -> TextViewer(doc = doc)
                "IMAGE" -> ImageViewer(doc = doc)
                "AUDIO" -> AudioPlayerViewer(doc = doc)
                else -> UnsupportedDocViewer(
                    doc = doc,
                    onOpenWith = { openWithExternalApp(context, doc) },
                    onExport = { exportLauncher.launch(doc.fileName) },
                    formatBytes = { viewModel.formatBytes(it) }
                )
            }
        }
    }

    // Details Dialog
    if (showDetailsDialog) {
        val dateFormatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(doc.createdTimestamp))
        val modFormatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(doc.modifiedTimestamp))

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Document Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File Name: ${doc.fileName}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Folder: ${doc.folderName}", fontSize = 13.sp)
                    Text("Category: ${doc.category}", fontSize = 13.sp)
                    Text("File Extension: .${doc.fileExtension}", fontSize = 13.sp)
                    Text("MIME Type: ${doc.mimeType}", fontSize = 13.sp)
                    Text("File Size: ${viewModel.formatBytes(doc.fileSize)}", fontSize = 13.sp)
                    if (doc.pageCount > 0) {
                        Text("Page Count: ${doc.pageCount} pages", fontSize = 13.sp)
                    }
                    Text("Imported: $dateFormatted", fontSize = 13.sp)
                    Text("Last Modified: $modFormatted", fontSize = 13.sp)
                    Text("Storage: Application-Private Sandbox (Encrypted & Offline)", fontSize = 12.sp, color = CyanAccent)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Close", color = Color(0xFF0A0C10))
                }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Move to Recently Deleted?", fontWeight = FontWeight.Bold) },
            text = { Text("This document will be moved to Recently Deleted. You can restore it within 30 days.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc.id)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PdfViewer(doc: VaultDocumentEntity) {
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    // Load PDF
    LaunchedEffect(doc.filePath) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(doc.filePath)
                if (file.exists()) {
                    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(descriptor)
                    pfd = descriptor
                    pdfRenderer = renderer
                    totalPages = renderer.pageCount
                    loadPage(renderer, 0) { bmp ->
                        currentBitmap = bmp
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun goToPage(index: Int) {
        if (index in 0 until totalPages && pdfRenderer != null) {
            isLoading = true
            currentPageIndex = index
            scale = 1f
            offset = Offset.Zero
            loadPage(pdfRenderer!!, index) { bmp ->
                currentBitmap = bmp
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // PDF Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E222B))
                .transformable(state = transformState),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = CyanAccent)
            } else if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Unable to render PDF page", color = Color.White)
            }
        }

        // PDF Control Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { goToPage(currentPageIndex - 1) },
                    enabled = currentPageIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Previous Page",
                        tint = if (currentPageIndex > 0) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                Text(
                    text = "Page ${currentPageIndex + 1} of $totalPages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                IconButton(
                    onClick = { goToPage(currentPageIndex + 1) },
                    enabled = currentPageIndex < totalPages - 1
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Next Page",
                        tint = if (currentPageIndex < totalPages - 1) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

private fun loadPage(renderer: PdfRenderer, pageIndex: Int, onLoaded: (Bitmap) -> Unit) {
    try {
        val page = renderer.openPage(pageIndex)
        val densityMultiplier = 2 // Render at 2x resolution for high clarity
        val bitmap = Bitmap.createBitmap(
            page.width * densityMultiplier,
            page.height * densityMultiplier,
            Bitmap.Config.ARGB_8888
        )
        // White canvas background for crisp document rendering
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        onLoaded(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun TextViewer(doc: VaultDocumentEntity) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var content by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isWordWrap by remember { mutableStateOf(true) }
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(doc.filePath) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(doc.filePath)
                if (file.exists()) {
                    val text = file.readText()
                    content = text
                    lines = text.lines()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                content = "Error loading text file: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${lines.size} lines • ${doc.fileExtension.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search In File",
                            tint = if (showSearch) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { isWordWrap = !isWordWrap }) {
                        Icon(
                            imageVector = Icons.Default.WrapText,
                            contentDescription = "Toggle Word Wrap",
                            tint = if (isWordWrap) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(content))
                            Toast.makeText(context, "Copied file content to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Text"
                        )
                    }
                }
            }
        }

        // In-file search bar
        if (showSearch) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Find in document...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSearch = false; searchText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else {
            val scrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1218))
                    .verticalScroll(scrollState)
                    .then(if (!isWordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    lines.forEachIndexed { index, line ->
                        Row {
                            Text(
                                text = "${index + 1}".padStart(4, ' ') + "  ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF5C687E),
                                modifier = Modifier.width(48.dp)
                            )

                            if (searchText.isNotBlank() && line.contains(searchText, ignoreCase = true)) {
                                val highlighted = buildAnnotatedString {
                                    var startIndex = 0
                                    val lowerLine = line.lowercase()
                                    val lowerQuery = searchText.lowercase()
                                    while (startIndex < line.length) {
                                        val matchIndex = lowerLine.indexOf(lowerQuery, startIndex)
                                        if (matchIndex == -1) {
                                            append(line.substring(startIndex))
                                            break
                                        } else {
                                            append(line.substring(startIndex, matchIndex))
                                            withStyle(SpanStyle(background = CyanAccent, color = Color.Black)) {
                                                append(line.substring(matchIndex, matchIndex + searchText.length))
                                            }
                                            startIndex = matchIndex + searchText.length
                                        }
                                    }
                                }
                                Text(
                                    text = highlighted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE0E6ED)
                                )
                            } else {
                                Text(
                                    text = line,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE0E6ED)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageViewer(doc: VaultDocumentEntity) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .transformable(state = transformState),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(doc.filePath),
            contentDescription = doc.fileName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun AudioPlayerViewer(doc: VaultDocumentEntity) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    LaunchedEffect(doc.filePath) {
        withContext(Dispatchers.IO) {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(doc.filePath)
                    prepare()
                }
                mediaPlayer = player
                duration = player.duration
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPosition = player.currentPosition
                }
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4081).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFFF4081),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = doc.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${doc.fileExtension.uppercase()} Audio • ${doc.folderName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { newValue ->
                        currentPosition = newValue.toInt()
                        mediaPlayer?.seekTo(newValue.toInt())
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF4081),
                        activeTrackColor = Color(0xFFFF4081)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatAudioTime(currentPosition), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatAudioTime(duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (currentPosition - 5000).coerceAtLeast(0)
                            currentPosition = newPos
                            mediaPlayer?.seekTo(newPos)
                        }
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind 5s", modifier = Modifier.size(28.dp))
                    }

                    FloatingActionButton(
                        onClick = {
                            mediaPlayer?.let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.start()
                                    isPlaying = true
                                }
                            }
                        },
                        containerColor = Color(0xFFFF4081),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newPos = (currentPosition + 5000).coerceAtMost(duration)
                            currentPosition = newPos
                            mediaPlayer?.seekTo(newPos)
                        }
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "Fast Forward 5s", modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UnsupportedDocViewer(
    doc: VaultDocumentEntity,
    onOpenWith: () -> Unit,
    onExport: () -> Unit,
    formatBytes: (Long) -> String
) {
    val categoryColor = getCategoryColor(doc.category)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(doc.category),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = doc.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = categoryColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${doc.fileExtension.uppercase()} • ${formatBytes(doc.fileSize)} • ${doc.folderName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "This file format cannot be rendered inside the vault directly. Use 'Open With Other App' to securely view it in your installed viewer (e.g. Word, Excel, PowerPoint, Archiver), or export it back to your device storage.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onOpenWith,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF0A0C10)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open With Other App",
                        color = Color(0xFF0A0C10),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onExport,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export to Device",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun formatAudioTime(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
