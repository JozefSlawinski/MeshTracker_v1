# Architektura — MeshTracker v1

Dokument opisuje strukturę aplikacji, kluczowe decyzje projektowe oraz przepływ danych. Przeznaczony do użytku własnego jako punkt odniesienia podczas dalszego rozwoju.

---

## Kontekst i cel

MeshTracker to natywna aplikacja Android, która łączy się z aplikacją [Meshtastic](https://meshtastic.org/) i wyświetla pozycje węzłów sieci radiowej na interaktywnej mapie Google Maps. Aplikacja służy do śledzenia urządzeń Meshtastic w czasie rzeczywistym bez potrzeby stałego połączenia z Internetem — komunikacja radio LoRa odbywa się lokalnie.

Główny przypadek użycia: śledzenie psów wyposażonych w węzły Meshtastic na nieogrodzonej działce. Gdy pies opuści wyznaczoną strefę, właściciel jest natychmiast powiadamiany.

**Co robi aplikacja:**
- Wiąże się z serwisem `MeshService` aplikacji Meshtastic przez Android Service Binding
- Nasłuchuje broadcastów o zmianach węzłów (`NODE_CHANGE`, `MESH_CONNECTED`, `MESH_DISCONNECTED`)
- Wyświetla węzły jako markery na mapie Google Maps (z kierunkiem ruchu jeśli dostępny)
- Pokazuje listę wszystkich węzłów z metadanymi (bateria, SNR, RSSI, czas ostatniego kontaktu)
- Pozwala definiować strefy kołowe na mapie i przypisywać do nich węzły
- Wykrywa naruszenia stref i powiadamia użytkownika — również gdy aplikacja jest zamknięta

**Czego aplikacja nie robi (v1):**
- Nie wysyła wiadomości przez sieć Meshtastic
- Nie integruje się z ATAK (choć referencyjna implementacja ATAK jest w `docs/PRZYKLADOWY_KOD.java`)
- Nie przechowuje historii pozycji
- Nie obsługuje wielokątnych stref (tylko okręgi — v2)

---

## Struktura pakietów

```
com.example.meshtracker_v1/
├── MainActivity.kt                    # Punkt wejścia aplikacji
├── model/
│   ├── MeshNodeInfo.kt                # Wrapper węzła (via reflection)
│   ├── MeshUserInfo.kt                # Dane użytkownika węzła
│   └── Position.kt (MeshPosition)     # Pozycja GPS
├── service/
│   └── MeshServiceManager.kt          # Singleton — binding z IMeshService
├── receiver/
│   └── MeshtasticBroadcastReceiver.kt # Odbiornik broadcastów Meshtastic
├── mapper/
│   └── NodeToMarkerMapper.kt          # MeshNodeInfo → Google Maps MarkerOptions
├── util/
│   └── Constants.kt                   # Stałe — akcje broadcastów, pakiet Meshtastic
└── ui/
    ├── MainScreen.kt                  # Nawigacja (dolny pasek: Mapa / Lista)
    ├── map/
    │   ├── MapScreen.kt               # Ekran mapy Google Maps
    │   └── MapViewModel.kt            # Jedyny ViewModel — zarządza stanem całej aplikacji
    ├── nodes/
    │   ├── NodeListScreen.kt          # Lista węzłów
    │   └── NodeItem.kt                # Kafelek pojedynczego węzła
    ├── components/
    │   └── ConnectionStatusBar.kt     # Pasek stanu połączenia (góra ekranu)
    └── theme/
        ├── Theme.kt / Color.kt / Type.kt
```

---

## Warstwy architektury

Aplikacja stosuje wzorzec **MVVM** (Model-View-ViewModel) z reaktywnym stanem przez `StateFlow`.

```
┌─────────────────────────────────────────────┐
│                  UI Layer                   │
│  MainActivity → MainScreen                  │
│      ├── MapScreen (Google Maps)            │
│      └── NodeListScreen                     │
│  Czyta StateFlow z ViewModelu               │
└──────────────────┬──────────────────────────┘
                   │ collectAsState()
┌──────────────────▼──────────────────────────┐
│              ViewModel Layer                │
│  MapViewModel (AndroidViewModel)            │
│  • nodes: StateFlow<Map<String,MeshNodeInfo>>│
│  • connectionState: StateFlow<ConnectionState>│
│  • Logika ponownego łączenia (backoff)       │
└────────────┬──────────────┬─────────────────┘
             │              │
┌────────────▼──────┐  ┌────▼─────────────────┐
│   Service Layer   │  │   Receiver Layer      │
│ MeshServiceManager│  │ MeshtasticBroadcast-  │
│ (Singleton)       │  │ Receiver              │
│ Binding do        │  │ Nasłuchuje broadcastów│
│ IMeshService      │  │ NODE_CHANGE itp.      │
└────────────┬──────┘  └────┬─────────────────┘
             │              │
             └──────┬───────┘
                    │
┌───────────────────▼─────────────────────────┐
│              Model Layer                    │
│  MeshNodeInfo / MeshUserInfo / MeshPosition │
│  fromMeshtasticNodeInfo() via Reflection    │
└─────────────────────────────────────────────┘
```

---

## Kluczowe komponenty

### MapViewModel

Centralny punkt aplikacji. Implementuje `MeshtasticBroadcastReceiver.MeshtasticReceiverListener`, więc bezpośrednio odbiera zdarzenia z receivera.

**Stan wewnętrzny:**
- `_nodes: MutableStateFlow<Map<String, MeshNodeInfo>>` — słownik węzłów po ID
- `_connectionState: MutableStateFlow<ConnectionState>` — aktualny stan połączenia
- `_selectedNodeId: MutableStateFlow<String?>` — wybrany węzeł (do animacji kamery)
- `nodesMutex: Mutex` — chroni zapis do `_nodes` z wielu coroutine

**Cykl życia połączenia:**

```
init()
  ├── setupServiceListener()   ← rejestruje ConnectionListener w MeshServiceManager
  ├── registerReceiver()       ← rejestruje BroadcastReceiver
  └── attemptConnect()
        ├── isMeshtasticInstalled? → NIE → ConnectionState.MeshtasticNotInstalled
        └── TAK → meshServiceManager.connect()
              ├── SUKCES → onServiceConnected()
              │     ├── radioState == CONNECTED → onFullyConnected()
              │     └── inaczej → startConnectionCheck() (polling co 2s)
              └── BŁĄD → scheduleReconnect() z exponential backoff
```

**Exponential backoff:**
Opóźnienie między próbami = `min(2000ms × 2^attempt, 60000ms)`. Odliczanie jest widoczne w UI przez `ConnectionState.Reconnecting(retryInSeconds)`.

**`onCleared()`** wywoływane przez Androida gdy ViewModel jest niszczony: anuluje coroutines, wyrejestrowuje receiver, rozłącza serwis.

---

### MeshServiceManager

Singleton zarządzający jednym `ServiceConnection` do `com.geeksville.mesh.service.MeshService`.

**Kluczowy problem — brak AIDL na etapie kompilacji:**
Aplikacja Meshtastic eksportuje interfejs `IMeshService` przez AIDL, ale pliki `.aidl` nie są dostępne jako biblioteka Maven. Zamiast dołączać AIDL lub całą aplikację Meshtastic jako zależność, `MeshServiceManager` używa **Java Reflection**:

```kotlin
// W onServiceConnected():
val stubClass = Class.forName("org.meshtastic.core.service.IMeshService\$Stub")
val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
meshService = asInterfaceMethod.invoke(null, service) // zwraca Any?

// Wywołanie metody:
val getNodesMethod = meshService?.javaClass?.getMethod("getNodes")
val nodes = getNodesMethod?.invoke(meshService) as? List<*>
```

Zaleta: brak zależności kompilacyjnej od Meshtastic.  
Wada: błędy wykrywane w runtime, a nie w czasie kompilacji. Nazwy metod mogą zmienić się po aktualizacji Meshtastic.

**Sprawdzanie instalacji:**
Przed próbą bindingu `isMeshtasticInstalled()` sprawdza przez `PackageManager` czy pakiet `com.geeksville.mesh` jest zainstalowany. Jeśli nie — UI pokazuje odpowiedni komunikat bez próby połączenia.

---

### MeshtasticBroadcastReceiver

Odbiera trzy broadcasty systemowe z aplikacji Meshtastic:

| Akcja | Znaczenie |
|---|---|
| `com.geeksville.mesh.NODE_CHANGE` | Węzeł pojawił się, zmienił pozycję lub zniknął |
| `com.geeksville.mesh.MESH_CONNECTED` | Radio Meshtastic połączyło się |
| `com.geeksville.mesh.MESH_DISCONNECTED` | Radio Meshtastic rozłączyło się |

**Problem ClassLoader przy deserializacji `NodeInfo`:**

`NodeInfo` to klasa `Parcelable` z pakietu `org.meshtastic.core.model`. Kiedy Android deserializuje `Intent.extras`, używa domyślnego ClassLoadera aplikacji — który nie zna tej klasy. Powoduje to `ClassNotFoundException` przy `getParcelableExtra()`.

Rozwiązanie zastosowane w `handleNodeChange()`:

```kotlin
// 1. Wczytaj ClassLoader z aplikacji Meshtastic
val meshtasticContext = context.createPackageContext(
    MESHTASTIC_PACKAGE,
    CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY
)
val meshtasticClassLoader = meshtasticContext.classLoader

// 2. Ustaw go jako Thread context ClassLoader PRZED dostępem do Bundle
Thread.currentThread().contextClassLoader = meshtasticClassLoader
bundle.classLoader = meshtasticClassLoader

// 3. Pobierz NodeInfo z Bundle
val nodeInfoClass = meshtasticClassLoader.loadClass("org.meshtastic.core.model.NodeInfo")
val nodeInfo = bundle.getParcelable("com.geeksville.mesh.NodeInfo", nodeInfoClass)
```

Blok `finally` przywraca oryginalny ClassLoader. Cała procedura jest otoczona obsługą wyjątków — jeśli się nie uda, receiver loguje dostępne klucze extras dla debugowania.

---

### MeshNodeInfo i modele danych

`MeshNodeInfo` to wewnętrzna klasa Kotlin będąca "bezpiecznym opakowaniem" dla obiektu `Any?` zwróconego z Meshtastic. Parsowanie odbywa się przez reflection w `fromMeshtasticNodeInfo()`:

```
org.meshtastic.core.model.NodeInfo (Any?)
    ↓ reflection
MeshNodeInfo(
    num: Int,           // numer węzła
    user: MeshUserInfo, // id, longName, shortName, hwModel, role
    position: MeshPosition, // lat, lng, alt, speed, heading, precisionBits, time
    snr, rssi, lastHeard, batteryLevel, channel, hopsAway
)
```

**`MeshPosition.isValid()`** — pozycja jest uznawana za prawidłową gdy `lat != 0.0 || lng != 0.0` oraz mieści się w zakresach `[-90, 90]` i `[-180, 180]`.

**`MeshNodeInfo.isOnline()`** — węzeł jest "online" jeśli `lastHeard` jest w ciągu ostatnich 300 sekund (5 minut).

---

### MapScreen — wizualizacja na mapie

**Markery:** Każdy węzeł z prawidłową pozycją otrzymuje marker. Typ markera zależy od dostępności danych kierunku ruchu (`groundTrack`):

- `groundTrack > 0` → strzałka narysowana przez `Canvas` API, obrócona o kąt `heading`. Ikony są cachowane w `Map<Triple<Int, Int, Boolean>, BitmapDescriptor>` gdzie klucz to `(heading, kolor, zaznaczony)`.
- `groundTrack == 0` → standardowy pin Google Maps z kolorem zależnym od roli węzła.

**Kolory ról:**

| Rola (int) | Kolor | Meshtastic role |
|---|---|---|
| 0 | Czerwony | CLIENT |
| 5 | Zielony | TRACKER |
| inne | Niebieski | Pozostałe |

**Kamera:** Domyślna pozycja to centrum Polski (`52.0, 19.0`, zoom 6). Gdy użytkownik kliknie węzeł na liście, kamera animuje się do jego pozycji z zoom 15.

---

## Przepływ danych

```
[Urządzenie Meshtastic] ←→ (LoRa radio) ←→ [Aplikacja Meshtastic]
                                                      │
                                              MeshService (Android Service)
                                                      │
                      ┌───────────────────────────────┤
                      │ bindService()                 │ broadcast intent
                      ▼                               ▼
              MeshServiceManager              MeshtasticBroadcastReceiver
              .getNodes()                     .onReceive(ACTION_NODE_CHANGE)
              (via reflection)                │
                      │                       │ ClassLoader dance
                      │                       │ NodeInfo → MeshNodeInfo
                      └──────────┬────────────┘
                                 ▼
                           MapViewModel
                           _nodes.value[id] = MeshNodeInfo
                                 │
                                 │ StateFlow
                                 ▼
                      ┌──────────────────────┐
                      │  MapScreen           │  ─→ Google Maps markers
                      │  NodeListScreen      │  ─→ LazyColumn z kartami
                      │  ConnectionStatusBar │  ─→ Stan połączenia
                      └──────────────────────┘
```

---

## Kluczowe decyzje i kompromisy

### 1. Reflection zamiast AIDL
**Dlaczego:** Brak dostępu do plików AIDL Meshtastic jako publicznej zależności Maven.  
**Koszt:** Błędy wykrywane w runtime. Przy aktualizacji Meshtastic nazwy metod mogą się zmienić bez ostrzeżenia kompilatora.  
**Alternatywa:** Dołączyć Meshtastic jako lokalną zależność AAR lub skonfigurować AIDL ręcznie — ale wymaga utrzymywania kopii plików AIDL.

### 2. ClassLoader do deserializacji Parcelable
**Dlaczego:** Android nie potrafi zdeserializować `NodeInfo` bez ClassLoadera z pakietu `com.geeksville.mesh`.  
**Koszt:** Skomplikowany kod z obsługą wyjątków. Wymaga `CONTEXT_INCLUDE_CODE` — jeśli Meshtastic zmieni eksportowanie klas, przestanie działać.  
**Alternatywa:** Konwertować dane po stronie Meshtastic i wysyłać prostsze typy (String, int) w broadcastach — ale wymaga modyfikacji Meshtastic.

### 3. Jeden ViewModel dla obu ekranów
**Dlaczego:** Mapa i lista węzłów współdzielą ten sam zestaw danych (`nodes`, `connectionState`). Jeden ViewModel eliminuje synchronizację stanu.  
**Koszt:** `MapViewModel` jest odpowiedzialny za zbyt wiele (połączenie, stan węzłów, zaznaczenie). W przyszłości warto wydzielić `ConnectionViewModel` i `NodeRepository`.

### 4. Google Maps zamiast ATAK
**Dlaczego:** Wersja v1 to samodzielna aplikacja — prostsze środowisko testowe, nie wymaga instalacji ATAK.  
**Kontekst:** `docs/PRZYKLADOWY_KOD.java` zawiera gotową implementację wysyłania pozycji jako CoT Events do ATAK — do użycia gdy aplikacja będzie rozwijana jako ATAK plugin.

### 5. `StateFlow` + Compose
**Dlaczego:** Naturalna integracja z Jetpack Compose przez `collectAsState()`. Reaktywna aktualizacja UI przy każdej zmianie węzłów bez ręcznego zarządzania odświeżaniem.

---

## Wymagania środowiskowe

**Hardware:** Urządzenie Meshtastic (np. Heltec LoRa 32, T-Beam, RAK WisNode) podłączone do smartfona przez Bluetooth lub USB.

**Software:**
- Android 7.0+ (minSdk 24), targetSdk 36
- Aplikacja Meshtastic (`com.geeksville.mesh`) zainstalowana i połączona z urządzeniem
- Klucz API Google Maps w `local.properties` pod kluczem `MAPS_API_KEY`

**Uprawnienia:**
- `INTERNET` — Google Maps SDK
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — wyświetlanie własnej lokalizacji na mapie
- `<queries>` dla pakietu `com.geeksville.mesh` — wymagane przez Android 11+ (Package Visibility)

**Budowanie:**
```bash
# Dodaj do local.properties:
MAPS_API_KEY=twoj_klucz_api

./gradlew assembleDebug
```

---

## Znane ograniczenia i obszary do poprawy

**Reflection i ClassLoader:**
- Kod jest kruchy względem zmian API Meshtastic. Warto napisać testy integracyjne z mock serwisem.
- Logi debugowania w `MeshNodeInfo.fromMeshtasticNodeInfo()` są bardzo szczegółowe — wyczyścić przed wersją produkcyjną.

**Brak persystencji:**
- Węzły znikają po restarcie aplikacji. Rozważyć Room DB lub DataStore do cache'owania ostatnio widzianych węzłów.

**Jeden ViewModel:**
- `MapViewModel` zarządza i połączeniem, i danymi węzłów. Wydzielenie `NodeRepository` jako pośrednika ułatwi testowanie jednostkowe.

**Brak testów:**
- Istnieją tylko szablonowe `ExampleUnitTest` i `ExampleInstrumentedTest`. Priorytet: testy dla `MeshNodeInfo.fromMeshtasticNodeInfo()` z mock danymi.

**Cache ikon:**
- `iconCache` w `MapScreen` to `remember { mutableMapOf() }` — nie jest czyszczony. Przy wielu węzłach z różnymi kursami może rosnąć w nieskończoność. Dodać `LruCache`.

---

## Geofencing — architektura (planowana)

### Cel i decyzje projektowe

Użytkownik definiuje **strefy kołowe** na mapie i przypisuje do nich węzły (1 węzeł = 1 strefa). Gdy węzeł opuści swoją strefę, aplikacja wykrywa naruszenie i powiadamia użytkownika — również gdy aplikacja jest zamknięta.

Kluczowe decyzje:
- **Kształt strefy:** tylko okrąg (v1). Wystarczający dla działki z marginesem, najprostszy w obsłudze dotykowej i obliczeniach. Wielokąty — v2.
- **Model przypisania:** 1 węzeł = 1 strefa. Zmiana strefy nadpisuje poprzednie przypisanie.
- **Detekcja w tle:** wymagana nawet gdy aplikacja jest zamknięta → `ForegroundService`.
- **Debounce:** smart — agresywny polling po pierwszym podejrzanym odczycie, powiadomienie po potwierdzeniu. Czas reakcji ≤ 15 s.

### Nowe komponenty

```
com.example.meshtracker_v1/
├── service/
│   └── MeshTrackerForegroundService.kt   # Hostuje BroadcastReceiver i BreachDetector w tle
├── geofence/
│   ├── BreachDetector.kt                 # Stateful per-node; zarządza licznikiem i pollingiem
│   ├── GeofenceChecker.kt                # Utility: isOutside(lat, lng, zone): Boolean (Haversine)
│   ├── PositionRequester.kt              # requestPosition(nodeNum) via IMeshService reflection
│   └── BreachNotificationManager.kt      # Tworzy/aktualizuje/anuluje powiadomienia systemowe
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt                # Room DB
│   │   ├── ZoneDao.kt
│   │   ├── ZoneAssignmentDao.kt
│   │   └── BreachStateDao.kt
│   ├── entity/
│   │   ├── ZoneEntity.kt
│   │   ├── ZoneAssignmentEntity.kt
│   │   └── BreachStateEntity.kt
│   └── ZoneRepository.kt                 # Źródło prawdy; eksponuje Flow do UI i Service
└── ui/
    ├── zones/
    │   ├── ZoneListScreen.kt             # Trzecia zakładka nawigacji
    │   └── ZoneViewModel.kt              # CRUD stref, przypisania węzłów
    └── map/
        └── ZoneCreationOverlay.kt        # Nakładka na MapScreen: okrąg + uchwyt promienia
```

### Schemat Room DB

**`zones`**

| Kolumna | Typ | Opis |
|---|---|---|
| `id` | `INTEGER PK AUTOINCREMENT` | |
| `name` | `TEXT NOT NULL` | Nazwa strefy, np. "Działka Kowalskich" |
| `center_lat` | `REAL NOT NULL` | |
| `center_lng` | `REAL NOT NULL` | |
| `radius_meters` | `REAL NOT NULL` | |
| `color_hex` | `TEXT NOT NULL` | Kolor okręgu na mapie |
| `created_at` | `INTEGER NOT NULL` | Epoch seconds |

**`zone_assignments`**

| Kolumna | Typ | Opis |
|---|---|---|
| `node_id` | `TEXT PRIMARY KEY` | ID węzła (klucz główny — 1 węzeł = 1 strefa) |
| `zone_id` | `INTEGER NOT NULL FK→zones(id) ON DELETE CASCADE` | |
| `assigned_at` | `INTEGER NOT NULL` | |

**`breach_states`**

| Kolumna | Typ | Opis |
|---|---|---|
| `node_id` | `TEXT PRIMARY KEY` | |
| `is_in_breach` | `INTEGER NOT NULL` | 0 = w strefie, 1 = poza strefą |
| `breach_start` | `INTEGER` | Epoch seconds, nullable |
| `last_lat` | `REAL` | Ostatnia znana pozycja poza strefą |
| `last_lng` | `REAL` | |

### Smart debounce — algorytm

Gdy węzeł po raz pierwszy pojawi się poza strefą, `BreachDetector` uruchamia **agresywny polling** przez `requestPosition(nodeNum)` co 5 s. Powiadomienie jest wysyłane po 3 kolejnych odczytach poza strefą (lub po 2 odczytach jeśli upłynęło ≥ 15 s — failsafe).

```
T = 0 s   → 1. odczyt poza strefą → start pollingu (requestPosition co 5 s)
T ≈ 5 s   → 2. odczyt poza strefą
T ≈ 10 s  → 3. odczyt poza strefą → BREACH CONFIRMED → powiadomienie
```

Łączny czas reakcji: ~10 s normalnie, max 15 s (failsafe). Mieści się w wymaganiu.

Reset: jeśli którykolwiek odczyt wróci do strefy — licznik zerowany, polling zatrzymywany.

Pseudokod `BreachDetector.onPositionUpdate()`:
```kotlin
fun onPositionUpdate(node: MeshNodeInfo, zone: ZoneEntity) {
    val outside = GeofenceChecker.isOutside(node.position, zone)

    if (!outside) {
        if (suspiciousCount > 0 || currentState == BREACH) {
            resetSuspicion()                      // polling off, licznik = 0
            if (currentState == BREACH) notifyReturn(node, zone)
            setState(INSIDE)
        }
        return
    }

    // węzeł poza strefą
    suspiciousCount++
    if (suspiciousCount == 1) startAggressivePolling(node.num)  // requestPosition co 5s

    val timeElapsed = now() - suspiciousStartTime
    if (suspiciousCount >= 3 || (suspiciousCount >= 2 && timeElapsed >= 15_000)) {
        stopAggressivePolling()
        confirmBreach(node, zone)   // zapisz do Room DB + wyślij powiadomienie
    }
}
```

### ForegroundService — cykl życia

`MeshTrackerForegroundService` startuje przy uruchomieniu aplikacji (`Application.onCreate()`) i działa do czasu jawnego wyłączenia przez użytkownika (opcja w ustawieniach) lub odinstalowania aplikacji.

Serwis przejmuje odpowiedzialność za:
- Binding do `IMeshService` (dotychczas w `MapViewModel`)
- Rejestrację `MeshtasticBroadcastReceiver`
- Wywołania `BreachDetector` przy każdym `NODE_CHANGE`

`MapViewModel` przestaje zarządzać połączeniem bezpośrednio. Zamiast tego czyta stan węzłów i naruszenia przez `Flow` z `ZoneRepository` (Room DB) — serwis zapisuje, UI czyta reaktywnie.

### Powiadomienia — dwa kanały

**`CHANNEL_SERVICE`** (`IMPORTANCE_LOW`) — cichy status serwisu, zawsze widoczny w szufladzie:
> "MeshTracker aktywny · 2 węzły monitorowane · 0 poza strefą"

**`CHANNEL_BREACH`** (`IMPORTANCE_HIGH`) — alarm naruszenia:
> "Burek opuścił strefę! · Działka Kowalskich · 3 min temu"
- Heads-up banner (zajmuje górę ekranu przy odblokowanym telefonie)
- Dźwięk + wibracja
- `ongoing = true` — nie można usunąć przeciągnięciem, znika gdy węzeł wróci
- Akcja "Pokaż na mapie" — otwiera aplikację z wyśrodkowaną kamerą na węźle
- Akcja "Wycisz na 1h" — tymczasowe wyciszenie dla danego węzła

### Stan naruszenia w UI

Po potwierdzeniu breachState w Room DB, UI reaguje automatycznie przez `Flow`:
- Marker na mapie zmienia się z normalnego (zielony/czerwony/niebieski) na **czerwony z ikoną ostrzeżenia**
- Strefa na mapie zmienia obramowanie na **czerwone**
- Pod `ConnectionStatusBar` pojawia się **sticky banner**: "Burek jest poza strefą 'Działka Kowalskich' · Od 14 min"
- `NodeItem` na liście pokazuje **czerwoną plakietkę "POZA STREFĄ"**
- Banner i plakietka znikają gdy węzeł wróci (automatycznie przez Flow)

### UX tworzenia strefy

1. Użytkownik długo przytrzymuje punkt na mapie → pojawia się okrąg z domyślnym promieniem 100 m
2. Przeciąga uchwyt na krawędzi okręgu → dostosowuje promień (wyświetlany w metrach)
3. Tapy przycisku "Zapisz" → bottom sheet z polem nazwy + wyborem koloru
4. Opcjonalnie: od razu przypisanie węzła(ów) do nowo utworzonej strefy

### Ryzyko: `requestPosition()` przez reflection

`PositionRequester` wywołuje `IMeshService.requestPosition(Int)` przez reflection. Metoda istnieje w aktualnych wersjach Meshtastic, ale jej nazwa może różnić się między wersjami.

Zabezpieczenie: przy starcie `MeshTrackerForegroundService` wykonać **discovery** — zalogować wszystkie dostępne metody IMeshService i sprawdzić obecność `requestPosition`. Jeśli metoda niedostępna — fallback: debounce oparty wyłącznie na naturalnych broadcastach `NODE_CHANGE` z licznikiem ≥ 2 odczytów poza strefą.

### Kolejność implementacji

1. Room DB + `ZoneRepository` (encje, DAO, podstawowy CRUD + Flow)
2. UI tworzenia stref (`ZoneCreationOverlay` + `ZoneListScreen`)
3. `GeofenceChecker` + testy jednostkowe
4. `MeshTrackerForegroundService` (samo przeżycie w tle, bez detekcji)
5. `BreachDetector` + `BreachNotificationManager`
6. `PositionRequester` z discovery `requestPosition()`
7. Aktualizacja UI: markery, banner, `NodeItem` plakietka
8. Refaktoryzacja `MapViewModel` — oddanie połączenia serwisowi
