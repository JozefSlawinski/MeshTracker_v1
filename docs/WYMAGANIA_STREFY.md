# Wymagania — Funkcjonalność Stref (Geofencing)

**Data:** 2026-05-14
**Status:** Zebrane wymagania, przed implementacją

---

## 1. Opis funkcjonalności

Strefy to obszary geograficzne w kształcie wielokątów rysowanych przez użytkownika na mapie.
Aplikacja monitoruje wybrane węzły Meshtastic i wykrywa gdy wchodzą lub opuszczają zdefiniowane strefy,
generując powiadomienia Android oraz zapisując log zdarzeń.

---

## 2. Wymagania funkcjonalne

### Z-1 — Kształt strefy
- Strefa ma kształt **wielokąta** o dowolnej liczbie wierzchołków (minimum 3).
- Każda strefa posiada: nazwę, kolor, status aktywna/nieaktywna, listę monitorowanych węzłów.

### Z-2 — Rysowanie wielokąta
- Użytkownik wchodzi w tryb rysowania przez FAB na mapie.
- **Long-press** na dowolnym miejscu mapy dodaje kolejny wierzchołek.
- Podgląd rysowania: linia łącząca wierzchołki (Polyline) wyświetlana na bieżąco.
- Gdy liczba wierzchołków ≥ 3, pojawia się FAB „Zamknij strefę".
- Po zamknięciu wielokąta wyświetla się dialog do wpisania nazwy strefy i wyboru węzłów.
- Możliwe jest anulowanie rysowania (FAB „Anuluj" dostępny od pierwszego wierzchołka).

### Z-3 — Nawigacja do zarządzania strefami
- Na ekranie mapy wyświetlany jest **FAB** (Floating Action Button) „Strefy".
- FAB otwiera **ModalBottomSheet** z listą wszystkich zdefiniowanych stref.
- Z listy można: włączyć/wyłączyć strefę, przejść do jej szczegółów, usunąć strefę, dodać nową (→ tryb rysowania).

### Z-4 — Monitorowane węzły (per strefa)
- Każda strefa konfiguruje **własną listę węzłów** do monitorowania.
- Wybór węzłów przez `NodePickerDialog` (multi-select z aktualnej listy węzłów z GPS).
- Tylko węzły z tej listy generują alerty i log dla danej strefy.

### Z-5 — Zdarzenia i alerty
| Zdarzenie | Akcja |
|-----------|-------|
| Węzeł **wchodzi** do strefy | Powiadomienie Android + log ENTER + zmiana wyglądu markera |
| Węzeł **wychodzi** ze strefy | Powiadomienie Android + log EXIT + powrót markera do normalnego wyglądu |
| Strefa nieaktywna | Brak alertów, wielokąt nadal widoczny na mapie (wyszarzony) |

### Z-6 — Wygląd strefy na mapie
- Aktywna strefa: **półprzezroczysty wypełniony wielokąt** w kolorze strefy (alpha ~30–40%).
- Nieaktywna strefa: szary, półprzezroczysty wielokąt (alpha ~15%).
- Brak etykiety nazwy na mapie (zbyt duże zaśmiecenie przy wielu strefach).
- Marker węzła znajdującego się w aktywnej strefie: **wyróżniony** (inna ikona / obramowanie).

### Z-7 — Log zdarzeń
- Każde zdarzenie ENTER/EXIT jest zapisywane w bazie danych z: `zoneId`, `nodeId`, `typ (ENTER/EXIT)`, `timestampSeconds`.
- Log jest wyświetlany w ekranie `ZoneDetailScreen` posortowany od najnowszego.
- Log można wyczyścić (przycisk „Wyczyść historię" z potwierdzeniem).

### Z-8 — Aktywacja/dezaktywacja strefy
- Każda strefa ma przełącznik (Toggle/Switch) w `ZoneBottomSheet`.
- Nieaktywna strefa: brak monitorowania, brak powiadomień, nadal widoczna na mapie (wyszarzona).

### Z-9 — Skala
- Maksymalna liczba stref: ~10 (prosta lista, bez paginacji).

---

## 3. Wymagania niefunkcjonalne

### Z-NF-1 — Persystencja
- Strefy i log zdarzeń przechowywane w **Room (SQLite)**.
- Dane przeżywają restart aplikacji.
- Schema Room wymaga zdefiniowania strategii migracji (exportSchema = true).

### Z-NF-2 — Monitoring w tle
- Detekcja wejść/wyjść realizowana przez **Android Foreground Service**.
- Serwis startuje automatycznie gdy istnieje ≥1 aktywna strefa.
- Zatrzymuje się gdy wszystkie strefy są nieaktywne lub usunięte.
- Serwis rejestruje własny `BroadcastReceiver` na `ACTION_NODE_CHANGE`.
- Wymagane uprawnienie: `FOREGROUND_SERVICE`, typ: `dataSync`.

### Z-NF-3 — Powiadomienia
- Dedykowany kanał powiadomień: `CHANNEL_ZONE_ALERTS` (priorytet HIGH).
- Każde powiadomienie zawiera: nazwę strefy, nazwę węzła, typ zdarzenia (wszedł/wyszedł).
- Tap w powiadomienie otwiera aplikację na ekranie mapy z zaznaczoną strefą.

---

## 4. Architektura

```
Data Layer (Room)
  ├── Zone          — id, name, color, isActive, verticesJson, watchedNodeIdsJson
  ├── ZoneEvent     — id, zoneId, nodeId, eventType (ENTER/EXIT), timestampSeconds
  ├── ZoneDao       — flows: allZones(), eventsForZone(zoneId)
  └── ZoneDatabase  — @Database + Hilt @Module

Repository
  └── ZoneRepository — CRUD + event recording + eksport danych jako Flow

Logic
  └── GeofenceChecker — algorytm ray-casting (punkt wewnątrz wielokąta)

Service
  └── ZoneMonitorService (Foreground Service + @AndroidEntryPoint)
        ├── wewnętrzny BroadcastReceiver → ACTION_NODE_CHANGE
        ├── state: Map<nodeId, Set<zoneId>>  (poprzedni stan — które węzły były w których strefach)
        ├── wykrycie zmiany stanu → ZoneRepository.recordEvent()
        └── NotificationManagerCompat.notify()

ViewModels
  ├── ZoneViewModel   — CRUD stref, stan rysowania, wybór węzłów
  └── MapViewModel    — rozszerzony o: activeZones: StateFlow, nodesInZones: StateFlow

UI
  ├── MapScreen            — Polygon overlay, long-press, Polyline podglądu, FAB
  ├── ZoneBottomSheet      — lista stref z togglem, add/edit/delete
  ├── ZoneDetailScreen     — informacje o strefie + log zdarzeń
  └── NodePickerDialog     — multi-select węzłów
```

---

## 5. Nowe zależności (gradle)

```toml
# libs.versions.toml
room-runtime = "2.6.1"
room-ktx = "2.6.1"
room-compiler = "2.6.1"   # KSP
```

```kotlin
// app/build.gradle.kts
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
```

> `com.google.maps.android.compose.Polygon` — dostępny w istniejącej zależności maps-compose, bez zmian w gradle.

---

## 6. Nowe pliki

| Plik | Typ | Opis |
|------|-----|------|
| `model/Zone.kt` | Room @Entity | Definicja strefy |
| `model/ZoneEvent.kt` | Room @Entity | Zdarzenie ENTER/EXIT |
| `data/ZoneDao.kt` | Room @Dao | Zapytania do bazy |
| `data/ZoneDatabase.kt` | Room @Database | Baza + Hilt module |
| `repository/ZoneRepository.kt` | Singleton | CRUD + flow |
| `logic/GeofenceChecker.kt` | Pure Kotlin | Ray-casting |
| `service/ZoneMonitorService.kt` | ForegroundService | Monitoring w tle |
| `ui/zones/ZoneViewModel.kt` | @HiltViewModel | Stan UI stref |
| `ui/zones/ZoneBottomSheet.kt` | Composable | Lista stref |
| `ui/zones/ZoneDetailScreen.kt` | Composable | Szczegóły + log |
| `ui/zones/NodePickerDialog.kt` | Composable | Wybór węzłów |

### Zmodyfikowane pliki

| Plik | Zmiana |
|------|--------|
| `ui/map/MapScreen.kt` | Polygon overlay, tryb rysowania, FAB |
| `ui/map/MapViewModel.kt` | `activeZones`, `nodesInZones`, `drawingState` |
| `AndroidManifest.xml` | `<service>`, `FOREGROUND_SERVICE` permission, notification channel |
| `app/build.gradle.kts` | Room dependencies |

---

## 7. Kolejność implementacji

```
Etap 1 — Baza danych
  1. Zone + ZoneEvent entities
  2. ZoneDao
  3. ZoneDatabase + Hilt module
  4. ZoneRepository

Etap 2 — Logika geofencingu
  5. GeofenceChecker (ray-casting)

Etap 3 — ViewModel i stan UI
  6. ZoneViewModel (CRUD, drawing state machine)
  7. Rozszerzenie MapViewModel

Etap 4 — Foreground Service
  8. ZoneMonitorService
  9. Kanał powiadomień + AndroidManifest

Etap 5 — UI
  10. MapScreen — Polygon overlay + tryb rysowania
  11. ZoneBottomSheet
  12. NodePickerDialog
  13. ZoneDetailScreen
```

---

## 8. Ryzyka i uwagi techniczne

| Ryzyko | Opis | Mitygacja |
|--------|------|-----------|
| Hilt w Foreground Service | `@AndroidEntryPoint` wymaga specjalnej konfiguracji | Użyć `EntryPointAccessors` jeśli Hilt nie wspiera bezpośrednio |
| Ray-casting przy antymerydianach | Wielokąty przecinające ±180° lon | Ograniczenie: strefy tylko w Polsce/Europie — brak obsługi |
| Wiele stref na małym ekranie | Nakładające się wielokąty | Różne kolory + kolejność renderowania (zIndex) |
| BroadcastReceiver w serwisie | Android 8+ ogranicza implicit broadcasts | `ACTION_NODE_CHANGE` to explicit broadcast z Meshtastic — OK |
| Tryb rysowania vs. normalne kliknięcia mapy | Long-press musi być odróżniony od tap | Osobny `drawingMode: Boolean` w stanie VM |
