package com.reader.feature.bookshelf.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reader.core.model.BookFormat
import com.reader.feature.bookshelf.model.BookshelfViewMode
import com.reader.feature.bookshelf.model.ImportCandidate
import com.reader.feature.bookshelf.mvi.BookshelfIntent
import com.reader.feature.bookshelf.viewmodel.BookshelfViewModel

/**
 * 书架主容器页面组件 (BookshelfScreen)。
 * 聚合 MVI 状态机、顶部状态栏、分类导航、网格/列表自适应排版、
 * SAF 文件挑选与本地导入弹窗。
 *
 * @param onOpenBook 点击书籍打开阅读器回调
 * @param modifier 外部修饰符
 * @param viewModel 书架业务逻辑 ViewModel
 */
@Composable
fun BookshelfScreen(
    onOpenBook: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookshelfViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 监听全局提示消息
    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.onIntent(BookshelfIntent.DismissMessage)
        }
    }

    // SAF (Storage Access Framework) 多选文档选择器
    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val candidates = mutableListOf<ImportCandidate>()
            uris.forEach { uri ->
                // 获取持久化读取权限 (纯离线合规)
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                resolveUriToCandidate(context, uri)?.let { candidate ->
                    candidates.add(candidate)
                }
            }

            if (candidates.isNotEmpty()) {
                viewModel.onIntent(BookshelfIntent.AddCandidates(candidates))
                viewModel.onIntent(BookshelfIntent.OpenImportDialog)
                viewModel.onIntent(BookshelfIntent.StartImport)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BookshelfTopBar(
                totalCount = uiState.totalCount,
                viewMode = uiState.viewMode,
                sortOrder = uiState.sortOrder,
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                isBatchMode = uiState.isBatchMode,
                selectedCount = uiState.selectedBookIds.size,
                onToggleSearch = { viewModel.onIntent(BookshelfIntent.ToggleSearch(it)) },
                onSearchQueryChange = { viewModel.onIntent(BookshelfIntent.Search(it)) },
                onClearSearch = { viewModel.onIntent(BookshelfIntent.ClearSearch) },
                onSwitchViewMode = { viewModel.onIntent(BookshelfIntent.SwitchViewMode(it)) },
                onChangeSortOrder = { viewModel.onIntent(BookshelfIntent.ChangeSortOrder(it)) },
                onOpenImportDialog = { viewModel.onIntent(BookshelfIntent.OpenImportDialog) },
                onToggleBatchMode = { viewModel.onIntent(BookshelfIntent.SetBatchMode(it)) },
                onSelectAllBooks = { viewModel.onIntent(BookshelfIntent.SelectAllBooks) },
                onBatchDelete = { viewModel.onIntent(BookshelfIntent.BatchDelete) }
            )
        },
        floatingActionButton = {
            // 非批量管理且非空状态时展示快捷导入 FAB
            if (!uiState.isBatchMode && !uiState.isEmpty) {
                ExtendedFloatingActionButton(
                    onClick = {
                        safPickerLauncher.launch(
                            arrayOf(
                                "text/plain",
                                "application/epub+zip",
                                "application/x-mobipocket-ebook",
                                "application/pdf",
                                "*/*"
                            )
                        )
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "导入书籍") },
                    text = { Text("导入书籍", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 分类筛选 Tab 栏
            if (!uiState.isSearchActive && !uiState.isEmpty) {
                BookshelfCategoryTabs(
                    selectedCategory = uiState.selectedCategory,
                    categoryCounts = uiState.categoryCounts,
                    onCategorySelected = { viewModel.onIntent(BookshelfIntent.SelectCategory(it)) }
                )
            }

            // 主展示区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    // 加载中
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // 空书架视图
                    uiState.isEmpty -> {
                        EmptyBookshelfView(
                            onImportClick = {
                                safPickerLauncher.launch(
                                    arrayOf(
                                        "text/plain",
                                        "application/epub+zip",
                                        "application/x-mobipocket-ebook",
                                        "application/pdf",
                                        "*/*"
                                    )
                                )
                            },
                            onScanDownloadClick = {
                                viewModel.onIntent(BookshelfIntent.OpenImportDialog)
                                viewModel.onIntent(BookshelfIntent.ScanDirectory("/storage/emulated/0/Download"))
                            }
                        )
                    }

                    // 搜索结果为空
                    uiState.isSearchEmpty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未找到与 \"${uiState.searchQuery}\" 相关的书籍",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // 网格视图模式
                    uiState.viewMode == BookshelfViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 108.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.displayedBooks,
                                key = { it.id }
                            ) { item ->
                                BookGridItem(
                                    item = item,
                                    onClick = { onOpenBook(item.id) },
                                    onLongClick = { /* 内部自动弹出上下文菜单 */ },
                                    onToggleFavorite = { viewModel.onIntent(BookshelfIntent.ToggleFavorite(item.id)) },
                                    onTogglePin = { viewModel.onIntent(BookshelfIntent.TogglePin(item.id)) },
                                    onDelete = { viewModel.onIntent(BookshelfIntent.RequestDelete(item)) },
                                    isBatchMode = uiState.isBatchMode,
                                    isSelected = uiState.selectedBookIds.contains(item.id),
                                    onToggleSelect = { viewModel.onIntent(BookshelfIntent.ToggleBatchSelect(item.id)) }
                                )
                            }
                        }
                    }

                    // 列表视图模式
                    uiState.viewMode == BookshelfViewMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.displayedBooks,
                                key = { it.id }
                            ) { item ->
                                BookListItem(
                                    item = item,
                                    onClick = { onOpenBook(item.id) },
                                    onLongClick = { /* 内部自动弹出上下文菜单 */ },
                                    onToggleFavorite = { viewModel.onIntent(BookshelfIntent.ToggleFavorite(item.id)) },
                                    onTogglePin = { viewModel.onIntent(BookshelfIntent.TogglePin(item.id)) },
                                    onDelete = { viewModel.onIntent(BookshelfIntent.RequestDelete(item)) },
                                    isBatchMode = uiState.isBatchMode,
                                    isSelected = uiState.selectedBookIds.contains(item.id),
                                    onToggleSelect = { viewModel.onIntent(BookshelfIntent.ToggleBatchSelect(item.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 本地导入对话框
    ImportDialog(
        state = uiState.importDialogState,
        onDismiss = { viewModel.onIntent(BookshelfIntent.DismissImportDialog) },
        onLaunchSafPicker = {
            safPickerLauncher.launch(
                arrayOf(
                    "text/plain",
                    "application/epub+zip",
                    "application/x-mobipocket-ebook",
                    "application/pdf",
                    "*/*"
                )
            )
        },
        onScanDirectory = { path -> viewModel.onIntent(BookshelfIntent.ScanDirectory(path)) },
        onToggleCandidate = { uri -> viewModel.onIntent(BookshelfIntent.ToggleCandidate(uri)) },
        onSelectAllCandidates = { selectAll -> viewModel.onIntent(BookshelfIntent.SetAllCandidatesSelected(selectAll)) },
        onStartImport = { viewModel.onIntent(BookshelfIntent.StartImport) }
    )

    // 删除单本书籍二次确认对话框
    uiState.bookToDelete?.let { book ->
        DeleteConfirmDialog(
            book = book,
            onConfirm = { viewModel.onIntent(BookshelfIntent.ConfirmDelete(book.id)) },
            onDismiss = { viewModel.onIntent(BookshelfIntent.DismissDeleteDialog) }
        )
    }
}

/**
 * SAF URI 转换为待导入候选项辅助方法。
 */
private fun resolveUriToCandidate(context: Context, uri: Uri): ImportCandidate? {
    var fileName = "未命名电子书"
    var fileSize = 0L

    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    val resolvedName = cursor.getString(nameIndex)
                    if (!resolvedName.isNullOrBlank()) fileName = resolvedName
                }
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }
    }

    val format = BookFormat.fromFileName(fileName) ?: BookFormat.TXT
    return ImportCandidate(
        uriString = uri.toString(),
        fileName = fileName,
        fileSize = fileSize,
        format = format,
        isSelected = true
    )
}
