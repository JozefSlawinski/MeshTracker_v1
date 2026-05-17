# Wymagania funkcjonalności — MeshTracker v1

**Wersja:** 1.0  
**Data:** 2026-05-15  
**Kontekst:** Dokument opisuje trzy funkcjonalności przygotowujące aplikację do przeprowadzenia testów z kategorii TC-MON (monitoring stref) i TC-DRN (testy z dronem).

---

## Stan wyjściowy — co już istnieje

Przed przystąpieniem do implementacji kluczowe jest zrozumienie istniejącego kodu, aby nie budować rzeczy od nowa.

`PositionHistoryRepository` zbiera `TimedPosition` (lat, lng, alt, timestamp) per węzeł, z filtrowaniem po minimalnej odległości i limitem maksymalnej liczby punktów. `MapScreen` rysuje już `Polyline` na podstawie tej historii — ale **wyłącznie dla aktualnie wybranego węzła** (`selectedNodeId`). `NodeDetailScreen` wyświetla historię jako tabelę tekstową (max. 10 wierszy). `SettingsScreen` ma już kontrolki `historyMaxPoints` i `historyMinDistanceM`.

Czego brakuje: widoczności śladów wszystkich węzłów jednocześnie, liczenia pakietów i mierzenia opóźnień, oraz eksportu danych do pliku.

---

## Funkcjonalność 1 — Ślady historii pozycji dla wszystkich węzłów

### Cel i motywacja

Aktualny kod pokazuje polyline tylko gdy węzeł jest **kliknięty**. W testach TC-DRN-001 i TC-MON-005 operator musi widzieć trasę drona na mapie **w tle**, podczas gdy może jednocześnie obserwować inne węzły. W testach siatką (TC-MON-005) każdy punkt pomiarowy drona musi być widoczny jako ślad — bez tego trzeba by robić zrzuty ekranu po każdym punkcie.

### Zakres zmian

#### 1.1 MapScreen — rysowanie śladów dla wszystkich węzłów

**Co zmienić:** W bloku `GoogleMap { }` w `MapScreen.kt` aktualny kod rysuje jeden `Polyline` warunkowany przez `selectedNodeId`. Należy zastąpić go iteracją po wszystkich węzłach z historią.

**Logika wizualna:**
- Węzeł wybrany (`selectedNodeId`): polyline niebieska, grubość 6f, pełna nieprzezroczystość
- Węzeł z rolą TRACKER (role == 5): polyline zielona, grubość 5f, alpha 0.85f
- Pozostałe węzły z historią: polyline szara, grubość 3f, alpha 0.5f
- Węzły z historią < 2 punktów: pomijane (brak linii)

**Nowy parametr w AppPreferences:** `showAllTracks: Boolean` (domyślnie `true`). Gdy `false` — rysuj tylko ślad wybranego węzła (zachowanie obecne). Dostępny w SettingsScreen jako toggle "Pokaż ślady wszystkich węzłów".

**Wymaganie wydajnościowe:** Przy 3 węzłach i max. 200 punktach historii (konfigurowalny `historyMaxPoints`) rysowanie nie może powodować widocznych lagów mapy. Polyline w `maps-compose` są wydajne — nie ma potrzeby optymalizacji przy tej liczbie punktów.

#### 1.2 MapScreen — znacznik kierunku na końcu śladu

Na ostatnim punkcie każdego śladu (najnowsza pozycja) narysować mały `Marker` bez pinu — tylko kółko — wskazujący, że to koniec trasy. Można wykorzystać istniejący `createCompositeMarker()` bez pinu (heading = kierunek ostatniego segmentu śladu).

> Ten punkt jest opcjonalny (nice-to-have) — nie blokuje testów.

#### 1.3 NodeDetailScreen — miniatura śladu

W sekcji "Historia pozycji" dodać informację o łącznej długości śladu:

```
Historia pozycji (47 punktów) — dystans: ~1.24 km
```

Dystans obliczony przez `PositionHistoryRepository.approximateDistanceMeters()` zsumowany po kolejnych parach punktów.

#### 1.4 Ustawienia — nowy toggle

W `SettingsScreen`, w sekcji "Mapa", dodać:

```
[Switch] Pokaż ślady wszystkich węzłów
         Rysuje trasy GPS dla wszystkich węzłów z historią
```

