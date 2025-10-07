package dev.pranav.reef

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.transition.platform.MaterialSharedAxis
import dev.pranav.reef.data.Routine
import dev.pranav.reef.data.RoutineSchedule
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.util.RoutineManager
import dev.pranav.reef.util.applyDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class CreateRoutineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()

        window.enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        window.returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        super.onCreate(savedInstanceState)

        val routineId = intent.getStringExtra("routine_id")

        setContent {
            ReefTheme {
                CreateRoutineScreen(
                    routineId = routineId,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onSaveComplete = { finishAfterTransition() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateRoutineScreen(
    routineId: String?,
    onBackPressed: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentRoutine by remember { mutableStateOf<Routine?>(null) }
    var routineName by remember { mutableStateOf("") }
    var scheduleType by remember { mutableStateOf(RoutineSchedule.ScheduleType.WEEKLY) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var selectedEndTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var appLimits by remember { mutableStateOf(listOf<Routine.AppLimit>()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAppSelector by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routineId) {
        routineId?.let {
            currentRoutine = RoutineManager.getRoutines().find { it.id == routineId }
            currentRoutine?.let { routine ->
                routineName = routine.name
                scheduleType = routine.schedule.type
                routine.schedule.time?.let { selectedTime = it }
                routine.schedule.endTime?.let { selectedEndTime = it }
                selectedDays = routine.schedule.daysOfWeek
                appLimits = routine.limits
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        if (currentRoutine != null) "Edit Routine"
                        else stringResource(R.string.create_routine)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = routineName,
                onValueChange = { routineName = it },
                label = { Text(stringResource(R.string.routine_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Text(
                text = stringResource(R.string.schedule),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ToggleButton(
                    checked = scheduleType == RoutineSchedule.ScheduleType.MANUAL,
                    onCheckedChange = { scheduleType = RoutineSchedule.ScheduleType.MANUAL },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.manual))
                }

                ToggleButton(
                    checked = scheduleType == RoutineSchedule.ScheduleType.DAILY,
                    onCheckedChange = { scheduleType = RoutineSchedule.ScheduleType.DAILY },
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.daily))
                }

                ToggleButton(
                    checked = scheduleType == RoutineSchedule.ScheduleType.WEEKLY,
                    onCheckedChange = { scheduleType = RoutineSchedule.ScheduleType.WEEKLY },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.weekly))
                }
            }

            AnimatedVisibility(visible = scheduleType != RoutineSchedule.ScheduleType.MANUAL) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.start_time),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            FilledTonalButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(formatTime(selectedTime))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.end_time),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            FilledTonalButton(
                                onClick = { showEndTimePicker = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(formatTime(selectedEndTime))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = scheduleType == RoutineSchedule.ScheduleType.WEEKLY) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.select_days),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            DayOfWeek.MONDAY to R.string.monday,
                            DayOfWeek.TUESDAY to R.string.tuesday,
                            DayOfWeek.WEDNESDAY to R.string.wednesday,
                            DayOfWeek.THURSDAY to R.string.thursday,
                            DayOfWeek.FRIDAY to R.string.friday,
                            DayOfWeek.SATURDAY to R.string.saturday,
                            DayOfWeek.SUNDAY to R.string.sunday
                        ).forEach { (day, stringRes) ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(day))
                                        selectedDays - day
                                    else selectedDays + day
                                },
                                label = { Text(stringResource(stringRes)) },
                                leadingIcon = if (selectedDays.contains(day)) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_limits),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = { showAppSelector = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_app))
                }
            }

            if (appLimits.isEmpty()) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_app_limits_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    appLimits.forEach { limit ->
                        AppLimitItem(
                            appLimit = limit,
                            onRemove = {
                                appLimits = appLimits.filter { it.packageName != limit.packageName }
                            },
                            context = context
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val result = saveRoutine(
                        context = context,
                        currentRoutine = currentRoutine,
                        name = routineName,
                        scheduleType = scheduleType,
                        selectedTime = selectedTime,
                        selectedEndTime = selectedEndTime,
                        selectedDays = selectedDays,
                        appLimits = appLimits,
                        onError = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                    if (result) onSaveComplete()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    stringResource(R.string.save_routine),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (currentRoutine != null) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.delete_routine),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { selectedTime = it },
            onDismiss = { showTimePicker = false },
            title = "Start Time"
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = selectedEndTime,
            onTimeSelected = { selectedEndTime = it },
            onDismiss = { showEndTimePicker = false },
            title = "End Time"
        )
    }

    if (showAppSelector) {
        AppSelectorDialog(
            onAppSelected = { packageName, appName, limitMinutes ->
                appLimits = appLimits.filter { it.packageName != packageName } +
                        Routine.AppLimit(packageName, limitMinutes)
                showAppSelector = false
            },
            onDismiss = { showAppSelector = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_routine)) },
            text = {
                Text("Are you sure you want to delete '${currentRoutine?.name}'? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentRoutine?.let {
                            RoutineManager.deleteRoutine(it.id, context)
                        }
                        onSaveComplete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppLimitItem(
    appLimit: Routine.AppLimit,
    onRemove: () -> Unit,
    context: android.content.Context
) {
    var appName by remember { mutableStateOf(appLimit.packageName) }
    var appIcon by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(appLimit.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(appLimit.packageName, 0)
                appName = pm.getApplicationLabel(appInfo).toString()
                appIcon = pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
            }
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            appIcon?.let { icon ->
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = stringResource(R.string.app_icon)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatLimitTime(appLimit.limitMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove app limit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    title: String
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorDialog(
    onAppSelected: (packageName: String, appName: String, limitMinutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps else apps.filter {
            it.second.contains(
                searchQuery,
                ignoreCase = true
            )
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val installedApps = context.packageManager.getInstalledApplications(0)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .filter { it.packageName != context.packageName }
                .sortedBy { context.packageManager.getApplicationLabel(it).toString() }
                .map {
                    it.packageName to context.packageManager.getApplicationLabel(it).toString()
                }
            apps = installedApps
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select App",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search apps") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Text(
                    text = if (searchQuery.isBlank()) "No apps found" else "No apps match your search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .height(400.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                ) {
                    items(filteredApps) { (packageName, appName) ->
                        TextButton(
                            onClick = { selectedApp = packageName to appName },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = appName,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedApp != null) {
        val (packageName, appName) = selectedApp!!
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text("Set limit for $appName") },
            text = {
                Column {
                    listOf(
                        "0 minutes" to 0,
                        "15 minutes" to 15,
                        "30 minutes" to 30,
                        "1 hour" to 60,
                        "2 hours" to 120,
                        "3 hours" to 180
                    ).forEach { (label, minutes) ->
                        TextButton(
                            onClick = {
                                onAppSelected(packageName, appName, minutes)
                                selectedApp = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedApp = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTime(time: LocalTime): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return time.format(formatter)
}

private fun formatLimitTime(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun saveRoutine(
    context: android.content.Context,
    currentRoutine: Routine?,
    name: String,
    scheduleType: RoutineSchedule.ScheduleType,
    selectedTime: LocalTime,
    selectedEndTime: LocalTime,
    selectedDays: Set<DayOfWeek>,
    appLimits: List<Routine.AppLimit>,
    onError: (String) -> Unit
): Boolean {
    if (name.trim().isEmpty()) {
        onError("Please enter a routine name")
        return false
    }

    val schedule = RoutineSchedule(
        type = scheduleType,
        timeHour = if (scheduleType != RoutineSchedule.ScheduleType.MANUAL) selectedTime.hour else null,
        timeMinute = if (scheduleType != RoutineSchedule.ScheduleType.MANUAL) selectedTime.minute else null,
        endTimeHour = if (scheduleType != RoutineSchedule.ScheduleType.MANUAL) selectedEndTime.hour else null,
        endTimeMinute = if (scheduleType != RoutineSchedule.ScheduleType.MANUAL) selectedEndTime.minute else null,
        daysOfWeek = if (scheduleType == RoutineSchedule.ScheduleType.WEEKLY) selectedDays else emptySet()
    )

    if (scheduleType == RoutineSchedule.ScheduleType.WEEKLY && schedule.daysOfWeek.isEmpty()) {
        onError("Please select at least one day")
        return false
    }

    val routine = Routine(
        id = currentRoutine?.id ?: UUID.randomUUID().toString(),
        name = name.trim(),
        isEnabled = currentRoutine?.isEnabled ?: true,
        schedule = schedule,
        limits = appLimits
    )

    if (currentRoutine == null) {
        RoutineManager.addRoutine(routine, context)
    } else {
        RoutineManager.updateRoutine(routine, context)
    }

    return true
}
