# Plan Implementacji - MeshTracker v1

## Przegląd projektu

Aplikacja Android do śledzenia i wyświetlania pozycji węzłów sieci Meshtastic na mapie. Aplikacja łączy się z aplikacją Meshtastic przez interfejs IMeshService i wyświetla pozycje węzłów w czasie rzeczywistym.

## Architektura systemu

```
Meshtastic Device (Radio)
    ↓
Meshtastic Android App (com.geeksville.mesh)
    ↓
IMeshService (AIDL Interface)
    ↓
MeshServiceManager (Service Binding)
    ↓
BroadcastReceiver (ACTION_NODE_CHANGE)
    ↓
NodeInfo → Position → Map Marker
    ↓
Google Maps / OpenStreetMap (wyświetlenie)
```

## Faza 1: Przygotowanie infrastruktury

### 1.1 Konfiguracja zależności

**Plik: `app/build.gradle.kts`**

- [ ] Dodać zależność do Google Maps (lub OpenStreetMap)
- [ ] Dodać zależność do AIDL (jeśli potrzebna)
- [ ] Dodać zależność do Location Services (opcjonalnie)
- [ ] Skonfigurować klucz API dla Google Maps (jeśli używane)

**Szacowany czas:** 30 minut

### 1.2 Konfiguracja AndroidManifest.xml

**Plik: `app/src/main/AndroidManifest.xml`**

- [ ] Dodać `<queries>` z pakietem `com.geeksville.mesh`
- [ ] Dodać uprawnienia do lokalizacji (jeśli potrzebne)
- [ ] Dodać uprawnienia do internetu (dla map)
- [ ] Skonfigurować klucz API Google Maps (jeśli używane)

**Szacowany czas:** 15 minut

### 1.3 Utworzenie struktury pakietów

**Struktura katalogów:**
```
app/src/main/java/com/example/meshtracker_v1/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   ├── map/
│   │   ├── MapScreen.kt
│   │   └── MapViewModel.kt
│   └── nodes/
│       ├── NodeListScreen.kt
│       └── NodeItem.kt
├── service/
│   ├── MeshServiceManager.kt
│   └── MeshServiceConnection.kt
├── receiver/
│   └── MeshtasticBroadcastReceiver.kt
├── model/
│   ├── NodeInfo.kt (wrapper/adapter)
│   ├── Position.kt (wrapper/adapter)
│   └── MeshUser.kt (wrapper/adapter)
├── util/
│   └── Constants.kt
└── mapper/
    └── NodeToMarkerMapper.kt
```

**Szacowany czas:** 20 minut

## Faza 2: Implementacja warstwy komunikacji z Meshtastic

### 2.1 Utworzenie interfejsu AIDL

**Plik: `app/src/main/aidl/org/meshtastic/core/service/IMeshService.aidl`**

- [ ] Skopiować interfejs AIDL z dokumentacji lub utworzyć na podstawie specyfikacji
- [ ] Zdefiniować metody:
  - `List<NodeInfo> getNodes()`
  - `MyNodeInfo getMyNodeInfo()`
  - `String getMyId()`
  - `String connectionState()`

**Szacowany czas:** 30 minut

### 2.2 Implementacja Constants

**Plik: `app/src/main/java/com/example/meshtracker_v1/util/Constants.kt`**

- [ ] Zdefiniować stałe dla akcji broadcastów:
  - `ACTION_NODE_CHANGE = "com.geeksville.mesh.NODE_CHANGE"`
  - `ACTION_MESH_CONNECTED = "com.geeksville.mesh.MESH_CONNECTED"`
  - `ACTION_MESH_DISCONNECTED = "com.geeksville.mesh.MESH_DISCONNECTED"`
- [ ] Zdefiniować stałe dla extras:
  - `EXTRA_NODE_INFO = "com.geeksville.mesh.NodeInfo"`
  - `EXTRA_CONNECTED = "com.geeksville.mesh.Connected"`