### Model danych — brak zmian

`TimedPosition` i `PositionHistoryRepository` nie wymagają modyfikacji. Cała zmiana jest w warstwie prezentacji (MapScreen, SettingsScreen) i preferencjach (`AppPreferences`).

### Kryteria akceptacji

| ID | Kryterium | Test |
|---|---|---|
| F1-AC1 | Ślad drona (T1000-E rola TRACKER) widoczny na mapie bez klikania | TC-DRN-001 |
| F1-AC2 | Ślad aktualizuje się w czasie rzeczywistym co ~broadcast interval | TC-DRN-001 |
| F1-AC3 | Wybrany węzeł ma wyraźnie grubszy/jaśniejszy ślad niż pozostałe | TC-MON-005 |
| F1-AC4 | Toggle w ustawieniach ukrywa/pokazuje ślady bez restartu | — |
| F1-AC5 | Przy 3 węzłach × 200 punktów mapa pozostaje płynna | TC-MON-005 |
| F1-AC6 | Węzeł offline z historią nadal pokazuje ostatni znany ślad | TC-DRN-003 |

---

## Funkcjonalność 2 — Statystyki pakietów per węzeł (PDR + Δt)

### Cel i motywacja

Testy TC-MON-001 (PDR w funkcji odległości) i TC-MON-002 (Δt vs liczba hopów) wymagają liczenia odebranych pakietów GPS i mierzenia czasu między nimi. Aktualnie tester musi liczyć ręcznie aktualizacje na ekranie i mierzyć stoperem. Ta funkcjonalność przenosi pomiary do aplikacji.

**Definicje:**
- **PDR (Packet Delivery Ratio)** — stosunek odebranych broadcastów do oczekiwanych, wyrażony w %. Wymaga znajomości `broadcastInterval` trackera (konfigurowany przez usera w ustawieniach lub estymowany z danych).
- **Δt** — czas w sekundach między kolejnymi odebranymi broadcastami od tego samego węzła.
- **Okno pomiarowe** — rolling window ostatnich N pakietów (domyślnie N=20) używane do obliczenia bieżącego PDR i avg Δt.

### Zakres zmian

#### 2.1 Nowy model danych — `PacketStats`

Nowy plik: `model/PacketStats.kt`

```kotlin
data class PacketStats(
    val nodeId: String,
    val receivedCount: Int = 0,           // łączna liczba odebranych broadcastów od startu sesji
    val lastReceivedSeconds: Int = 0,     // timestamp ostatniego odebranego pakietu
    val deltaTHistory: List<Int> = emptyList(),  // ostatnie N wartości Δt [sekundy]
    val sessionStartSeconds: Int = 0      // kiedy zaczęto zliczać (start sesji lub pierwsze odebranie)
) {
    val avgDeltaT: Double
        get() = if (deltaTHistory.isEmpty()) 0.0
                else deltaTHistory.average()

    val minDeltaT: Int
        get() = deltaTHistory.minOrNull() ?: 0

    val maxDeltaT: Int
        get() = deltaTHistory.maxOrNull() ?: 0

    /** PDR obliczone na podstawie obserwowanego avg Δt i podanego expectedIntervalSeconds. */
    fun pdr(expectedIntervalSeconds: Int): Double {[podsumowanie_jak_pisac_prace_dyplomowa.md](podsumowanie_jak_pisac_prace_dyplomowa.md)
        if (expectedIntervalSeconds <= 0 || avgDeltaT <= 0.0) return 0.0
        return (expectedIntervalSeconds / avgDeltaT).coerceAtMost(1.0) * 100.0
    }
}
```

#### 2.2 Nowe repozytorium — `PacketStatsRepository`

Nowy plik: `repository/PacketStatsRepository.kt`

Singleton (Hilt `@Singleton`) z `StateFlow<Map<String, PacketStats>>`. Metoda `record(nodeId, timestampSeconds)` wywoływana przy każdym `onNodeChanged` w `MapViewModel`.

```kotlin
fun record(nodeId: String, timestampSeconds: Int, maxDeltaTHistory: Int = 20)
fun resetAll()
fun resetForNode(nodeId: String)
```

