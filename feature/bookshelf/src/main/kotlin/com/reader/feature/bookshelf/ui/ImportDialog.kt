package com.reader.feature.bookshelf.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reader.feature.bookshelf.model.ImportCandidate
import com.reader.feature.bookshelf.model.ImportCandidateStatus
import com.reader.feature.bookshelf.model.ImportDialogState

/**
 * 本地书籍导入综合对话框。
 * 集成 SAF (Storage Access Framework) 系统选择器与本地多级目录扫描，
 * 具备实时逐本导入状态流、进度反馈与异常捕获。
 */
@Composable
fun ImportDialog(
    state: ImportDialogState,
    onDismiss: () -> Unit,
    onLaunchSafPicker: () -> Unit,
    onScanDirectory: (String) -> Unit,
    onToggleCandidate: (String) -> Unit,
    onSelectAllCandidates: (Boolean) -> Unit,
    onStartImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    var selectedTab by remember { mutableIntStateOf(0) }
    var customPath by remember { mutableStateOf(state.scanPath.ifEmpty { "/storage/emulated/0/Download" }) }

    Dialog(
        onDismissRequest = {
            if (!state.isImporting) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !state.isImporting
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 顶部标题与关闭
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "导入离线电子书",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TXT · EPUB · MOBI · AZW3 · PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!state.isImporting) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onDismiss() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 导入渠道 Tab
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("SAF 系统选择", fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("本地目录扫描", fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab 0: SAF 选择入口
                if (selectedTab == 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "通过 Android 系统存储框架 (SAF) 单选或多选外部存储中的任意电子书文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onLaunchSafPicker,
                                enabled = !state.isImporting,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("打开系统文件管理器选择")
                            }
                        }
                    }
                }

                // Tab 1: 本地目录路径扫描
                if (selectedTab == 1) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customPath,
                            onValueChange = { customPath = it },
                            label = { Text("输入扫描目录路径") },
                            singleLine = true,
                            enabled = !state.isScanning && !state.isImporting,
                            trailingIcon = {
                                Button(
                                    onClick = { onScanDirectory(customPath) },
                                    enabled = !state.isScanning && !state.isImporting && customPath.isNotBlank(),
                                    modifier = Modifier.padding(end = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (state.isScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("扫描")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 常用目录快捷标签
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickPathChip(
                                label = "下载目录",
                                path = "/storage/emulated/0/Download",
                                onSelect = {
                                    customPath = it
                                    onScanDirectory(it)
                                }
                            )
                            QuickPathChip(
                                label = "文档目录",
                                path = "/storage/emulated/0/Documents",
                                onSelect = {
                                    customPath = it
                                    onScanDirectory(it)
                                }
                            )
                        }

                        if (state.scanErrorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.scanErrorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 候选书籍列表与全选条
                if (state.candidates.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "待导入清单 (${state.selectedCount}/${state.totalCount})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "全选",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Checkbox(
                                checked = state.isAllSelected,
                                onCheckedChange = { onSelectAllCandidates(it) },
                                enabled = !state.isImporting
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(state.candidates, key = { it.uriString }) { candidate ->
                            CandidateFileRow(
                                candidate = candidate,
                                onToggle = { onToggleCandidate(candidate.uriString) },
                                enabled = !state.isImporting && candidate.status != ImportCandidateStatus.SKIPPED_ALREADY_EXISTS
                            )
                        }
                    }
                }

                // 导入进度栏
                AnimatedVisibility(visible = state.isImporting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "正在导入: ${state.currentFileName ?: "准备中..."}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                // 完成结果概览
                if (state.isFinished) {
                    Text(
                        text = "导入完成：成功 ${state.successCount} 本" +
                                if (state.failureCount > 0) "，失败 ${state.failureCount} 本" else "" +
                                if (state.skippedCount > 0) "，跳过已存在 ${state.skippedCount} 本" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 底部操作栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !state.isImporting
                    ) {
                        Text(if (state.isFinished) "完成" else "取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onStartImport,
                        enabled = !state.isImporting && state.selectedCount > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (state.isImporting) "正在导入..." else "开始导入 (${state.selectedCount})"
                        )
                    }
                }
            }
        }
    }
}

/**
 * 候选文件列表项行。
 */
@Composable
private fun CandidateFileRow(
    candidate: ImportCandidate,
    onToggle: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onToggle() }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = candidate.isSelected,
            onCheckedChange = { if (enabled) onToggle() },
            enabled = enabled
        )

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = formatBadgeColor(candidate.format),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = candidate.format.extension.uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = candidate.formattedFileSize,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // 状态标识
        when (candidate.status) {
            ImportCandidateStatus.PENDING -> {}
            ImportCandidateStatus.IMPORTING -> {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            ImportCandidateStatus.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已导入",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
            }
            ImportCandidateStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "导入失败",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            ImportCandidateStatus.SKIPPED_ALREADY_EXISTS -> {
                Text(
                    text = "已在书架",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 快捷目录胶囊标签。
 */
@Composable
private fun QuickPathChip(
    label: String,
    path: String,
    onSelect: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onSelect(path) }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
