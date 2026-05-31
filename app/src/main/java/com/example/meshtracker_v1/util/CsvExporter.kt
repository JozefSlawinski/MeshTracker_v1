package com.example.meshtracker_v1.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.repository.PacketStatsRepository
import com.example.meshtracker_v1.repository.PositionHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: PositionHistoryRepository,
    private val statsRepository: PacketStatsRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    suspend fun export(
        nodes: Map<String, MeshNodeInfo>,
        filterNodeId: String? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val history = historyRepository.history.value
            val stats = statsRepository.stats.value
            val fileName = "meshtracker_export_${fileNameFormat.format(Date())}.csv"

            val targetNodes = if (filterNodeId != null) {
                nodes.filterKeys { it == filterNodeId }
            } else {
                nodes
            }

            val lines = buildCsvLines(targetNodes, history, stats, filterNodeId)

            if (lines.isEmpty()) {
                throw IllegalStateException("empty")
            }

            val csvContent = buildString {
                appendLine(CSV_HEADER)
                lines.forEach { appendLine(it) }
            }

            writeCsvFile(fileName, csvContent)
        }
    }

    private fun buildCsvLines(
        nodes: Map<String, MeshNodeInfo>,
        history: Map<String, List<com.example.meshtracker_v1.model.TimedPosition>>,
        stats: Map<String, com.example.meshtracker_v1.model.PacketStats>,
        filterNodeId: String?
    ): List<String> {
        val lines = mutableListOf<String>()
        val allNodeIds = (nodes.keys + history.keys).toSet()
            .filter { filterNodeId == null || it == filterNodeId }

        for (nodeId in allNodeIds) {
            val node = nodes[nodeId]
            val nodeHistory = history[nodeId]
            val roleName = node?.user?.role?.let { roleLabel(it) } ?: ""
            val displayName = node?.getDisplayName() ?: nodeId

            if (!nodeHistory.isNullOrEmpty()) {
                nodeHistory.forEachIndexed { index, pos ->
                    val deltaT = if (index > 0) {
                        (pos.timestampSeconds - nodeHistory[index - 1].timestampSeconds).toString()
                    } else ""

                    lines.add(buildRow(
                        timestampUnix = pos.timestampSeconds,
                        nodeId = nodeId,
                        nodeName = displayName,
                        role = roleName,
                        lat = pos.latitude,
                        lng = pos.longitude,
                        altitudeM = if (pos.altitude != 0) pos.altitude.toString() else "",
                        snrDb = node?.snr?.let { if (it != Float.MAX_VALUE) String.format(Locale.US, "%.1f", it) else "" } ?: "",
                        rssiDbm = node?.rssi?.let { if (it != Int.MAX_VALUE) it.toString() else "" } ?: "",
                        hopsAway = node?.hopsAway?.toString() ?: "",
                        deltaTSeconds = deltaT,
                        batteryPct = node?.batteryLevel?.let { if (it > 0) it.toString() else "" } ?: "",
                        speedMs = node?.position?.groundSpeed?.let { if (it > 0) it.toString() else "" } ?: "",
                        headingDeg = node?.position?.groundTrack?.let { if (it > 0) it.toString() else "" } ?: "",
                        satellites = node?.position?.satellitesInView?.let { if (it > 0) it.toString() else "" } ?: ""
                    ))
                }
            } else if (node != null) {
                lines.add(buildRow(
                    timestampUnix = node.lastHeard,
                    nodeId = nodeId,
                    nodeName = displayName,
                    role = roleName,
                    lat = node.position?.latitude,
                    lng = node.position?.longitude,
                    altitudeM = node.position?.altitude?.let { if (it != 0) it.toString() else "" } ?: "",
                    snrDb = node.snr.let { if (it != Float.MAX_VALUE) String.format(Locale.US, "%.1f", it) else "" },
                    rssiDbm = node.rssi.let { if (it != Int.MAX_VALUE) it.toString() else "" },
                    hopsAway = node.hopsAway.toString(),
                    deltaTSeconds = "",
                    batteryPct = node.batteryLevel.let { if (it > 0) it.toString() else "" },
                    speedMs = node.position?.groundSpeed?.let { if (it > 0) it.toString() else "" } ?: "",
                    headingDeg = node.position?.groundTrack?.let { if (it > 0) it.toString() else "" } ?: "",
                    satellites = node.position?.satellitesInView?.let { if (it > 0) it.toString() else "" } ?: ""
                ))
            }
        }
        return lines
    }

    private fun buildRow(
        timestampUnix: Int,
        nodeId: String,
        nodeName: String,
        role: String,
        lat: Double?,
        lng: Double?,
        altitudeM: String,
        snrDb: String,
        rssiDbm: String,
        hopsAway: String,
        deltaTSeconds: String,
        batteryPct: String,
        speedMs: String,
        headingDeg: String,
        satellites: String
    ): String {
        val readable = if (timestampUnix > 0) dateFormat.format(Date(timestampUnix.toLong() * 1000)) else ""
        val latStr = lat?.let { String.format(Locale.US, "%.6f", it) } ?: ""
        val lngStr = lng?.let { String.format(Locale.US, "%.6f", it) } ?: ""
        return listOf(
            timestampUnix.toString(),
            "\"$readable\"",
            "\"$nodeId\"",
            "\"$nodeName\"",
            "\"$role\"",
            latStr, lngStr, altitudeM,
            snrDb, rssiDbm, hopsAway, deltaTSeconds,
            batteryPct, speedMs, headingDeg, satellites
        ).joinToString(",")
    }

    private fun writeCsvFile(fileName: String, content: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(content) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileName)
            file.writeText(content, Charsets.UTF_8)
            Uri.fromFile(file)
        }
    }

    private fun roleLabel(role: Int): String = when (role) {
        0 -> "CLIENT"; 1 -> "CLIENT_MUTE"; 2 -> "ROUTER"; 3 -> "ROUTER_CLIENT"
        4 -> "REPEATER"; 5 -> "TRACKER"; 6 -> "SENSOR"; 7 -> "TAK"
        else -> "UNKNOWN"
    }

    companion object {
        private const val CSV_HEADER =
            "timestamp_unix,timestamp_readable,node_id,node_name,role," +
            "lat,lng,altitude_m,snr_db,rssi_dbm,hops_away,delta_t_s," +
            "battery_pct,speed_ms,heading_deg,satellites"
    }
}