**Logika `record()`:**
1. Pobierz istniejący `PacketStats` dla `nodeId` (lub utwórz nowy z `sessionStartSeconds = timestampSeconds`)
2. Jeśli `lastReceivedSeconds > 0`: oblicz `deltaT = timestampSeconds - lastReceivedSeconds`
   - Odfiltruj anomalie: ignoruj `deltaT < 5` (duplikat) i `deltaT > 600` (przerwa — nie wlicza do avg, ale wlicza do received count)
   - Dodaj `deltaT` do `deltaTHistory`, przytnij do `maxDeltaTHistory`
3. Inkrementuj `receivedCount`
4. Zaktualizuj `lastReceivedSeconds`
5. Wyemituj nowy stan

#### 2.3 Integracja w MapViewModel

W `onNodeChanged()` w `MapViewModel`, po aktualizacji węzła w `_nodes`, wywołać:

```kotlin
packetStatsRepository.record(
    nodeId = nodeInfo.getId(),
    timestampSeconds = (System.currentTimeMillis() / 1000).toInt()
)
```

Dodać do `MapViewModel`:
```kotlin
val packetStats: StateFlow<Map<String, PacketStats>> = packetStatsRepository.stats
```

Dodać do AppPreferences:
```kotlin
val expectedBroadcastInterval: Int  // domyślnie 30 [sekundy]
```

#### 2.4 NodeDetailScreen — nowa sekcja "Statystyki odbioru"

Dodać nową `DetailSection` w `NodeDetailContent`, po sekcji "Sygnał":

```
┌─────────────────────────────────────────────┐
│ Statystyki odbioru                          │
├─────────────────────────────────────────────┤
│ Odebrano pakietów     │ 47                  │
│ Avg Δt                │ 32.4 s              │
│ Min / Max Δt          │ 15 s / 91 s         │
│ PDR (est.)            │ ~93%  [●●●●●●●●●○]  │
│ Oczekiwany interwał   │ 30 s (z ustawień)   │
└─────────────────────────────────────────────┘
```

**Pasek PDR:** `LinearProgressIndicator` z kolorem zielony ≥90%, żółty ≥70%, czerwony <70%.

**Przycisk "Resetuj statystyki":** czyści `PacketStats` dla tego węzła (przydatne przy zmianie punktu pomiarowego w TC-MON-001).

**Uwaga w UI:** Jeśli `receivedCount < 5` — wyświetl "Za mało danych" zamiast wartości PDR (estymacja z <5 pakietów jest niedokładna).

#### 2.5 SettingsScreen — oczekiwany interwał broadcastu

W sekcji "Połączenie" dodać:

```
Oczekiwany interwał broadcastu trackera
[Dropdown: 15s | 30s | 60s | 120s]
Używany do obliczenia PDR w statystykach węzła
```

### Kryteria akceptacji

| ID | Kryterium | Test |
|---|---|---|
| F2-AC1 | Licznik rośnie o 1 przy każdej aktualizacji węzła widocznej na mapie | TC-MON-001 |
| F2-AC2 | Avg Δt ≈ broadcast interval ± 10% gdy węzeł w pełnym zasięgu | TC-MON-002 |
| F2-AC3 | PDR ~100% gdy węzeł w bezpośrednim zasięgu (<50m) | TC-MON-001 |
| F2-AC4 | PDR spada poniżej 80% gdy węzeł jest za zasięgiem LoRa | TC-MON-001 |
| F2-AC5 | "Resetuj statystyki" zeruje licznik bez wpływu na historię pozycji | — |
| F2-AC6 | Dla węzła z <5 pakietami UI pokazuje "Za mało danych" | — |
| F2-AC7 | Statystyki nie są persystowane — reset przy restarcie aplikacji | — |

---

## Funkcjonalność 3 — Eksport sesji do CSV

### Cel i motywacja

Po sesji testowej (np. 3 okrążenia w TC-MON-003, siatka pomiarowa w TC-MON-005) tester musi przenieść dane do arkusza kalkulacyjnego. Aktualnie dane istnieją tylko w pamięci — restart aplikacji je kasuje. Eksport do CSV pozwala na obliczenie PDR, CT, Δt i wygenerowanie wykresów bez ręcznego przepisywania.

### Format pliku