- [ ] Zdefiniować stałe dla stanów:
  - `STATE_CONNECTED = "CONNECTED"`
  - `STATE_DISCONNECTED = "DISCONNECTED"`

**Szacowany czas:** 15 minut

### 2.3 Implementacja MeshServiceManager

**Plik: `app/src/main/java/com/example/meshtracker_v1/service/MeshServiceManager.kt`**

**Funkcjonalność:**
- [ ] Singleton pattern
- [ ] Metoda `getInstance(context: Context): MeshServiceManager`
- [ ] Metoda `connect(): Boolean` - łączenie z serwisem
- [ ] Metoda `disconnect(): Unit` - rozłączanie
- [ ] Metoda `isConnected(): Boolean` - sprawdzanie stanu
- [ ] Metoda `getNodes(): List<NodeInfo>?` - pobieranie listy węzłów
- [ ] Metoda `getMyNodeInfo(): MyNodeInfo?` - informacje o własnym węźle
- [ ] Metoda `getMyNodeID(): String?` - ID własnego węzła
- [ ] Interface `ConnectionListener` z metodami:
  - `onServiceConnected()`
  - `onServiceDisconnected()`
- [ ] Metoda `setConnectionListener(listener: ConnectionListener)`
- [ ] Obsługa `ServiceConnection` i `bindService()`
- [ ] Obsługa wyjątków `RemoteException`

**Szacowany czas:** 2-3 godziny

### 2.4 Implementacja BroadcastReceiver

**Plik: `app/src/main/java/com/example/meshtracker_v1/receiver/MeshtasticBroadcastReceiver.kt`**

**Funkcjonalność:**
- [ ] Klasa dziedzicząca po `BroadcastReceiver`
- [ ] Metoda `onReceive(context: Context, intent: Intent)`
- [ ] Obsługa akcji:
  - `ACTION_NODE_CHANGE` - wywołanie callback z `NodeInfo`
  - `ACTION_MESH_CONNECTED` - wywołanie callback połączenia
  - `ACTION_MESH_DISCONNECTED` - wywołanie callback rozłączenia
- [ ] Interface `MeshtasticReceiverListener` z metodami:
  - `onNodeChanged(nodeInfo: NodeInfo)`
  - `onMeshConnected()`
  - `onMeshDisconnected()`
- [ ] Walidacja danych z intent

**Szacowany czas:** 1-2 godziny

## Faza 3: Implementacja modeli danych

### 3.1 Wrapper dla NodeInfo

**Plik: `app/src/main/java/com/example/meshtracker_v1/model/NodeInfo.kt`**

- [ ] Klasa data class lub wrapper dla `org.meshtastic.core.model.NodeInfo`
- [ ] Metody pomocnicze:
  - `hasValidPosition(): Boolean`
  - `isOnline(): Boolean`
  - `getDisplayName(): String`

**Szacowany czas:** 30 minut

### 3.2 Wrapper dla Position

**Plik: `app/src/main/java/com/example/meshtracker_v1/model/Position.kt`**

- [ ] Klasa data class lub wrapper dla `org.meshtastic.core.model.Position`
- [ ] Metody pomocnicze:
  - `isValid(): Boolean` - sprawdza czy pozycja jest prawidłowa (nie 0,0)
  - `isInRange(): Boolean` - sprawdza czy w prawidłowym zakresie
  - `toLatLng(): LatLng` - konwersja do Google Maps LatLng

**Szacowany czas:** 30 minut

### 3.3 Wrapper dla MeshUser

**Plik: `app/src/main/java/com/example/meshtracker_v1/model/MeshUser.kt`**

- [ ] Klasa data class lub wrapper dla `org.meshtastic.core.model.MeshUser`
- [ ] Metody pomocnicze:
  - `getDisplayName(): String` - zwraca longName lub shortName
  - `getHardwareModel(): String` - zwraca hwModelString

