package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.InterceptedCall
import com.example.viewmodel.InterceptorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: InterceptorViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.allCalls.collectAsState()
    val context = LocalContext.current

    var selectedLogForDetail by remember { mutableStateOf<InterceptedCall?>(null) }
    var showSimulatorDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dial Interceptor",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Developer Debugging Tool",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInstructionsDialog = true },
                        modifier = Modifier.testTag("instructions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Setup Instructions"
                        )
                    }
                    IconButton(
                        onClick = { showSimulatorDialog = true },
                        modifier = Modifier.testTag("simulator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Trigger Simulation Dialer"
                        )
                    }
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all captured logs"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            if (logs.isEmpty()) {
                EmptyStateView(
                    onSimulateClick = { showSimulatorDialog = true },
                    onInstructionsClick = { showInstructionsDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        StatisticsRow(logs = logs)
                    }
                    items(logs, key = { it.id }) { log ->
                        LogItemCard(
                            log = log,
                            onClick = { selectedLogForDetail = log },
                            onDelete = { viewModel.deleteCall(log) }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedLogForDetail?.let { log ->
        LogDetailDialog(
            log = log,
            onDismiss = { selectedLogForDetail = null },
            onSaveNotes = { updatedLog ->
                viewModel.updateCall(updatedLog)
                selectedLogForDetail = null
            },
            onDelete = {
                viewModel.deleteCall(log)
                selectedLogForDetail = null
            }
        )
    }

    // Simulator Dialog
    if (showSimulatorDialog) {
        SimulatorDialog(
            onDismiss = { showSimulatorDialog = false },
            onTriggerDial = { code, isCall ->
                val type = if (isCall) "CALL" else "DIAL"
                viewModel.insertCall(code, type, "Simulated via Built-in Developer dialer")
                showSimulatorDialog = false
            }
        )
    }

    // Instructions Dialog
    if (showInstructionsDialog) {
        InstructionsDialog(onDismiss = { showInstructionsDialog = false })
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Logs?") },
            text = { Text("This will permanently delete all captured and simulated USSD dial sequences from local storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showClearConfirmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatisticsRow(logs: List<InterceptedCall>) {
    val ussdCount = logs.count { it.decodedCode.contains("*") || it.decodedCode.contains("#") }
    val stdCount = logs.size - ussdCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("USSD/MMI Codes", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = ussdCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Standard Numbers", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = stdCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LogItemCard(
    log: InterceptedCall,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isUSSD = log.decodedCode.contains("*") || log.decodedCode.contains("#")
    val formattedTime = remember(log.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss • dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            )
            .testTag("log_item_${log.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isUSSD) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header with status indicator, action label tag, and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isUSSD) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUSSD) Icons.Default.Code else Icons.Default.Phone,
                        contentDescription = if (isUSSD) "USSD Code" else "Phone Number",
                        modifier = Modifier.size(18.dp),
                        tint = if (isUSSD) MaterialTheme.colorScheme.onPrimaryContainer 
                               else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Small Tag for Action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (log.actionType == "CALL") Color(0xFFE57373) 
                            else if (log.actionType == "DIAL") Color(0xFF81C784)
                            else Color(0xFF64B5F6)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.actionType,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The USSD/MMI Code - displayed beautifully, taking up full width
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = log.decodedCode,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isUSSD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (log.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal separator
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Actions block below the USSD code
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val clipboardManager = LocalClipboardManager.current
                val localContext = LocalContext.current

                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Copy button
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(log.decodedCode))
                            android.widget.Toast.makeText(localContext, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("copy_button_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.labelMedium)
                    }

                    // Share button
                    TextButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, log.decodedCode)
                            }
                            localContext.startActivity(Intent.createChooser(shareIntent, "Share code"))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("share_button_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share code",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium)
                    }

                    // Dial / Open Dialer button
                    TextButton(
                        onClick = {
                            try {
                                val encodedCode = android.net.Uri.encode(log.decodedCode)
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:$encodedCode")
                                }
                                localContext.startActivity(dialIntent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(localContext, "Error opening dialer", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("dial_button_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Open in dialer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dial", style = MaterialTheme.typography.labelMedium)
                    }

                    // Force Logcat Trigger Button
                    TextButton(
                        onClick = {
                            android.util.Log.i("USSD_CAPTURED", "=================== DIAL INTERCEPTOR (MANUAL ACTION) ===================")
                            android.util.Log.i("USSD_CAPTURED", "[MANUAL LOG] >>> ${log.decodedCode} <<<")
                            android.util.Log.i("USSD_CAPTURED", "Action Type : ${log.actionType}")
                            android.util.Log.i("USSD_CAPTURED", "Timestamp   : ${log.timestamp}")
                            android.util.Log.i("USSD_CAPTURED", "Original URI: ${log.rawUri}")
                            android.util.Log.i("USSD_CAPTURED", "========================================================================")
                            android.widget.Toast.makeText(localContext, "Logged to Logcat!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.testTag("logcat_button_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Log to Logcat manually",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logcat", style = MaterialTheme.typography.labelMedium)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_button_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete log entry",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    onSimulateClick: () -> Unit,
    onInstructionsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NetworkCheck,
                contentDescription = "Listening",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Active Interceptor Listening...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Whenever your phone makes an outgoing dial or triggers a USSD code (*123#, *#06#, etc.), and you select Dial Interceptor as your dial handler, the details will be intercepted, decoded, and logged here automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onInstructionsClick,
            modifier = Modifier.fillMaxWidth().testTag("empty_instructions_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Show Interception Setup Guide")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSimulateClick,
            modifier = Modifier.fillMaxWidth().testTag("empty_simulate_btn")
        ) {
            Icon(imageVector = Icons.Default.BugReport, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Built-in Dialer Simulator")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailDialog(
    log: InterceptedCall,
    onDismiss: () -> Unit,
    onSaveNotes: (InterceptedCall) -> Unit,
    onDelete: () -> Unit
) {
    var notesText by remember { mutableStateOf(log.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Details",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Number / Code Section
                Column {
                    Text("Decoded Code/Number", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.decodedCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        val clipboardManager = LocalClipboardManager.current
                        val localContext = LocalContext.current

                        Row {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(log.decodedCode))
                                    android.widget.Toast.makeText(localContext, "Copied code!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("detail_copy_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, log.decodedCode)
                                    }
                                    localContext.startActivity(Intent.createChooser(shareIntent, "Share code"))
                                },
                                modifier = Modifier.testTag("detail_share_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share code",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val encodedCode = android.net.Uri.encode(log.decodedCode)
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = android.net.Uri.parse("tel:$encodedCode")
                                        }
                                        localContext.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(localContext, "Error opening dialer", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("detail_dial_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = "Open in dialer",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Full Scheme Spec Specific Section
                Column {
                    Text("Original URI Scheme Specific Part", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = log.rawUri,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                // Metadata details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Action Type", style = MaterialTheme.typography.labelSmall)
                        Text(log.actionType, fontWeight = FontWeight.SemiBold)
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Timestamp", style = MaterialTheme.typography.labelSmall)
                        val formatted = remember(log.timestamp) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        }
                        Text(formatted, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                HorizontalDivider()

                // Notes input field
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Developer/Test Notes") },
                    placeholder = { Text("E.g. testing service registration flow, account balance query") },
                    modifier = Modifier.fillMaxWidth().testTag("detail_notes_input"),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSaveNotes(log.copy(notes = notesText)) },
                modifier = Modifier.testTag("detail_save_btn")
            ) {
                Text("Save Notes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        }
    )
}

@Composable
fun SimulatorDialog(
    onDismiss: () -> Unit,
    onTriggerDial: (String, Boolean) -> Unit
) {
    var dialCode by remember { mutableStateOf("") }
    var actionTypeIsCall by remember { mutableStateOf(false) } // Default to DIAL action

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dialer Simulator")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Simulate dialing USSD MMI sequences or traditional numbers to verify the database, filters, and display views.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dialCode,
                    onValueChange = { dialCode = it },
                    label = { Text("Code or Number") },
                    placeholder = { Text("e.g., *123# or 1234567") },
                    modifier = Modifier.fillMaxWidth().testTag("sim_dial_input"),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    )
                )

                // Fast Pre-set Buttons
                Text("Quick presets:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("*123#", "*#06#", "*100*1#")
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = { dialCode = preset },
                            label = { Text(preset, fontFamily = FontFamily.Monospace) }
                        )
                    }
                }

                HorizontalDivider()

                // Choose action type
                Text("Action Type to simulate:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { actionTypeIsCall = false }
                    ) {
                        RadioButton(
                            selected = !actionTypeIsCall,
                            onClick = { actionTypeIsCall = false },
                            modifier = Modifier.testTag("radio_dial")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DIAL (Default)")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { actionTypeIsCall = true }
                    ) {
                        RadioButton(
                            selected = actionTypeIsCall,
                            onClick = { actionTypeIsCall = true },
                            modifier = Modifier.testTag("radio_call")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CALL")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dialCode.isNotBlank()) {
                        onTriggerDial(dialCode, actionTypeIsCall)
                    }
                },
                enabled = dialCode.isNotBlank(),
                modifier = Modifier.testTag("sim_trigger_btn")
            ) {
                Text("Trigger Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun InstructionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.IntegrationInstructions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Interception Guide")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "To test real-world interception with dial actions, your app registers an Intent Filter targeting 'tel' schemas. Please follow these steps:",
                    style = MaterialTheme.typography.bodyMedium
                )

                InstructionStep(
                    step = "1",
                    title = "Trigger Dialing Event",
                    description = "Open your device's built-in Phone app or system dialer interface."
                )

                InstructionStep(
                    step = "2",
                    title = "Type a USSD Code",
                    description = "Type a valid USSD sequence like *123# or a standard phone number, then press Call."
                )

                InstructionStep(
                    step = "3",
                    title = "Select Dial Interceptor",
                    description = "When Android prompts with a 'Complete action using' dialog, select Dial Interceptor. Your application will intercept the raw code, decode its asterisk and hash elements (which are typically hidden or normalized in default log histories), write them to local Room DB, and immediately return back so you don't lose context."
                )

                HorizontalDivider()

                Text(
                    text = "Security Tip: Standard CALL actions might require system-level default phone app permissions on certain Android versions if intercepting standard dials, whereas standard dialer triggers can be captured universally via the standard resolver popup choice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("instructions_close_btn")) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun InstructionStep(
    step: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
