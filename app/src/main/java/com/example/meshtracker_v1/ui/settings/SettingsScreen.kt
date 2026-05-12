package com.example.meshtracker_v1.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val refreshInterval by viewModel.refreshInterval.collectAsState()
    val onlineThreshold by viewModel.onlineThreshold.collectAsState()
    val defaultOnlineFilter by viewModel.defaultOnlineFilter.collectAsState()
    val defaultGpsFilter by viewModel.defaultGpsFilter.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val historyMaxPoints by viewModel.historyMaxPoints.collectAsState()
    val historyMinDistanceM by viewModel.historyMinDistanceM.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ---------------------------------------------------------------- Połączenie
        SettingsSectionHeader("Połączenie")

        SettingsDropdown(
            label = "Interwał odświeżania",
            selected = refreshInterval,
            options = listOf(5, 10, 30, 60),
            optionLabel = { "${it}s" },
            onSelect = viewModel::setRefreshInterval
        )

        SettingsDropdown(
            label = "Próg \"online\"",
            selected = onlineThreshold,
            options = listOf(5, 10, 30),
            optionLabel = { "${it} min" },
            onSelect = viewModel::setOnlineThreshold
        )

        SettingsDivider()

        // ---------------------------------------------------------------- Filtry domyślne
        SettingsSectionHeader("Filtry domyślne")

        SettingsSwitch(
            label = "Domyślnie: tylko online",
            checked = defaultOnlineFilter,
            onCheckedChange = viewModel::setDefaultOnlineFilter
        )

        SettingsSwitch(
            label = "Domyślnie: tylko z GPS",
            checked = defaultGpsFilter,
            onCheckedChange = viewModel::setDefaultGpsFilter
        )

        SettingsDivider()

        // ---------------------------------------------------------------- Mapa
        SettingsSectionHeader("Mapa")

        Text(
            text = "Typ mapy",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        val mapTypeLabels = listOf("Normalny", "Satelita", "Teren", "Hybrydowy")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            mapTypeLabels.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = mapType == index,
                    onClick = { viewModel.setMapType(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, mapTypeLabels.size),
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        SettingsDivider()

        // ---------------------------------------------------------------- Historia pozycji
        SettingsSectionHeader("Historia pozycji")

        SettingsDropdown(
            label = "Max punktów historii",
            selected = historyMaxPoints,
            options = listOf(10, 50, 100, 200),
            optionLabel = { "$it pkt" },
            onSelect = viewModel::setHistoryMaxPoints
        )

        SettingsDropdown(
            label = "Min. odległość nowego punktu",
            selected = historyMinDistanceM,
            options = listOf(10, 20, 50, 100),
            optionLabel = { "${it}m" },
            onSelect = viewModel::setHistoryMinDistanceM
        )

        SettingsDivider()

        // ---------------------------------------------------------------- Reset
        Button(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Przywróć domyślne")
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Przywróć domyślne?") },
            text = { Text("Wszystkie ustawienia zostaną zresetowane do wartości domyślnych.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToDefaults()
                    showResetDialog = false
                }) { Text("Resetuj") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Anuluj") }
            }
        )
    }
}

// ---------------------------------------------------------------- helpers

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
