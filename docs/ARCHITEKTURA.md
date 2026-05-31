# ADR-001: Architektura aplikacji MeshTracker v1

**Status:** Accepted  
**Data:** 2026-05-18  
**Projekt:** MeshTracker v1 — Android  
**Decydenci:** Jozef Slawinski

---

## Kontekst

MeshTracker to aplikacja Android śledząca węzły sieci radiowej Meshtastic. Aplikacja łączy się z zewnętrzną aplikacją Meshtastic (przez IPC), wizualizuje pozycje węzłów na mapie Google Maps, śledzi historię ruchu oraz pozwala na zdefiniowanie wielokątnych stref geofencingu (ENTER/EXIT) z powiadomieniami. Wymagania niefunkcjonalne obejmują: pracę w tle (foreground service), reaktywne odświeżanie UI oraz testowalność warstwy danych.

---

## Decyzja

Aplikacja przyjmuje **wzorzec MVVM** (Model-View-ViewModel) z **warstwową architekturą** Clean-inspired, wstrzykiwaniem zależności przez **Hilt/Dagger** oraz **reaktywnymi strumieniami danych** (Kotlin StateFlow/Flow). Całość napisana w Kotlin z Jetpack Compose jako UI toolkit.

---

## Opcje rozważane

### Opcja A: MVVM + Hilt + Jetpack Compose ✅ (wybrana)

| Wymiar | Ocena |
|--------|-------|
| Złożoność | Średnia |
| Koszt utrzymania | Niski — wzorzec oficjalnie wspierany przez Google |
| Skalowalność | Wysoka — łatwe dodawanie nowych ekranów i repozytoriów |
| Znajomość przez zespół | Wysoka — standardowy stack Android |
| Testowalność | Wysoka — ViewModele i repozytoria łatwe do izolowania |

**Zalety:** Reaktywne UI przez StateFlow/Compose, prosta integracja DI, naturalne zarządzanie cyklem życia, czytelny podział warstw.  
**Wady:** Nieco więcej boilerplate niż MVI, złożona konfiguracja Hilt przy dużym projekcie.

### Opcja B: MVI (Model-View-Intent)

| Wymiar | Ocena |
|--------|-------|
| Złożoność | Wysoka |
| Testowalność | Bardzo wysoka |
| Znajomość przez zespół | Niska — wymaga dodatkowych bibliotek (Orbit, Mavericks) |

**Zalety:** Jednokierunkowy przepływ danych, łatwy debugging stanu.  
**Wady:** Wyższy próg wejścia, naddatek abstrakcji dla projektu tej skali.

---

## Analiza kompromisów

MVVM z Hilt oferuje optymalny balans między testowalną separacją warstw a produktywnością developmentu. MVI byłby lepszym wyborem dla bardzo złożonych ekranów z dziesiątkami zdarzeń, ale przy ekranach tej skali (mapa + lista + strefy) stanowiłby przerost formy. Reaktywne StateFlow w ViewModelach spełnia główny cel MVI — jeden punkt prawdy dla stanu UI.

---

## Architektura systemu