**Nazwa pliku:** `meshtracker_export_YYYYMMDD_HHmmss.csv`  
**Lokalizacja:** katalog `Downloads` (przez `MediaStore` na Android 10+) lub zewnętrzne storage z `FileProvider` (Android 9-)  
**Kodowanie:** UTF-8, separator: `,`, cudzysłowy dla pól tekstowych

**Nagłówek i przykładowe wiersze:**

```csv
timestamp_unix,timestamp_readable,node_id,node_name,role,lat,lng,altitude_m,snr_db,rssi_dbm,hops_away,delta_t_s,battery_pct,speed_ms,heading_deg,satellites
1747296000,2026-05-15 10:00:00,!a1b2c3d4,T1000-Dron,TRACKER,52.123456,21.123456,120,4.5,-85,2,30,82,3,45,8
1747296030,2026-05-15 10:00:30,!a1b2c3d4,T1000-Dron,TRACKER,52.123789,21.124012,122,4.2,-87,2,30,82,3,47,8
1747296000,2026-05-15 10:00:00,!b2c3d4e5,T-Beam,CLIENT,52.100000,21.100000,105,,,0,,68,0,0,0
```

**Opis kolumn:**

| Kolumna | Źródło | Uwagi |
|---|---|---|
| `timestamp_unix` | `TimedPosition.timestampSeconds` | Czas GPS lub czas odbioru jeśli GPS brak |
| `timestamp_readable` | Sformatowany `timestamp_unix` | Format `yyyy-MM-dd HH:mm:ss` |
| `node_id` | `MeshNodeInfo.getId()` | np. `!a1b2c3d4` |
| `node_name` | `MeshNodeInfo.getDisplayName()` | snapshot z chwili exportu |
| `role` | `MeshUserInfo.role` → `roleLabel()` | CLIENT, TRACKER, ROUTER itd. |
| `lat` | `TimedPosition.latitude` | 6 miejsc po przecinku |
| `lng` | `TimedPosition.longitude` | 6 miejsc po przecinku |
| `altitude_m` | `TimedPosition.altitude` | Puste jeśli 0 |
| `snr_db` | `MeshNodeInfo.snr` | Puste jeśli `Float.MAX_VALUE` |
| `rssi_dbm` | `MeshNodeInfo.rssi` | Puste jeśli `Int.MAX_VALUE` |
| `hops_away` | `MeshNodeInfo.hopsAway` | |
| `delta_t_s` | `PacketStats.deltaTHistory` | Δt dla tego konkretnego pakietu; puste dla pierwszego |
| `battery_pct` | `MeshNodeInfo.batteryLevel` | Puste jeśli 0 |
| `speed_ms` | `MeshPosition.groundSpeed` | Puste jeśli 0 |
| `heading_deg` | `MeshPosition.groundTrack` | Puste jeśli 0 |
| `satellites` | `MeshPosition.satellitesInView` | Puste jeśli 0 |

> **Ważne:** Eksport zawiera historię z `PositionHistoryRepository`, nie tylko aktualny stan. Każdy zapis w historii staje się jednym wierszem CSV. Węzły bez historii (np. brak pozycji GPS) są eksportowane jako jeden wiersz z aktualnym stanem (bez lat/lng).

### Zakres zmian

#### 3.1 Nowa klasa — `CsvExporter`

Nowy plik: `util/CsvExporter.kt`

```kotlin
class CsvExporter @Inject constructor(
    private val context: Context,
    private val historyRepository: PositionHistoryRepository,
    private val statsRepository: PacketStatsRepository
) {
    suspend fun export(nodes: Map<String, MeshNodeInfo>): Result<Uri>
}
```

Logika:
1. Zbierz dane: `historyRepository.history.value` + `statsRepository.stats.value` + `nodes`
2. Dla każdego `nodeId` w historii: iteruj po `TimedPosition`, dobierz `deltaT` z `PacketStats.deltaTHistory[index]` (jeśli dostępne)
3. Dla węzłów bez historii: jeden wiersz z aktualnym stanem
4. Zapisz plik przez `MediaStore.Downloads` (API 29+) lub `FileProvider` (API <29)
5. Zwróć `Uri` do pliku

#### 3.2 NodeDetailScreen — przycisk eksportu

W sekcji przycisków akcji (obok "Pokaż na mapie" i "Udostępnij") dodać trzeci przycisk:

```
[📊 Eksportuj dane]
```

Kliknięcie wywołuje `CsvExporter.export()` dla **tego konkretnego węzła** (filtrowanie po `nodeId`). Po sukcesie otwiera Android Share sheet z plikiem CSV.

#### 3.3 SettingsScreen — eksport całej sesji

W nowej sekcji "Dane testowe" w `SettingsScreen`:

```
┌─────────────────────────────────────────────────┐
│ Dane testowe                                    │
├─────────────────────────────────────────────────┤
│ [Eksportuj sesję do CSV]                        │
│ Eksportuje historię pozycji wszystkich węzłów  │
│                                                 │
│ [Wyczyść historię pozycji]           [Potwierdź]│
│ Usuwa ślady z mapy i dane do exportu            │
└─────────────────────────────────────────────────┘
```

**"Eksportuj sesję"** wywołuje `CsvExporter.export()` dla wszystkich węzłów. Jeśli historia jest pusta — pokazuje `Snackbar` "Brak danych do eksportu — uruchom monitoring i poczekaj na aktualizacje węzłów".

**"Wyczyść historię"** wywołuje `positionHistoryRepository.clearAll()` + `packetStatsRepository.resetAll()`. Wymaga potwierdzenia przez `AlertDialog` (identyczny wzorzec jak "Wyczyść log zdarzeń" w `ZoneDetailScreen`).

#### 3.4 Uprawnienia w AndroidManifest

Na Android <10 wymagane `WRITE_EXTERNAL_STORAGE`. Na Android 10+ `MediaStore` nie wymaga dodatkowego uprawnienia.

```xml
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

Runtime request tylko na API <29 — analogicznie do wzorca już stosowanego dla lokalizacji w `MapScreen`.

### Kryteria akceptacji

| ID | Kryterium | Test |
|---|---|---|
| F3-AC1 | Plik CSV zawiera wiersz dla każdego punktu w historii węzła | TC-MON-001 po sesji |
| F3-AC2 | Kolumna `delta_t_s` jest wypełniona dla pakietów 2+ w sesji | TC-MON-002 |
| F3-AC3 | Plik otwiera się poprawnie w LibreOffice Calc / Excel (kodowanie UTF-8, separator `,`) | — |
| F3-AC4 | Eksport per węzeł (NodeDetailScreen) zawiera tylko wiersze dla tego węzła | — |
| F3-AC5 | Eksport całej sesji (SettingsScreen) zawiera wiersze dla wszystkich węzłów | TC-MON-005 |
| F3-AC6 | Przy pustej historii eksport pokazuje `Snackbar` zamiast tworzyć pusty plik | — |
| F3-AC7 | "Wyczyść historię" wymaga potwierdzenia i czyści zarówno historię jak i statystyki | — |
| F3-AC8 | Plik zapisywany jest w katalogu `Downloads` i dostępny po zamknięciu aplikacji | — |

---

## Zależności między funkcjonalnościami

```
F1 (Track history)
  └─ brak zależności od F2/F3 — niezależna warstwa UI

F2 (Statystyki pakietów)
  └─ wymagane przez F3 — kolumna delta_t_s w CSV pochodzi z PacketStatsRepository

F3 (Eksport CSV)
  ├─ wymaga F2 (PacketStatsRepository) dla kolumny delta_t_s
  └─ wymaga danych z PositionHistoryRepository (już istnieje)
```

**Zalecana kolejność implementacji:** F1 → F2 → F3

---

## Powiązanie z testami TC-MON

| Funkcjonalność | Odblokowuje testy |
|---|---|
| F1 — Track history (wszystkie węzły) | TC-DRN-001, TC-DRN-002, TC-MON-005 |
| F2 — Statystyki PDR + Δt | TC-MON-001, TC-MON-002, TC-MON-006 |
| F3 — Eksport CSV | TC-MON-001 (arkusz), TC-MON-002 (arkusz), TC-MON-003 (obliczenia CT) |

Testy TC-MON-003, TC-MON-004 i TC-MON-007 nie wymagają żadnej z tych funkcjonalności — można je przeprowadzić z aktualną wersją aplikacji (geofencing jest już kompletny).

---

*Dokument wymagań dla MeshTracker v1 | powiązany z: TESTY_FUNKCJONALNE.md*
