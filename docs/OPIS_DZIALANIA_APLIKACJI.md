## Działanie aplikacji mobilnej

### Integracja z aplikacją Meshtastic

Aplikacja MeshTracker nie komunikuje się bezpośrednio z urządzeniami radiowymi — korzysta z aplikacji Meshtastic zainstalowanej na tym samym telefonie, która pełni rolę pośrednika między sprzętem a aplikacjami trzecimi. Meshtastic eksponuje dwa mechanizmy IPC (ang. *Inter-Process Communication*): interfejs AIDL (ang. *Android Interface Definition Language*) umożliwiający bezpośrednie wywołania metod na działającym serwisie, oraz broadcasty Androida rozsyłane do wszystkich zainteresowanych aplikacji przy każdej zmianie stanu sieci. MeshTracker korzysta z obu jednocześnie, co zapewnia zarówno możliwość pobrania bieżącego stanu sieci na żądanie, jak i reaktywne powiadamianie o nowych danych bez konieczności aktywnego odpytywania.

Poniższy diagram przedstawia kompletny przepływ danych od chwili uruchomienia aplikacji do momentu pojawienia się węzła na mapie:

```
Aplikacja Meshtastic (com.geeksville.mesh)
        │
        ├─ [1] bindService(IMeshService) ──────────────────────────────►
        │                                                    MeshServiceManager
        │                                                    • getNodes()     ─────────────► [5] odświeżenie stanu
        │                                                    • connectionState()
        │                                                    • getMyNodeId()
        │
        └─ [2] Broadcast: ACTION_NODE_CHANGE ─────────────────────────►
                                                    MeshtasticBroadcastReceiver
                                                             │
                                                             │ [3] fromMeshtasticNodeInfo()
                                                             ▼
                                                       MeshNodeInfo
                                                             │
                                                             │ [4] onNodeChanged()
                                                             ▼
                                                    MeshServiceRepository
                                                             │ listener
                                                             ▼
                                                       MapViewModel
                                                     (_nodes: StateFlow)
                                                             │
                                                             │ collectAsState()
                                                             ▼
                                                        MapScreen
                                                    (markery na mapie)
```

Po uruchomieniu aplikacji `MapViewModel` inicjuje połączenie przez `MeshRepository.connect()`. `MeshServiceManager` wywołuje `bindService()` z akcją `com.geeksville.mesh.service.MeshService`, co powoduje powiązanie z działającym serwisem Meshtastic. Równolegle rejestrowany jest `MeshtasticBroadcastReceiver` nasłuchujący na trzy akcje: `com.geeksville.mesh.NODE_CHANGE` (aktualizacja węzła), `com.geeksville.mesh.MESH_CONNECTED` oraz `com.geeksville.mesh.MESH_DISCONNECTED`. Gdy połączenie zostanie nawiązane, ViewModel pobiera bieżącą listę węzłów przez AIDL i uruchamia cykliczne odświeżanie w odstępach konfigurowanych przez użytkownika. Każdy odebrany broadcast z kolei aktualizuje stan w czasie rzeczywistym bez oczekiwania na kolejny cykl odświeżania.

### Parsowanie danych z Meshtastic

Kluczowym wyzwaniem implementacyjnym okazała się kwestia dostępu do obiektów `NodeInfo` przekazywanych przez Meshtastic. Aplikacja Meshtastic nie udostępnia swojego SDK jako biblioteki — klasy modelu danych (`org.meshtastic.core.model.NodeInfo`, `MeshUser`, `Position`) są częścią jej prywatnego classpath i nie są dostępne bezpośrednio w projekcie zewnętrznym. Zdecydowałem się na zastosowanie mechanizmu **Java Reflection**, który pozwala odczytać wartości pól i wywołać metody obiektu w czasie wykonania programu, bez znajomości jego klasy na etapie kompilacji.