**Szacowany czas:** 20 minut

## Faza 4: Implementacja warstwy UI - Mapa

### 4.1 Integracja z Google Maps / OpenStreetMap

**Opcja A: Google Maps**
- [ ] Dodać zależność `com.google.maps.android:maps-compose`
- [ ] Skonfigurować klucz API w `AndroidManifest.xml`
- [ ] Utworzyć `MapScreen.kt` z `GoogleMap` composable

**Opcja B: OpenStreetMap (osmdroid)**
- [ ] Dodać zależność `org.osmdroid:osmdroid-android`
- [ ] Utworzyć `MapScreen.kt` z `MapView`

**Szacowany czas:** 1-2 godziny

### 4.2 Implementacja MapScreen

**Plik: `app/src/main/java/com/example/meshtracker_v1/ui/map/MapScreen.kt`**

**Funkcjonalność:**
- [ ] Composable `MapScreen()`
- [ ] Integracja z `MapViewModel`
- [ ] Wyświetlanie mapy
- [ ] Wyświetlanie markerów dla węzłów
- [ ] Obsługa kliknięć na markery
- [ ] Obsługa zoom i pan
- [ ] Wyświetlanie informacji o węźle w info window

**Szacowany czas:** 2-3 godziny

### 4.3 Implementacja MapViewModel

**Plik: `app/src/main/java/com/example/meshtracker_v1/ui/map/MapViewModel.kt`**

**Funkcjonalność:**
- [ ] Klasa dziedzicząca po `ViewModel`
- [ ] StateFlow/State dla:
  - `nodes: StateFlow<List<NodeInfo>>`
  - `selectedNode: StateFlow<NodeInfo?>`
  - `connectionState: StateFlow<ConnectionState>`
- [ ] Metody:
  - `updateNode(nodeInfo: NodeInfo)` - aktualizacja węzła
  - `removeNode(nodeId: String)` - usunięcie węzła
  - `selectNode(nodeId: String?)` - wybór węzła
  - `refreshNodes()` - odświeżenie listy
- [ ] Integracja z `MeshServiceManager`
- [ ] Integracja z `MeshtasticBroadcastReceiver`

**Szacowany czas:** 2-3 godziny

### 4.4 Implementacja NodeToMarkerMapper

**Plik: `app/src/main/java/com/example/meshtracker_v1/mapper/NodeToMarkerMapper.kt`**

**Funkcjonalność:**
- [ ] Funkcja `nodeToMarker(nodeInfo: NodeInfo): MarkerOptions`
- [ ] Konfiguracja:
  - Pozycja (lat/lng)
  - Tytuł (nazwa węzła)
  - Snippet (dodatkowe info: bateria, SNR, etc.)
  - Ikona (opcjonalnie różne ikony dla różnych typów)
- [ ] Aktualizacja istniejącego markera

**Szacowany czas:** 1 godzina

## Faza 5: Implementacja warstwy UI - Lista węzłów

### 5.1 Implementacja NodeListScreen

**Plik: `app/src/main/java/com/example/meshtracker_v1/ui/nodes/NodeListScreen.kt`**

**Funkcjonalność:**
- [ ] Composable `NodeListScreen()`
- [ ] Lista węzłów w `LazyColumn`
- [ ] Integracja z `MapViewModel`
- [ ] Filtrowanie węzłów (tylko z pozycją, tylko online)
- [ ] Sortowanie (nazwa, odległość, ostatnio słyszane)
- [ ] Obsługa kliknięć - nawigacja do mapy z zaznaczonym węzłem

**Szacowany czas:** 2 godziny

### 5.2 Implementacja NodeItem

**Plik: `app/src/main/java/com/example/meshtracker_v1/ui/nodes/NodeItem.kt`**