Aplikacja wykorzystuje architekturę opartą na wzorcu **MVVM (Model-View-ViewModel)** z dodatkowymi warstwami odpowiedzialnymi za komunikację zewnętrzną i zarządzanie danymi.
```
┌─────────────────────────────────────────────────────────────────┐
│                         WARSTWA UI                              │
│  MainActivity → MainScreen (nawigacja dolna)                    │
│  ┌──────────────┐  ┌────────────────┐  ┌───────────────────┐   │
│  │  MapScreen   │  │ NodeListScreen │  │  SettingsScreen   │   │
│  │  MapViewModel│  │ NodeDetail...  │  │  SettingsViewModel│   │
│  └──────────────┘  └────────────────┘  └───────────────────┘   │
│  ┌──────────────────────────────────────┐                       │
│  │  ZoneDetailScreen / ZoneBottomSheet  │                       │
│  │  ZoneViewModel                       │                       │
│  └──────────────────────────────────────┘                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │ StateFlow / coroutines
┌─────────────────────▼───────────────────────────────────────────┐
│                         WARSTWA LOGIKI                          │
│  GeofenceChecker  (ray-casting — czyste JVM, bez Android SDK)   │
│  NodeFilterState  (kryteria filtrowania listy węzłów)           │
│  NodeToMarkerMapper (model → marker na mapie)                   │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│                    WARSTWA DANYCH                               │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐   │
│  │   MeshRepository    │  │       ZoneRepository            │   │
│  │  (interface + impl) │  │  (Room Flow — strefy, eventy)   │   │
│  └──────────┬──────────┘  └───────────────┬─────────────────┘   │
│  ┌──────────▼──────────┐  ┌───────────────▼─────────────────┐   │
│  │PositionHistory Repo │  │   PacketStatsRepository         │   │
│  │  (in-memory Flow)   │  │   (in-memory, per-node stats)   │   │
│  └─────────────────────┘  └─────────────────────────────────┘   │
│  AppPreferences (DataStore Preferences)                         │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│                     WARSTWA SERWISÓW                            │
│  ┌───────────────────────┐   ┌──────────────────────────────┐   │
│  │  MeshServiceManager   │   │    ZoneMonitorService        │   │
│  │  (AIDL binding,       │   │  (Foreground Service,        │   │
│  │   singleton)          │   │   geofence ENTER/EXIT)       │   │
│  └───────────┬───────────┘   └──────────────────────────────┘   │
│  ┌───────────▼───────────────────────────────────────────────┐  │
│  │  MeshtasticBroadcastReceiver                              │  │
│  │  (odbiera ACTION_NODE_CHANGE, CONNECTED, DISCONNECTED)    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────────────────┘
                      │ 
┌─────────────────────▼───────────────────────────────────────────┐
│               ZEWNĘTRZNA APLIKACJA MESHTASTIC                   │
│  com.geeksville.mesh — IMeshService (AIDL)                      │
│  Broadcasty: ACTION_NODE_CHANGE, MESH_CONNECTED, etc.           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Opis warstw

### 1. Warstwa UI (Presentation)

Zbudowana w **Jetpack Compose + Material 3**. Nawigacja odbywa się przez dolny pasek (`NavigationSuiteScaffold`). Każdy ekran posiada dedykowany `ViewModel` wstrzykiwany przez Hilt.

- **`MapScreen` / `MapViewModel`** — główny ekran z mapą Google Maps Compose, markerami węzłów, historią tras i nakładką stref geofencingu.
- **`NodeListScreen` / `NodeDetailScreen`** — lista i szczegóły węzłów z filtrowaniem (online/GPS/szukaj).
- **`ZoneDetailScreen` / `ZoneViewModel`** — zarządzanie strefami (rysowanie wielokąta long-pressem, CRUD, log zdarzeń ENTER/EXIT).
- **`SettingsScreen` / `SettingsViewModel`** — konfiguracja: typ mapy, próg "online", interwał odświeżania, eksport CSV.

Stan każdego `ViewModel` wystawiony jest jako `StateFlow`, co zapewnia reaktywne re-kompozycje UI.

### 2. Warstwa domeny / logiki

Czyste klasy Kotlin bez zależności od Android SDK — w pełni testowalne jako unit testy JVM.

- **`GeofenceChecker`** — algorytm ray-casting do sprawdzania przynależności punktu GPS do wielokąta. Wystawia też `computeNodeZoneMap()` dla reaktywnego przeliczenia całej mapy węzłów.
- **`NodeFilterState`** — niemutowalne data class z kryteriami filtrowania.
- **`NodeToMarkerMapper`** — mapuje `MeshNodeInfo` na dane markera dla Compose Maps.

### 3. Warstwa danych

Zapewnia abstrakcję źródeł danych i zarządza logiką persystencji.

- **`MeshRepository`** — interfejs definiujący kontrakt połączenia i odbioru zdarzeń z sieci Meshtastic. Implementacja produkcyjna: `MeshServiceRepository`; w testach podmieniana na `FakeMeshRepository`.
- **`ZoneRepository`** — CRUD dla stref i logów zdarzeń; wystawia `Flow<List<Zone>>` i `Flow<List<ZoneEvent>>` z Room.
- **`PositionHistoryRepository`** — historia pozycji węzłów (in-memory `MutableStateFlow`); zachowuje maksymalnie N punktów z minimalną odległością między nimi.
- **`PacketStatsRepository`** — zlicza odebrane pakiety per węzeł (in-memory).
- **`AppPreferences`** — ustawienia użytkownika przez DataStore Preferences.

### 4. Warstwa serwisów i IPC

- **`MeshServiceManager`** — singleton zarządzający cyklem życia bindingu AIDL do aplikacji Meshtastic. Używa Java Reflection do obsługi różnych wersji API Meshtastic (`getNodes()`, `getMyId()`, `connectionState()`). Opcjonalnie przechodzi do trybu broadcast-only gdy klasy AIDL nie są dostępne w classpath.
- **`ZoneMonitorService`** — Foreground Service uruchamiany przez `ZoneViewModel` gdy istnieje ≥1 aktywna strefa. Rejestruje własną instancję `MeshtasticBroadcastReceiver`, zbiera aktywne strefy przez `Flow` z Room i na każdy update węzła sprawdza przynależność do stref (algorytm ray-casting), zapisuje zdarzenie i wysyła powiadomienie push.
- **`MeshtasticBroadcastReceiver`** — odbiera broadcasty z aplikacji Meshtastic (`ACTION_NODE_CHANGE`, `MESH_CONNECTED`, `MESH_DISCONNECTED`) i konwertuje je na wywołania interfejsu `MeshtasticReceiverListener`.

### 5. Persystencja danych

| Magazyn | Technologia | Dane |
|---------|-------------|------|
| Strefy geofencingu | Room (`ZoneDatabase`) | `Zone`, `ZoneEvent` (relacja 1:N, CASCADE delete) |
| Ustawienia użytkownika | DataStore Preferences | Progi, interwały, typ mapy |
| Historia pozycji | In-memory (`StateFlow`) | Max N punktów na węzeł, brak persystencji między sesjami |
| Statystyki pakietów | In-memory (`ConcurrentHashMap`) | Licznik pakietów per węzeł |

Wierzchołki wielokątów i listy obserwowanych węzłów przechowywane są jako JSON string w kolumnach Room (bez `TypeConverter`) — upraszcza migracje i pozwala uniknąć zależności od Room dla klas dziedzinowych.

---

## Integracja z Meshtastic

Aplikacja Meshtastic działa jako osobna apka na urządzeniu. MeshTracker komunikuje się z nią na dwa uzupełniające się sposoby:

```
Meshtastic App
     │
     ├─► AIDL IMeshService (bindService)
     │     → getNodes(), connectionState(), getMyId()
     │     → MeshServiceManager (singleton, reflection)
     │
     └─► Broadcast Intents (ACTION_NODE_CHANGE itp.)
           → MeshtasticBroadcastReceiver
           → MeshServiceRepository / ZoneMonitorService