Pierwszym problemem jest samo dostarczenie obiektu `NodeInfo` z intentu broadcastu. Broadcasty są serializowane przez system Androida z użyciem domyślnego classpath aplikacji odbierającej — co oznacza, że `Bundle` zawierający `Parcelable` klasy Meshtastic nie może zostać zdeserializowany bez załadowania odpowiedniego `ClassLoader`. Rozwiązuję ten problem przez tymczasowe przestawienie `Thread.currentThread().contextClassLoader` na classloader pobrany z kontekstu pakietu Meshtastic:

```kotlin
val meshtasticContext = context.createPackageContext(
    "com.geeksville.mesh",
    Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
)
val meshtasticClassLoader = meshtasticContext.classLoader

val originalClassLoader = Thread.currentThread().contextClassLoader
try {
    Thread.currentThread().contextClassLoader = meshtasticClassLoader
    bundle.classLoader = meshtasticClassLoader
    // deserializacja NodeInfo...
} finally {
    Thread.currentThread().contextClassLoader = originalClassLoader
}
```

Sama konwersja obiektu `NodeInfo` na wewnętrzną klasę `MeshNodeInfo` odbywa się w metodzie `fromMeshtasticNodeInfo()`. Każde pole odczytywane jest przez Reflection z obsługą kilku wariantów nazw metod — wynika to ze zmian API między wersjami aplikacji Meshtastic. Poniższy fragment pokazuje odczyt wartości SNR, który kolejno próbuje metody `getSnr()`, pole `snr`, metodę `getRxSnr()` oraz pole `rxSnr`:

```kotlin
fun readSnr(nodeInfo: Any): Float {
    // próba 1: metoda getSnr()
    try {
        val v = nodeInfo.javaClass.getMethod("getSnr").invoke(nodeInfo)
        val f = when (v) {
            is Float  -> v
            is Double -> v.toFloat()
            is Number -> v.toFloat()
            else      -> null
        }
        if (f != null) return f
    } catch (_: NoSuchMethodException) { }

    // próba 2: pole 'snr'
    try {
        val field = nodeInfo.javaClass.getDeclaredField("snr")
        field.isAccessible = true
        val f = (field.get(nodeInfo) as? Number)?.toFloat()
        if (f != null) return f
    } catch (_: Exception) { }

    // próba 3–4: getRxSnr() / rxSnr (starsze wersje Meshtastic)
    // ...

    return Float.MAX_VALUE  // sentinel — brak danych SNR
}
```

Osobnym wyzwaniem jest format współrzędnych GPS. Meshtastic przechowuje szerokość i długość geograficzną jako liczby całkowite `Int` równe wartości w stopniach pomnożonej przez 10^7 (format E7). Odczyt przez `getLatitude()` może zwrócić zarówno już przeliczoną wartość `Double`, jak i surowy `Int` — aplikacja wykrywa format na podstawie zakresu wartości i w razie potrzeby dzieli przez 10^7:

```kotlin
val result = when (value) {
    is Double -> if (Math.abs(value) > 1000.0) value / 1e7 else value
    is Int    -> value / 1e7   // format E7 → stopnie dziesiętne
    is Long   -> value / 1e7
    else      -> (value as? Number)?.toDouble() ?: 0.0
}
```

### Wykrywanie przekroczenia strefy — algorytm ray-casting

Geofencing w MeshTracker oparty jest o algorytm **ray-casting** (pol. rzutowania promienia), który sprawdza, czy dany punkt geograficzny leży wewnątrz wielokąta zdefiniowanego przez użytkownika. Zasada działania algorytmu polega na przeprowadzeniu z badanego punktu poziomego promienia w kierunku wschodnim (w stronę rosnącej długości geograficznej) i zliczeniu liczby przecięć z krawędziami wielokąta. Nieparzysta liczba przecięć oznacza, że punkt leży wewnątrz; parzysta — na zewnątrz. Intuicja jest prosta: aby dostać się z punktu wewnętrznego na zewnątrz, trzeba zawsze przekroczyć nieparzystą liczbę granic.