**Funkcjonalność:**
- [ ] Composable `NodeItem(nodeInfo: NodeInfo, onClick: () -> Unit)`
- [ ] Wyświetlanie:
  - Nazwa węzła
  - Status (online/offline)
  - Ostatnio słyszane
  - Bateria (jeśli dostępna)
  - SNR/RSSI (jeśli dostępne)
- [ ] Styling z Material3

**Szacowany czas:** 1 godzina

## Faza 6: Integracja komponentów

### 6.1 Integracja w MainActivity

**Plik: `app/src/main/java/com/example/meshtracker_v1/MainActivity.kt`**

**Zmiany:**
- [ ] Inicjalizacja `MeshServiceManager` w `onCreate()`
- [ ] Rejestracja `MeshtasticBroadcastReceiver` w `onCreate()`
- [ ] Wyrejestrowanie receiver w `onDestroy()`
- [ ] Rozłączenie `MeshServiceManager` w `onDestroy()`
- [ ] Przekazanie `MeshServiceManager` do ViewModel

**Szacowany czas:** 1 godzina

### 6.2 Integracja ViewModel z serwisem

**Plik: `app/src/main/java/com/example/meshtracker_v1/ui/map/MapViewModel.kt`**

**Zmiany:**
- [ ] Inicjalizacja `MeshServiceManager` w konstruktorze
- [ ] Ustawienie `ConnectionListener`
- [ ] Rejestracja `MeshtasticBroadcastReceiver` z callbackami
- [ ] Obsługa `onNodeChanged()` - aktualizacja state
- [ ] Obsługa `onMeshConnected()` - odświeżenie węzłów
- [ ] Obsługa `onMeshDisconnected()` - aktualizacja stanu połączenia

**Szacowany czas:** 2 godziny

## Faza 7: Obsługa błędów i logowanie

### 7.1 Implementacja logowania

- [ ] Utworzenie utility class `Logger.kt` (opcjonalnie)
- [ ] Dodanie logów w kluczowych miejscach:
  - Połączenie/rozłączenie z serwisem
  - Otrzymanie broadcastów
  - Aktualizacja węzłów
  - Błędy i wyjątki
- [ ] Użycie tagów: `"MeshTracker"`, `"MeshService"`, `"MapView"`

**Szacowany czas:** 30 minut

### 7.2 Obsługa wyjątków

- [ ] Try-catch w `MeshServiceManager` dla `RemoteException`
- [ ] Obsługa null safety w `MeshtasticBroadcastReceiver`
- [ ] Obsługa błędów połączenia w `MapViewModel`
- [ ] Wyświetlanie komunikatów błędów w UI (Snackbar)

**Szacowany czas:** 1 godzina

## Faza 8: Testowanie i optymalizacja

### 8.1 Testy jednostkowe

- [ ] Testy dla `MeshServiceManager` (mock service)
- [ ] Testy dla `NodeToMarkerMapper`
- [ ] Testy dla walidacji pozycji

**Szacowany czas:** 2-3 godziny

### 8.2 Testy integracyjne

- [ ] Test połączenia z aplikacją Meshtastic
- [ ] Test odbierania broadcastów
- [ ] Test wyświetlania węzłów na mapie
- [ ] Test aktualizacji pozycji w czasie rzeczywistym

**Szacowany czas:** 2-3 godziny

### 8.3 Optymalizacja

- [ ] Optymalizacja aktualizacji markerów (unikanie niepotrzebnych aktualizacji)
- [ ] Debouncing dla częstych aktualizacji pozycji
- [ ] Cache dla węzłów
- [ ] Lazy loading dla listy węzłów

**Szacowany czas:** 2 godziny

## Faza 9: Dodatkowe funkcjonalności (opcjonalne)

### 9.1 Filtrowanie i wyszukiwanie

- [ ] Filtrowanie węzłów po nazwie
- [ ] Filtrowanie po statusie (online/offline)
- [ ] Filtrowanie po zasięgu (tylko w zasięgu X km)

**Szacowany czas:** 2 godziny

### 9.2 Szczegóły węzła

