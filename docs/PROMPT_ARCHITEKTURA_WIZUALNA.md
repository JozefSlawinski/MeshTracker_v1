# Prompt do wizualizacji architektury MeshTracker v1

Poniższy prompt możesz wkleić do narzędzi takich jak ChatGPT, Claude, Gemini, draw.io AI, Eraser.io lub Mermaid Live Editor.

---

## PROMPT

```
Create a layered software architecture diagram for an Android application called MeshTracker v1.

The diagram must show 5 clearly separated horizontal layers, stacked top to bottom, with labeled arrows indicating the direction of data flow between them.

---

LAYER 1 — UI LAYER (top)
Color: light blue
Components (show as boxes inside the layer):
- MainActivity → MainScreen (bottom navigation host)
- MapScreen + MapViewModel (Google Maps, node markers, zone polygons, drawing mode)
- NodeListScreen + NodeDetailScreen (node list with filters, node detail view)
- ZoneDetailScreen + ZoneViewModel (geofence zone detail, ENTER/EXIT event log)
- SettingsScreen + SettingsViewModel (app configuration)
- Shared component: ConnectionStatusBar (connection state indicator, shown above all screens)

---

LAYER 2 — DOMAIN / LOGIC LAYER
Color: light green
Components:
- GeofenceChecker (pure JVM, ray-casting point-in-polygon algorithm)
- NodeFilterState (immutable filter criteria data class)
- NodeToMarkerMapper (model → map marker)

---

LAYER 3 — DATA LAYER
Color: light yellow
Components (show as boxes):
- MeshRepository (interface) / MeshServiceRepository (implementation, singleton)
- ZoneRepository (Room database Flow — zones + zone events, 1:N with CASCADE delete)
- PositionHistoryRepository (in-memory StateFlow, max N points per node)
- PacketStatsRepository (in-memory ConcurrentHashMap, packet counter per node)
- AppPreferences (DataStore Preferences — thresholds, intervals, map type)

---

LAYER 4 — SERVICE / IPC LAYER
Color: light orange
Components:
- MeshServiceManager (singleton, Android Service binding via AIDL + Java Reflection)
- ZoneMonitorService (Android Foreground Service, started when ≥1 active zone exists, monitors node positions against zones, fires push notifications on ENTER/EXIT)
- MeshtasticBroadcastReceiver (receives broadcast intents: ACTION_NODE_CHANGE, MESH_CONNECTED, MESH_DISCONNECTED from Meshtastic app)

---

LAYER 5 — EXTERNAL SYSTEM (bottom)
Color: light gray
Components:
- Meshtastic App (com.geeksville.mesh) — external Android app
  - IMeshService AIDL (getNodes, connectionState, getMyId) — bidirectional binding arrow to MeshServiceManager
  - Broadcast Intents (ACTION_NODE_CHANGE, MESH_CONNECTED, MESH_DISCONNECTED) — one-way arrow to MeshtasticBroadcastReceiver

---

PERSISTENCE (show as a sidebar or footnote panel on the right side):
- Room Database (ZoneDatabase): tables Zone + ZoneEvent — connected to ZoneRepository
- DataStore Preferences — connected to AppPreferences
- In-Memory only: PositionHistoryRepository, PacketStatsRepository (note: data lost on app close)

---

DEPENDENCY INJECTION (show as a small inset box or legend):
Hilt/Dagger — SingletonComponent provides:
  MeshServiceManager, MeshRepository → MeshServiceRepository, ZoneDatabase, ZoneDao, AppPreferences, CsvExporter
Hilt ViewModelComponent provides:
  MapViewModel, ZoneViewModel, SettingsViewModel

---

DATA FLOW ARROWS (label each arrow):
1. UI Layer → Domain Layer: "calls (StateFlow observe / function call)"
2. Domain Layer → Data Layer: "reads repositories"
3. Data Layer → Service Layer: "binds / registers"
4. Service Layer → External: "AIDL bindService() / BroadcastReceiver"
5. External → Service Layer: "Broadcast Intents (node updates, connection state)"
6. Service Layer → Data Layer: "updates node state / records zone events"
7. Data Layer → UI Layer: "StateFlow / Flow (reactive)"

---

STYLE REQUIREMENTS:
- Use a clean, modern flat design
- Each layer should be a distinct horizontal band with a subtle background color
- Component names should be clearly readable (use monospace font for class names)
- Show the 5 layers clearly separated with solid borders
- Add a title at the top: "MeshTracker v1 — Application Architecture (MVVM + Clean layers)"
- Add a subtitle: "Android • Kotlin • Jetpack Compose • Hilt • Room • Coroutines/StateFlow"
- The overall flow direction is top-to-bottom (UI at top, External system at bottom)
- Arrows between layers should be vertical; arrows within a layer (e.g., Repository → DAO) can be horizontal
```

---

## Wskazówki użycia

**Mermaid (diagram blokowy):** wklej prompt i poproś o kod Mermaid `flowchart TB` lub `graph TD`.

**draw.io / Lucidchart:** wklej prompt i poproś o plik XML lub opis elementów do ręcznego odwzorowania.

**Eraser.io:** obsługuje bezpośrednie generowanie diagramów z opisu tekstowego — wklej sekcję PROMPT bez zmian.

**ChatGPT / Claude:** dodaj na początku: *"Generate a Mermaid diagram code for the following architecture:"* lub *"Describe this as a PlantUML component diagram:"*
