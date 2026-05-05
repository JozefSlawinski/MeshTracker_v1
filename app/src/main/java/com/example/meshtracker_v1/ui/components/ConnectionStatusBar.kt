package com.example.meshtracker_v1.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meshtracker_v1.ui.map.MapViewModel

@Composable
fun ConnectionStatusBar(
    connectionState: MapViewModel.ConnectionState,
    nodeCount: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (connectionState) {
        is MapViewModel.ConnectionState.Connected    -> MaterialTheme.colorScheme.primary
        is MapViewModel.ConnectionState.Connecting   -> MaterialTheme.colorScheme.secondary
        is MapViewModel.ConnectionState.Reconnecting -> MaterialTheme.colorScheme.secondary
        is MapViewModel.ConnectionState.Disconnected -> MaterialTheme.colorScheme.error
        is MapViewModel.ConnectionState.MeshtasticNotInstalled -> MaterialTheme.colorScheme.error
    }

    val statusText = when (connectionState) {
        is MapViewModel.ConnectionState.Connected ->
            "Połączono z Meshtastic"
        is MapViewModel.ConnectionState.Connecting ->
            "Łączenie z Meshtastic..."
        is MapViewModel.ConnectionState.Reconnecting ->
            "Ponowne łączenie za ${connectionState.retryInSeconds}s..."
        is MapViewModel.ConnectionState.Disconnected ->
            connectionState.reason
        is MapViewModel.ConnectionState.MeshtasticNotInstalled ->
            "Meshtastic nie jest zainstalowane"
    }

    val showSpinner = connectionState is MapViewModel.ConnectionState.Connecting ||
            connectionState is MapViewModel.ConnectionState.Reconnecting

    val showRetry = connectionState is MapViewModel.ConnectionState.Disconnected ||
            connectionState is MapViewModel.ConnectionState.MeshtasticNotInstalled

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = statusColor.copy(alpha = 0.12f),
        contentColor = statusColor
    ) {
        AnimatedContent(
            targetState = statusText,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "connection_status"
        ) { text ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showSpinner) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 2.dp,
                            color = statusColor
                        )
                    } else {
                        Surface(
                            color = statusColor,
                            shape = CircleShape,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                when {
                    showRetry -> {
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Ponów",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    connectionState is MapViewModel.ConnectionState.Connected -> {
                        Text(
                            text = "$nodeCount węzł${
                                when {
                                    nodeCount == 1 -> ""
                                    nodeCount in 2..4 -> "y"
                                    else -> "ów"
                                }
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
