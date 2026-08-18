package io.github.sufarook.kiln.sample.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Shared UI — this exact composable runs on both Android and iOS. */
@Composable
fun App(store: TaskStore) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            TaskScreen(store)
        }
    }
}

@Composable
private fun TaskScreen(store: TaskStore) {
    val scope = rememberCoroutineScope()

    // observeAll() returns a Flow that re-emits after every write to its table,
    // so the UI stays in sync without any manual refresh.
    val tasks by store.observeTasks().collectAsState(initial = emptyList())
    val tags by store.observeTags().collectAsState(initial = emptyList())
    val links by store.observeTaskTags().collectAsState(initial = emptyList())

    var newTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { store.seedTagsIfEmpty() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kiln — Compose Multiplatform", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tasks and tags are joined through a composite-key junction table.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("New task") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = newTitle.isNotBlank(),
                onClick = {
                    val title = newTitle.trim()
                    newTitle = ""
                    scope.launch { store.addTask(title) }
                }
            ) { Text("Add") }
        }

        Spacer(Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Text("No tasks yet — add one above.", style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                val assigned = links.filter { it.taskId == task.id }.map { it.tagId }.toSet()
                TaskRow(
                    task = task,
                    allTags = tags,
                    assignedTagIds = assigned,
                    onToggleDone = { scope.launch { store.toggleDone(task) } },
                    onDelete = { scope.launch { store.deleteTask(task) } },
                    onToggleTag = { tagId, isAssigned ->
                        scope.launch {
                            if (isAssigned) store.unassign(task.id, tagId)
                            else store.assign(task.id, tagId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    allTags: List<Tag>,
    assignedTagIds: Set<Long>,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onToggleTag: (Long, Boolean) -> Unit
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                allTags.forEach { tag ->
                    val isAssigned = tag.id in assignedTagIds
                    TagChip(
                        label = tag.name,
                        selected = isAssigned,
                        onClick = { onToggleTag(tag.id, isAssigned) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val foreground =
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text("#$label", color = foreground, style = MaterialTheme.typography.labelMedium)
    }
}
