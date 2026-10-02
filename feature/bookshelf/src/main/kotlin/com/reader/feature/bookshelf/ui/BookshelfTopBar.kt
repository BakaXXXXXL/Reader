package com.reader.feature.bookshelf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.feature.bookshelf.model.BookshelfSortOrder
import com.reader.feature.bookshelf.model.BookshelfViewMode

/**
 * 书架统一顶部导航控制栏。
 * 具备常规模式、实时搜索模式与批量多选模式三种状态呈现。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfTopBar(
    totalCount: Int,
    viewMode: BookshelfViewMode,
    sortOrder: BookshelfSortOrder,
    searchQuery: String,
    isSearchActive: Boolean,
    isBatchMode: Boolean,
    selectedCount: Int,
    onToggleSearch: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSwitchViewMode: (BookshelfViewMode) -> Unit,
    onChangeSortOrder: (BookshelfSortOrder) -> Unit,
    onOpenImportDialog: () -> Unit,
    onToggleBatchMode: (Boolean) -> Unit,
    onSelectAllBooks: () -> Unit,
    onBatchDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    when {
        // 模式 1：批量管理模式
        isBatchMode -> {
            TopAppBar(
                modifier = modifier,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                navigationIcon = {
                    IconButton(onClick = { onToggleBatchMode(false) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "退出批量管理")
                    }
                },
                title = {
                    Text(
                        text = "已选择 $selectedCount 项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSelectAllBooks) {
                        Icon(imageVector = Icons.Default.SelectAll, contentDescription = "全选")
                    }
                    if (selectedCount > 0) {
                        IconButton(onClick = onBatchDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "批量删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }

        // 模式 2：展开搜索模式
        isSearchActive -> {
            TopAppBar(
                modifier = modifier,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        onToggleSearch(false)
                        onClearSearch()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "退出搜索"
                        )
                    }
                },
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("搜索书名或作者...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { /* 实时响应 */ }),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = onClearSearch) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "清除搜索")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        // 模式 3：常规书架主标题栏
        else -> {
            TopAppBar(
                modifier = modifier,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "我的书架",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (totalCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    // 搜索按钮
                    IconButton(onClick = { onToggleSearch(true) }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "搜索书籍")
                    }

                    // 视图切换 (网格 / 列表)
                    IconButton(
                        onClick = {
                            val nextMode = if (viewMode == BookshelfViewMode.GRID) {
                                BookshelfViewMode.LIST
                            } else {
                                BookshelfViewMode.GRID
                            }
                            onSwitchViewMode(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == BookshelfViewMode.GRID) {
                                Icons.AutoMirrored.Filled.ViewList
                            } else {
                                Icons.Default.GridView
                            },
                            contentDescription = "切换视图"
                        )
                    }

                    // 排序下拉菜单
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "排序方式")
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            BookshelfSortOrder.entries.forEach { order ->
                                val isSelected = order == sortOrder
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        sortMenuExpanded = false
                                        onChangeSortOrder(order)
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 更多功能菜单
                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "更多选项")
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("导入本地电子书") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenImportDialog()
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("批量整理") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onToggleBatchMode(true)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.SelectAll, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}