Implementacja tej logiki w `GeofenceChecker.contains()` iteruje po parach sąsiadujących wierzchołków wielokąta. Dla każdej krawędzi sprawdzane są dwa warunki: czy krawędź przecina poziomą linię przebiegającą przez badany punkt (jeden wierzchołek powyżej, drugi poniżej lub na poziomie szerokości geograficznej punktu), oraz czy punkt przecięcia promienia z krawędzią leży na wschód od badanego punktu:

```kotlin
fun contains(pointLat: Double, pointLon: Double, polygon: List<ZoneVertex>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.lastIndex

    for (i in polygon.indices) {
        val vi = polygon[i]
        val vj = polygon[j]

        val crossesRay = (vi.lat <= pointLat && vj.lat > pointLat) ||
                         (vj.lat <= pointLat && vi.lat > pointLat)

        if (crossesRay) {
            // długość geograficzna przecięcia promienia z krawędzią
            val intersectLon = vi.lon +
                (pointLat - vi.lat) / (vj.lat - vi.lat) * (vj.lon - vi.lon)

            if (pointLon < intersectLon) inside = !inside
        }
        j = i
    }
    return inside
}
```

Algorytm działa poprawnie dla dowolnych wielokątów wklęsłych i wypukłych, nie wymaga triangulacji ani żadnego wstępnego przetwarzania — złożoność obliczeniowa wynosi O(n), gdzie n to liczba wierzchołków. Jedynym ograniczeniem jest brak obsługi wielokątów przecinających południk 180°, co w kontekście monitorowania psów na terenie Polski jest w praktyce nieistotne.

### Monitorowanie stref w tle

Wykrywanie przekroczeń strefy musi działać niezawodnie niezależnie od tego, czy użytkownik aktywnie korzysta z aplikacji. System Android agresywnie ogranicza działanie procesów w tle, dlatego całą logikę monitorowania umieściłem w `ZoneMonitorService` — komponencie typu **Android Foreground Service**. Serwis działający na pierwszym planie wyświetla stałe powiadomienie w pasku statusu (które informuje o liczbie aktywnych stref), co chroni go przed zatrzymaniem przez system zarządzania pamięcią nawet przy wygaszonym ekranie i zamkniętej aplikacji.

Cykl życia serwisu powiązany jest bezpośrednio ze stanem stref w bazie Room. `ZoneViewModel` obserwuje reaktywny `Flow<List<Zone>>` i uruchamia lub zatrzymuje serwis automatycznie:

```
Użytkownik definiuje strefę i zaznacza ją jako aktywną
        │
        ▼
ZoneRepository zapisuje do Room (INSERT/UPDATE)
        │
        ▼
Flow<List<Zone>> emituje nową listę stref
        │
        ├─ ZoneViewModel.collect → ZoneMonitorService.start(context)
        │
        └─ ZoneMonitorService.collect → activeZones = zones.filter { it.isActive }
                                               │
                                               └─ jeśli pusta → stopSelf()
```

Po uruchomieniu serwis rejestruje własną instancję `MeshtasticBroadcastReceiver`. Każde odebranie `ACTION_NODE_CHANGE` wyzwala sprawdzenie pozycji węzła względem wszystkich aktywnych stref za pomocą opisanego wcześniej algorytmu ray-casting. Kluczowym elementem jest mapa `previousNodeZoneMap: ConcurrentHashMap<String, Set<String>>`, która przechowuje ostatni znany stan przynależności każdego węzła do stref. Porównanie bieżącego stanu z poprzednim pozwala wykryć zdarzenia ENTER (węzeł pojawił się w strefie) i EXIT (węzeł opuścił strefę):