```

Podejście z reflection pozwala zachować zgodność z różnymi wersjami aplikacji Meshtastic bez recompilacji AIDL stub. Tryb broadcast-only działa nawet gdy AIDL nie jest dostępne.

---

## Wstrzykiwanie zależności (Hilt)

```
SingletonComponent
  ├── MeshServiceManager  (Provides — singleton z Application context)
  ├── MeshRepository      (Binds → MeshServiceRepository)
  ├── ZoneDatabase        (Provides via DatabaseModule)
  ├── ZoneDao             (Provides via DatabaseModule)
  ├── AppPreferences      (Provides via DatabaseModule)
  └── CsvExporter         (Provides via DatabaseModule)

ViewModelComponent (HiltViewModel)
  ├── MapViewModel
  ├── ZoneViewModel
  └── SettingsViewModel
```

---

## Zarządzanie stanem połączenia

`MapViewModel` modeluje stan połączenia jako sealed class:

```
Disconnected ──connect()──► Connecting ──AIDL ready──► [check radio]
                                                              │
                                            radioState=CONNECTED ──► Connected
                                            radioState≠CONNECTED ──► Connecting (poll 2s)
                                            AIDL unavailable ──────► Connecting (wait broadcast)

Connected ──MESH_DISCONNECTED──► Reconnecting (countdown 30s) ──► Connected / Disconnected
```

Automatyczny reconnect działa z 30-sekundowymi interwałami. Odbierany broadcast `ACTION_NODE_CHANGE` inkrementalnie naprawia stan z `Connecting` na `Connected` (race-condition guard).

---

## Konsekwencje

**Ułatwione przez tę architekturę:**
- Podmiana implementacji repozytorium w testach (interfejs `MeshRepository`).
- Testowanie logiki geofencingu jako czystych unit testów JVM (brak zależności Android).
- Reaktywne, wydajne UI dzięki granularnym StateFlow i Compose.
- Praca w tle (ZoneMonitorService) niezależna od cyklu życia Activity.

**Utrudnione / do rozważenia w przyszłości:**
- Historia pozycji i statystyki pakietów są in-memory — utrata danych po zamknięciu aplikacji. Do rozważenia: persystencja w Room.
- JSON strings dla wierzchołków stref utrudniają zapytania SQL (np. "wszystkie strefy zawierające dany punkt"). Przy rozbudowie warto wprowadzić `TypeConverter` lub osobną tabelę `zone_vertices`.
- Reflection dla AIDL jest kruche — aktualizacja Meshtastic może zmienić API. Do rozważenia: oficjalny Meshtastic SDK lub generowany kod z protobuf.
- Brak eksportu schematu Room (`exportSchema = false`) — należy włączyć i skonfigurować migracje przed pierwszym release produkcyjnym.

---

## Działania do podjęcia

- [ ] Włączyć `exportSchema = true` w `ZoneDatabase` i dodać migracje Room przed v1.0
- [ ] Rozważyć persystencję historii pozycji (Room lub pliki) dla użytkowników z przerwami sieciowymi
- [ ] Dodać tabelę `zone_vertices` lub `TypeConverter` jeśli pojawią się zapytania przestrzenne
- [ ] Monitorować zmiany API Meshtastic i rozważyć oficjalny SDK gdy będzie dostępny
- [ ] Zwiększyć pokrycie testami integracyjnymi `MeshServiceRepository` (Hilt + Espresso)

---

## Opis ekranów aplikacji

Aplikacja zawiera **5 ekranów nawigacyjnych** zarządzanych przez `MainScreen`. Trzy główne ekrany dostępne są przez dolny pasek nawigacyjny (Mapa / Węzły / Ustawienia). Dwa ekrany szczegółów (węzeł, strefa) zastępują pasek nawigacyjny i wyświetlają własny `TopAppBar` z przyciskiem powrotu.

Globalny komponent `ConnectionStatusBar` pojawia się nad każdym z trzech głównych ekranów i informuje o stanie połączenia z Meshtastic.

---

### Ekran 1 — Mapa (`MapScreen`)

**Plik:** `ui/map/MapScreen.kt` | **ViewModel:** `MapViewModel`

Główny ekran aplikacji. Wyświetla interaktywną mapę Google Maps z nakładkami węzłów i stref geofencingu oraz obsługuje tryb rysowania wielokątów.

#### Elementy wizualne

**Mapa Google Maps (GoogleMap Compose)** — zajmuje cały ekran. Typ mapy konfigurowalny (normalny / satelita / teren / hybryda). Stan kamery przechowywany w `MainScreen` — przeżywa przełączanie zakładek.

**Markery węzłów** — każdy marker to niestandardowa bitmapa rysowana programowo na `Canvas`:
- Kółko z 1-2 inicjałami węzła (skrócona nazwa lub prefix node ID)
- Kolor kółka zależy od stanu: żółty (zaznaczony), szary (offline), magenta (w strefie), czerwony (CLIENT), niebieski (ROUTER), zielony (TRACKER)
- Dot SNR w prawym górnym rogu kółka — zielony/pomarańczowy/czerwony zależnie od jakości sygnału
- Węzeł lokalny (BT) — zamiast dota SNR pokazuje niebieską ikonkę telefonu
- Węzeł statyczny: marker z pinem (anchor w czubku)
- Węzeł w ruchu (`groundTrack > 0`): strzałka wychodząca z kółka w kierunku ruchu, anchor w środku kółka
- Ikony cachowane w `MutableMap<MarkerKey, BitmapDescriptor>` — bitmapa przerysowywana tylko gdy zmieni się stan wizualny
- Snippet markera zawiera: status online/offline, bateria %, SNR dB, prędkość m/s, kierunek °, wiek GPS, ostrzeżenie o obniżonej precyzji

**Wielokąty stref** — aktywne strefy renderowane jako `Polygon` z przezroczystym wypełnieniem (alpha 0.25) i obrysem w kolorze strefy.

**Historia tras (Polyline)** — w trybie "wszystkie ślady": trasy wszystkich węzłów jednocześnie (zaznaczony — niebieski gruby, tracker — zielony, pozostałe — szare półprzezroczyste). W trybie domyślnym: tylko trasa zaznaczonego węzła.

**Podgląd rysowanego wielokąta** — zielona polilinia + zielone markery wierzchołków podczas trybu rysowania.

**Komunikaty stanu:**
- "Brak węzłów w sieci" / "Brak węzłów z pozycją GPS" gdy lista jest pusta
- Spinner `CircularProgressIndicator` podczas łączenia
- Pasek informacyjny "Dodano N wierzchołków • Przytrzymaj mapę, aby dodać kolejny" w trybie rysowania

**Floating Action Buttons (lewy dolny róg):**
- Tryb normalny: pojedynczy FAB `Place` — otwiera ZoneBottomSheet
- Tryb rysowania: trzy `ExtendedFloatingActionButton` — Anuluj / Cofnij (gdy ≥1 wierzchołek) / Zamknij (gdy ≥3 wierzchołki)
- Tryb potwierdzania: FABs ukryte (dialog zajmuje ekran)

#### Interakcje użytkownika

| Gest | Efekt |
|------|-------|
| Tap markera węzła | Zaznaczenie węzła — kamera animuje do jego pozycji, marker powiększa się |
| Tap mapy (poza markerem) | Odznaczenie węzła |
| Long-press na mapie (tryb rysowania) | Dodanie wierzchołka wielokąta |
| FAB Strefy | Otwarcie ZoneBottomSheet |
| FAB Zamknij | Zamknięcie wielokąta → dialog ZoneConfirmDialog |
| FAB Cofnij | Usunięcie ostatniego wierzchołka |
| FAB Anuluj | Porzucenie rysowania |

#### Obsługa uprawnień

Przy pierwszym uruchomieniu automatycznie prosi o `ACCESS_FINE_LOCATION` i `ACCESS_COARSE_LOCATION`. Mapa włącza niebieską kropkę "Moja lokalizacja" po przyznaniu uprawnienia.

---

### Ekran 2 — Lista węzłów (`NodeListScreen`)

**Plik:** `ui/nodes/NodeListScreen.kt` | **ViewModel:** `MapViewModel` (współdzielony)

Ekran przeglądu wszystkich węzłów sieci z możliwością filtrowania i wyszukiwania.

#### Elementy wizualne

**Pasek wyszukiwania** (`OutlinedTextField`) — wyszukiwanie po nazwie węzła lub node ID (case-insensitive). Przycisk X do czyszczenia zapytania.

**Chipy filtrów** (`FilterChip`) — dwa togglowalne filtry:
- "Tylko online" — ukrywa węzły, od których nie było słyszalności przez `onlineThreshold` minut
- "Ma GPS" — ukrywa węzły bez ważnej pozycji
- Gdy aktywny jakikolwiek filtr — pojawia się link "Wyczyść filtry"

**Licznik wyników** — "Węzły: N" lub "Wyniki: X / N węzłów" gdy filtry są aktywne.

**Lista węzłów** (`LazyColumn`) — elementy `NodeItem` z kluczem `nodeId` (wydajna rekompozycja). Sortowanie: online przed offline, w ramach grupy alfabetycznie.

**Każdy `NodeItem` zawiera:**
- Nazwa węzła + badge Online (zielony) / Offline (szary)
- ID węzła (monospace)
- Rola (CLIENT / TRACKER / inne)
- Współrzędne GPS + wysokość (jeśli dostępne)
- Prędkość i kierunek (jeśli w ruchu)
- Wiek pozycji GPS — czerwony gdy > 10 minut
- Ostrzeżenie ⚠ o obniżonej precyzji GPS (precisionBits ≤ 27)
- Poziom baterii 🔋, SNR 📡, RSSI 📶
- Czas ostatniego kontaktu (format HH:mm:ss gdy dziś, dd.MM HH:mm wcześniej)
- Liczba przeskoków (hopCount > 0)

**Stany puste:**
- Spinner podczas łączenia gdy brak węzłów
- "Brak węzłów spełniających kryteria" + przycisk "Wyczyść filtry" gdy filtry nie pasują
- "Brak węzłów w sieci" / "Brak połączenia z Meshtastic" gdy lista globalnie pusta

#### Interakcje

Kliknięcie `NodeItem` → nawigacja do `NodeDetailScreen` z przekazanym `nodeId`.

---

### Ekran 3 — Szczegóły węzła (`NodeDetailScreen`)

**Plik:** `ui/nodes/NodeDetailScreen.kt` | **ViewModel:** `MapViewModel` (współdzielony)

Pełny widok informacji o wybranym węźle. Ekran szczegółów — brak dolnego paska nawigacyjnego.

#### Nagłówek (`TopAppBar`)

Tytuł: nazwa węzła. Przycisk ← powrót do listy węzłów.

#### Karty informacyjne (przewijana kolumna)

**Karta nagłówkowa:**
- Pełna nazwa węzła + skrócona (shortName)
- Badge ONLINE (zielony) / OFFLINE (szary)
- Node ID w formacie monospace

**Karta Tożsamość:**
- Rola węzła (CLIENT, ROUTER, ROUTER_CLIENT, REPEATER, TRACKER, SENSOR, TAK)
- Model sprzętu
- Flaga licencji HAM
- Numer kanału
- Liczba przeskoków (bezpośrednio / N przeskoki)

**Karta Sygnał:**
- SNR z opisem jakości (Doskonały ≥5 dB / Dobry ≥0 / Słaby ≥-5 / Bardzo słaby)
- RSSI w dBm
- Poziom baterii % + pasek `LinearProgressIndicator` (zielony/pomarańczowy/czerwony)

**Karta Statystyki odbioru:**
- Liczba odebranych pakietów
- Średni Δt między pakietami (s), min/max Δt
- PDR (Packet Delivery Rate) % — stosunek odebranych pakietów do oczekiwanych przy zadanym interwale broadcastu; pasek wizualny
- Oczekiwany interwał broadcastu (z ustawień)
- Przycisk "Resetuj statystyki" z potwierdzeniem w `AlertDialog`

**Karta Pozycja GPS** (gdy dostępna):
- Szerokość/długość geograficzna (6 miejsc po przecinku)
- Wysokość n.p.m.
- Liczba widocznych satelitów
- Prędkość (m/s) i kierunek (° z nazwą kardynalną: N/NE/E/SE/S/SW/W/NW)
- Dokładność pozycji szacowana z `precisionBits` (metry)
- Wiek ostatniego odczytu GPS

**Karta Ostatni kontakt:**
- Data i czas w formacie `yyyy-MM-dd HH:mm:ss`
- Względny wiek: Ns / N min / Nh Nmin / N dni

**Karta Historia pozycji** (gdy historia niepusta):
- Nagłówek zawiera całkowity dystans trasy (km)
- Ostatnie 10 punktów w układzie: timestamp | lat, lon (format monospace)
- Wzmianka "… i N wcześniejszych" gdy historia > 10 punktów

#### Przyciski akcji

| Przycisk | Warunek widoczności | Efekt |
|----------|--------------------|----|
| Pokaż na mapie | Węzeł ma pozycję GPS | Przejście do MapScreen, kamera animuje do węzła |
| Udostępnij | Zawsze | `Intent.ACTION_SEND` z tekstem: nazwa, ID, koord., link Google Maps |
| Eksportuj dane CSV | Historia niepusta | `Intent.ACTION_SEND` z plikiem CSV (pozycje węzła) |

---

### Ekran 4 — Szczegóły strefy (`ZoneDetailScreen`)

**Plik:** `ui/zones/ZoneDetailScreen.kt` | **ViewModel:** `ZoneViewModel`

Widok szczegółowy strefy geofencingu z logiem zdarzeń ENTER/EXIT. Ekran szczegółów — brak dolnego paska.

#### Nagłówek (`TopAppBar`)

Tytuł: nazwa strefy. Przycisk ← powrót do mapy. Przycisk 🗑 (czerwony) — czyszczenie logu, widoczny gdy log niepusty, z potwierdzeniem w `AlertDialog`.

#### Karta informacji o strefie

- Kolorowe kółko + nazwa strefy + chip "Aktywna" / "Nieaktywna" (klikalny — toggle)
- Liczba wierzchołków wielokąta
- Lista ID monitorowanych węzłów (lub "—")
- Łączna liczba zdarzeń w logu

#### Log zdarzeń (`LazyColumn`)

Każdy wiersz `ZoneEventRow` zawiera:
- Etykieta **▶ WEJŚCIE** (zielone tło `primaryContainer`) lub **◀ WYJŚCIE** (czerwone `errorContainer`)
- Nazwa węzła
- Timestamp w formacie `dd.MM HH:mm:ss`

Zdarzenia sortowane od najnowszego (Room `ORDER BY id DESC`). Przy pustym logu wyświetla informację jak dodać węzeł do strefy.

---

### Ekran 5 — Ustawienia (`SettingsScreen`)

**Plik:** `ui/settings/SettingsScreen.kt` | **ViewModel:** `SettingsViewModel`

Ekran konfiguracji aplikacji z sekcjami zgrupowanymi nagłówkami.

#### Sekcja: Połączenie

| Ustawienie | Opcje | Opis |
|-----------|-------|------|
| Interwał odświeżania | 5 / 10 / 30 / 60 s | Jak często ViewModel odpytuje `getNodes()` przez AIDL |
| Próg "online" | 5 / 10 / 30 min | Po jakim czasie bez słyszalności węzeł staje się "offline" |
| Oczekiwany interwał broadcastu | 15 / 30 / 60 / 120 s | Używany do kalkulacji PDR (pakiety/oczekiwane) |

#### Sekcja: Filtry domyślne

- Switch "Domyślnie: tylko online" — zaznaczony filtr "Tylko online" przy starcie aplikacji
- Switch "Domyślnie: tylko z GPS" — zaznaczony filtr "Ma GPS" przy starcie

#### Sekcja: Mapa

- Typ mapy — `SingleChoiceSegmentedButtonRow` z 4 opcjami: Normalny / Satelita / Teren / Hybryda
- Switch "Pokaż ślady wszystkich węzłów" — włącza jednoczesne rysowanie tras wszystkich węzłów na mapie

#### Sekcja: Historia pozycji

| Ustawienie | Opcje | Opis |
|-----------|-------|------|
| Max punktów historii | 10 / 50 / 100 / 200 | Limit punktów przechowywanych w pamięci per węzeł |
| Min. odległość nowego punktu | 0 / 10 / 20 / 50 / 100 m | Filtr odległości — nowy punkt dodawany tylko gdy węzeł przemieścił się o min. tę odległość |

#### Sekcja: Dane testowe

- **Eksportuj sesję do CSV** — eksportuje historię pozycji wszystkich węzłów przez `Intent.ACTION_SEND` (share sheet)
- **Wyczyść historię pozycji** — usuwa in-memory historię i statystyki pakietów wszystkich węzłów; wymaga potwierdzenia

#### Reset

**Przywróć domyślne** — resetuje wszystkie wartości DataStore do domyślnych; wymaga potwierdzenia.

---

### Nakładka: ConnectionStatusBar

**Plik:** `ui/components/ConnectionStatusBar.kt`

Pasek wyświetlany nad ekranami Mapa i Węzły (nie na Ustawieniach i nie na ekranach szczegółów).

| Stan | Kolor paska | Treść |
|------|------------|-------|
| Connected | Zielony (primary) | "Połączono z Meshtastic" + liczba węzłów |
| Connecting | Niebieski (secondary) + spinner | "Łączenie z Meshtastic..." |
| Reconnecting | Niebieski + spinner | "Ponowne łączenie za Ns..." (odlicza sekundy) |
| Disconnected | Czerwony (error) | Powód rozłączenia + przycisk "Ponów" |
| MeshtasticNotInstalled | Czerwony | "Meshtastic nie jest zainstalowane" + "Ponów" |

Przejścia między stanami animowane przez `AnimatedContent` (fade in/out).

---

### Nakładka: ZoneBottomSheet

**Plik:** `ui/zones/ZoneBottomSheet.kt`

`ModalBottomSheet` otwierany przyciskiem FAB na ekranie Mapy.

Zawiera listę wszystkich zdefiniowanych stref (`LazyColumn`, max 360 dp wysokości). Każdy wiersz strefy (`ZoneListItem`) pokazuje:
- Kolorowe kółko koloru strefy
- Nazwa + info (liczba wierzchołków, liczba monitorowanych węzłów)
- Switch aktywności (toggle inline)
- Przycisk 🗑 usunięcia strefy
- Przycisk → przejścia do `ZoneDetailScreen`

Na dole przyciski "Dodaj strefę" — zamyka sheet i aktywuje tryb rysowania (`DrawingState.Drawing`).

---

### Dialog: ZoneConfirmDialog

**Plik:** `ui/zones/ZoneConfirmDialog.kt`

Dialog wyświetlany po zamknięciu wielokąta (`DrawingState.Confirming`). Umożliwia:
- Wpisanie nazwy nowej strefy
- Wybranie koloru z 5 predefiniowanych (`Zone.PRESET_COLORS`)
- Wybór węzłów do monitorowania (checkboxy z listy aktualnych węzłów)

Po zatwierdzeniu: `ZoneViewModel.confirmZone()` → zapis do Room → `DrawingState.Idle`.

---

### Przepływ nawigacji

```
MainActivity
    └── MainScreen
           ├── [BottomNav: Mapa / Węzły / Ustawienia]
           │
           ├─ Screen.Map ──────────────────────────── MapScreen
           │                   ├── FAB → ZoneBottomSheet (overlay)
           │                   ├── FAB Zamknij → ZoneConfirmDialog (overlay)
           │                   └── ZoneBottomSheet "→" → Screen.ZoneDetail
           │
           ├─ Screen.List ─────────────────────────── NodeListScreen
           │                   └── tap NodeItem → Screen.NodeDetail
           │
           ├─ Screen.NodeDetail ───────────────────── NodeDetailScreen
           │                   ├── ← back → Screen.List
           │                   └── "Pokaż na mapie" → Screen.Map + selectNode()
           │
           ├─ Screen.ZoneDetail ───────────────────── ZoneDetailScreen
           │                   └── ← back → Screen.Map
           │
           └─ Screen.Settings ─────────────────────── SettingsScreen
```