- [ ] Ekran szczegółów węzła
- [ ] Historia pozycji
- [ ] Wykres sygnału (SNR/RSSI)
- [ ] Informacje o urządzeniu

**Szacowany czas:** 3-4 godziny

### 9.3 Ustawienia

- [ ] Ekran ustawień
- [ ] Interwał odświeżania
- [ ] Filtry domyślne
- [ ] Preferencje mapy (typ, zoom)

**Szacowany czas:** 2 godziny

## Harmonogram implementacji

### Tydzień 1: Infrastruktura i komunikacja
- **Dzień 1-2:** Faza 1 (Przygotowanie infrastruktury)
- **Dzień 3-4:** Faza 2 (Warstwa komunikacji z Meshtastic)
- **Dzień 5:** Faza 3 (Modele danych)

### Tydzień 2: UI i integracja
- **Dzień 1-3:** Faza 4 (UI - Mapa)
- **Dzień 4-5:** Faza 5 (UI - Lista węzłów)

### Tydzień 3: Integracja i testy
- **Dzień 1-2:** Faza 6 (Integracja komponentów)
- **Dzień 3:** Faza 7 (Obsługa błędów)
- **Dzień 4-5:** Faza 8 (Testowanie)

### Tydzień 4: Dodatkowe funkcjonalności (opcjonalne)
- Faza 9 (dodatkowe funkcjonalności)

## Wymagania techniczne

### Zależności (przykładowe)

```kotlin
// Google Maps
implementation("com.google.maps.android:maps-compose:4.3.0")
implementation("com.google.android.gms:play-services-maps:18.2.0")

// Lub OpenStreetMap
implementation("org.osmdroid:osmdroid-android:6.1.17")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### Uprawnienia

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Wymagania systemowe

- Android 7.0 (API 24) lub wyższy
- Aplikacja Meshtastic zainstalowana i uruchomiona
- Urządzenie Meshtastic połączone z aplikacją

## Uwagi implementacyjne

1. **AIDL Interface:** Jeśli interfejs AIDL nie jest dostępny, może być konieczne użycie reflection lub innego mechanizmu komunikacji z aplikacją Meshtastic.

2. **Parcelable:** Upewnij się, że klasy `NodeInfo`, `Position`, `MeshUser` implementują `Parcelable` lub użyj adapterów.

3. **Threading:** Wszystkie operacje związane z UI powinny być wykonywane na głównym wątku. Operacje sieciowe i serwisowe na wątkach w tle.

4. **Lifecycle:** Pamiętaj o prawidłowym zarządzaniu cyklem życia - rejestracja i wyrejestrowanie receiverów, rozłączanie serwisów.

5. **State Management:** Użyj StateFlow/LiveData dla reaktywnego zarządzania stanem.

6. **Memory Leaks:** Upewnij się, że nie ma wycieków pamięci - użyj weak references dla listenerów, jeśli potrzebne.

## Checklist przed wdrożeniem

- [ ] Wszystkie fazy implementacji zakończone
- [ ] Testy jednostkowe przechodzą
- [ ] Testy integracyjne przechodzą
- [ ] Aplikacja działa z aplikacją Meshtastic
- [ ] Pozycje węzłów wyświetlają się na mapie
- [ ] Aktualizacje pozycji działają w czasie rzeczywistym
- [ ] Obsługa błędów działa poprawnie
- [ ] Brak wycieków pamięci
- [ ] Logi są czytelne i pomocne
- [ ] UI jest responsywne i przyjazne użytkownikowi

## Dokumentacja dla deweloperów

Po zakończeniu implementacji, zaktualizuj:
- [ ] README.md z instrukcjami instalacji i użycia
- [ ] Komentarze w kodzie dla kluczowych funkcji
- [ ] Dokumentacja API (jeśli potrzebna)

---

**Data utworzenia:** 2024
**Wersja:** 1.0
**Status:** Plan implementacji