```kotlin
val currentZones = mutableSetOf<String>()
for (zone in activeZones) {
    if (nodeId !in zone.watchedNodeIds()) continue
    if (GeofenceChecker.contains(pos.latitude, pos.longitude, zone.vertices())) {
        currentZones.add(zone.id)
    }
}

val previousZones = previousNodeZoneMap[nodeId] ?: emptySet()

for (zoneId in currentZones - previousZones) {  // ENTER
    zoneRepository.recordEvent(zoneId, nodeId, nodeName, ZoneEventType.ENTER)
    fireAlertNotification(zone, nodeName, ZoneEventType.ENTER)
}
for (zoneId in previousZones - currentZones) {  // EXIT
    zoneRepository.recordEvent(zoneId, nodeId, nodeName, ZoneEventType.EXIT)
    fireAlertNotification(zone, nodeName, ZoneEventType.EXIT)
}

previousNodeZoneMap[nodeId] = currentZones
```

Zdarzenia zapisywane są do bazy Room z sygnaturą czasową, a powiadomienia push wysyłane przez `NotificationManagerCompat` z priorytetem `PRIORITY_HIGH`, co zapewnia ich wyświetlenie nawet przy wygaszonym ekranie.

### Wyświetlanie listy węzłów

Lista węzłów zaimplementowana jest jako `LazyColumn` z Jetpack Compose, który — analogicznie do `RecyclerView` — renderuje wyłącznie elementy aktualnie widoczne na ekranie, co zapewnia płynne przewijanie niezależnie od liczby węzłów. Dane wyświetlane na liście przepływają z `MapViewModel` przez reaktywny `StateFlow<List<MeshNodeInfo>>`, co oznacza, że każda aktualizacja pozycji lub stanu węzła powoduje automatyczne przerenderowanie tylko tych kart, których dane faktycznie się zmieniły.

Użytkownik może zawęzić widoczną listę za pomocą trzech mechanizmów filtrowania: pola wyszukiwania tekstowego (po nazwie lub ID węzła), chipa „Tylko online" (ukrywającego węzły milczące dłużej niż skonfigurowany próg) oraz chipa „Ma GPS" (ukrywającego węzły bez ważnej pozycji). Filtry są łączone koniunkcyjnie, a ich bieżący stan przechowywany jest w `NodeFilterState` — niemutowalnej klasie danych obserwowanej przez ViewModel. Poniższy fragment pokazuje logikę filtrowania zastosowaną w `MapViewModel`:

```kotlin
val filteredNodes: StateFlow<List<MeshNodeInfo>> = combine(
    _nodes, _filterState, onlineThresholdSeconds
) { nodesMap, filter, threshold ->
    nodesMap.values
        .filter { node ->
            val matchesQuery = filter.searchQuery.isEmpty() ||
                node.getDisplayName().contains(filter.searchQuery, ignoreCase = true) ||
                node.getId().contains(filter.searchQuery, ignoreCase = true)
            val matchesOnline = !filter.showOnlineOnly || node.isOnline(threshold)
            val matchesGps    = !filter.showWithGpsOnly || node.hasValidPosition()
            matchesQuery && matchesOnline && matchesGps
        }
        .sortedWith(
            compareBy<MeshNodeInfo> { !it.isOnline(threshold) }
                .thenBy { it.getDisplayName() }
        )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

Wynikowa lista sortowana jest w taki sposób, by węzły online zawsze pojawiały się przed offline, a w ramach każdej grupy — alfabetycznie według nazwy. Każdy element listy (`NodeItem`) wyświetla komplet informacji diagnostycznych dostępnych dla danego węzła: współrzędne GPS z wiekiem ostatniego odczytu, prędkość i kierunek ruchu, poziom naładowania baterii, siłę sygnału SNR oraz RSSI, a także czas ostatniego kontaktu. Elementy węzłów offline mają przyciemnione tło (`surfaceVariant`), co pozwala użytkownikowi na natychmiastową ocenę stanu sieci bez konieczności czytania etykiet statusu.
