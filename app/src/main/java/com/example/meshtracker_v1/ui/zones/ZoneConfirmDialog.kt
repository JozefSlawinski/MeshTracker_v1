package com.example.meshtracker_v1.ui.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.model.Zone

/**
 * Dialog potwierdzenia nowej strefy — wyświetlany po zamknięciu wielokąta.
 * Pozwala ustawić nazwę, kolor i listę monitorowanych węzłów.
 *
 * @param availableNodes lista węzłów do wyboru (z których użytkownik zaznacza obserwowane)
 * @param onConfirm     callback z danymi zatwierdzonej strefy
 * @param onDismiss     callback anulowania (usuwa rysowany wielokąt)
 */
@Composable
fun ZoneConfirmDialog(
    availableNodes: List<MeshNodeInfo>,
    onConfirm: (name: String, colorArgb: Int, watchedNodeIds: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Zone.PRESET_COLORS.first()) }
    // Lista zaznaczonych węzłów (mutowalny snapshot — śledzi checkboxy)
    val selectedNodeIds = remember { mutableListOf<String>().toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa strefa") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---- Nazwa ----
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa strefy") },
                    placeholder = { Text("np. Parking, Dom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- Kolor ----
                Text("Kolor strefy", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Zone.PRESET_COLORS.forEach { colorArgb ->
                        val isSelected = colorArgb == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb))
                                .then(
                                    if (isSelected)
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    else Modifier
                                )
                                .clickable { selectedColor = colorArgb }
                        )
                    }
                }

                // ---- Węzły ----
                HorizontalDivider()
                Text("Monitorowane węzły", style = MaterialTheme.typography.labelMedium)
                if (availableNodes.isEmpty()) {
                    Text(
                        text = "Brak widocznych węzłów.\nMożesz dodać je później w szczegółach strefy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availableNodes.forEach { node ->
                        val nodeId = node.getId()
                        val isChecked = nodeId in selectedNodeIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedNodeIds.remove(nodeId)
                                    else selectedNodeIds.add(nodeId)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedNodeIds.add(nodeId)
                                    else selectedNodeIds.remove(nodeId)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    node.getDisplayName(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    nodeId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.trim().ifEmpty { "Strefa" },
                        selectedColor,
                        selectedNodeIds.toList()
                    )
                }
            ) { Text("Dodaj") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}
