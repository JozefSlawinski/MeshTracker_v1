# Dokumentacja Projektu - MeshTracker v1

## Spis treści

1. [Wprowadzenie](#wprowadzenie)
2. [Przegląd technologii i narzędzi](#przegląd-technologii-i-narzędzi)
3. [Architektura systemu](#architektura-systemu)
4. [Szczegółowy opis modułów](#szczegółowy-opis-modułów)
5. [Zastosowane rozwiązania techniczne](#zastosowane-rozwiązania-techniczne)
6. [Struktura projektu](#struktura-projektu)
7. [Przepływ danych](#przepływ-danych)
8. [Konfiguracja i wymagania](#konfiguracja-i-wymagania)
9. [Podsumowanie](#podsumowanie)

---

## Wprowadzenie

### Cel projektu

MeshTracker v1 to aplikacja mobilna dla systemu Android, której głównym celem jest śledzenie i wizualizacja pozycji węzłów w sieci Meshtastic w czasie rzeczywistym. Aplikacja umożliwia użytkownikom monitorowanie lokalizacji wszystkich urządzeń w sieci mesh poprzez wyświetlanie ich pozycji na interaktywnej mapie oraz przeglądanie szczegółowych informacji o każdym węźle.

### Zakres funkcjonalności

Aplikacja oferuje następujące funkcjonalności:

- **Połączenie z siecią Meshtastic**: Automatyczne łączenie z aplikacją Meshtastic poprzez interfejs AIDL (Android Interface Definition Language)
- **Wyświetlanie pozycji węzłów**: Wizualizacja wszystkich węzłów z prawidłową pozycją GPS na mapie Google Maps
- **Lista węzłów**: Przeglądanie wszystkich wykrytych węzłów wraz z ich statusem, parametrami sygnału i informacjami o baterii
- **Aktualizacje w czasie rzeczywistym**: Automatyczne odświeżanie pozycji węzłów poprzez nasłuchiwanie broadcastów systemowych
- **Status połączenia**: Wizualna informacja o stanie połączenia z radiem Meshtastic

### Kontekst techniczny

Projekt wykorzystuje architekturę opartą na wzorcu MVVM (Model-View-ViewModel) oraz nowoczesne technologie Android, w tym Jetpack Compose do budowy interfejsu użytkownika. Komunikacja z aplikacją Meshtastic odbywa się poprzez mechanizm Android Services oraz broadcastów systemowych.

---

## Przegląd technologii i narzędzi

### Platforma i język programowania

- **Platforma**: Android
- **Język programowania**: Kotlin 2.0.21
- **Minimalna wersja SDK**: Android 7.0 (API 24)
- **Docelowa wersja SDK**: Android 14 (API 36)
- **Wersja kompilacji SDK**: Android 14 (API 36)

### Główne biblioteki i frameworki

#### Jetpack Compose
- **androidx.compose.ui**: Biblioteka UI dla Compose
- **androidx.compose.material3**: Material Design 3 dla Compose
- **androidx.compose.material3-adaptive-navigation-suite**: Adaptacyjna nawigacja Material 3
- **Wersja**: 2024.09.00 (BOM)

Jetpack Compose został wybrany jako nowoczesne rozwiązanie do budowy interfejsu użytkownika, oferujące deklaratywny sposób definiowania UI oraz lepszą wydajność w porównaniu do tradycyjnego XML.

#### Google Maps
- **com.google.maps.android:maps-compose**: Integracja Google Maps z Compose (wersja 4.3.0)
- **com.google.android.gms:play-services-maps**: Google Play Services Maps (wersja 18.2.0)

Google Maps zostało wybrane jako platforma do wyświetlania map i markerów ze względu na szerokie wsparcie, wysoką jakość map oraz łatwą integrację z Android.

#### Android Architecture Components
- **androidx.lifecycle:lifecycle-viewmodel-compose**: ViewModel dla Compose (wersja 2.7.0)
- **androidx.lifecycle:lifecycle-runtime-compose**: Runtime lifecycle dla Compose (wersja 2.7.0)

Komponenty architektury Android zapewniają prawidłowe zarządzanie cyklem życia komponentów oraz separację logiki biznesowej od warstwy prezentacji.

#### Kotlin Coroutines
- **org.jetbrains.kotlinx:kotlinx-coroutines-android**: Coroutines dla Android (wersja 1.7.3)

Kotlin Coroutines umożliwiają asynchroniczne operacje bez blokowania głównego wątku, co jest kluczowe dla responsywności aplikacji.

### Narzędzia deweloperskie

- **Android Gradle Plugin**: 8.13.2
- **Gradle**: Wrapper w projekcie
- **Kotlin Compiler**: 2.0.21

### Integracja z Meshtastic

Aplikacja komunikuje się z aplikacją Meshtastic (pakiet: `com.geeksville.mesh`) poprzez:

- **AIDL (Android Interface Definition Language)**: Interfejs do komunikacji między procesami (IPC)
- **Android Services**: Binding do serwisu Meshtastic
- **Broadcast Receivers**: Nasłuchiwanie broadcastów systemowych z aplikacji Meshtastic

---

## Architektura systemu

### Ogólny przegląd architektury

Aplikacja wykorzystuje architekturę opartą na wzorcu **MVVM (Model-View-ViewModel)** z dodatkowymi warstwami odpowiedzialnymi za komunikację zewnętrzną i zarządzanie danymi.

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  MapScreen   │  │ NodeListScreen│ │ MainScreen   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              MapViewModel                            │   │
│  │  - Zarządzanie stanem węzłów                         │   │
│  │  - Obsługa połączenia z Meshtastic                n  │   │
│  │  - Reaktywne aktualizacje (StateFlow)             n  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┴───────────────────┐
        ▼                                       ▼
┌───────────────────────┐            ┌─────────────────────────┐
│  Service Layer        │            │  Receiver Layer         │
│  ┌──────────────────┐ │            │  ┌───────────────────┐  │
│  │MeshServiceManager│ │            │  │MeshtasticBroadcast│  │
│  │                  │ │            │  │   Receiver        │  │
│  │- Service Binding │ │            │  │                   │  │
│  │- AIDL Interface  │ │            │  │- Broadcasts       │  │
│  └──────────────────┘ │            │  └───────────────────┘  │
└───────────────────────┘            └─────────────────────────┘
        │                                       │
        └───────────────────┬───────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Model Layer                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                   │
│  │NodeInfo  │  │ Position │  │MeshUser  │                   │
│  └──────────┘  └──────────┘  └──────────┘                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              External Application (Meshtastic)              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  IMeshService (AIDL)                                 │   │
│  │  - getNodes()                                        │   │
│  │  - getMyId()                                         │   │
│  │  - connectionState()                                 │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Przepływ komunikacji

1. **Inicjalizacja**: `MapViewModel` inicjalizuje `MeshServiceManager` i rejestruje `MeshtasticBroadcastReceiver`
2. **Połączenie**: `MeshServiceManager` łączy się z serwisem Meshtastic poprzez binding
3. **Odbieranie danych**: Dane przychodzą na dwa sposoby:
   - **Synchronicznie**: Wywołanie `getNodes()` przez `MeshServiceManager`
   - **Asynchronicznie**: Broadcasty systemowe odbierane przez `MeshtasticBroadcastReceiver`
4. **Przetwarzanie**: Dane są konwertowane do modeli wewnętrznych (`MeshNodeInfo`, `MeshPosition`, `MeshUserInfo`)
5. **Aktualizacja UI**: `StateFlow` w `MapViewModel` automatycznie aktualizuje UI poprzez Compose

### Wzorce projektowe

#### Singleton Pattern
`MeshServiceManager` wykorzystuje wzorzec Singleton, aby zapewnić jedną instancję zarządzającą połączeniem z serwisem Meshtastic w całej aplikacji.

#### Observer Pattern
- `StateFlow` w ViewModel obserwowany przez UI
- `ConnectionListener` w `MeshServiceManager` do powiadamiania o zmianach stanu połączenia
- `MeshtasticReceiverListener` w `BroadcastReceiver` do przekazywania zdarzeń do ViewModel

#### Repository Pattern (implikowany)
Warstwa modeli (`MeshNodeInfo`, `MeshPosition`, `MeshUserInfo`) pełni rolę repozytorium, abstrahując szczegóły implementacji klas Meshtastic.

---

## Szczegółowy opis modułów

### 1. Warstwa UI (User Interface)

#### 1.1 MainScreen
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/ui/MainScreen.kt`

Główny ekran aplikacji odpowiedzialny za:
- Nawigację między ekranami (Mapa i Lista węzłów)
- Wyświetlanie statusu połączenia w górnym pasku
- Zarządzanie stanem wybranego ekranu

**Kluczowe komponenty**:
- `NavigationBar`: Dolny pasek nawigacji z przełączaniem między ekranami
- `ConnectionStatusBar`: Górny pasek statusu pokazujący stan połączenia i liczbę węzłów
- `Scaffold`: Główny kontener Material Design 3

#### 1.2 MapScreen
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/ui/map/MapScreen.kt`

Ekran mapy wyświetlający węzły Meshtastic na mapie Google Maps.

**Funkcjonalności**:
- Wyświetlanie mapy Google Maps z markerami dla każdego węzła
- Automatyczne centrowanie kamery na wybranym węźle
- Wyświetlanie informacji o węźle w snippet markera (status, bateria, SNR)
- Obsługa kliknięć na markery
- Komunikaty o stanie połączenia (Snackbar)

**Kluczowe elementy**:
- `GoogleMap`: Komponent mapy z biblioteki Maps Compose
- `Marker`: Markery dla każdego węzła z pozycją GPS
- `CameraPositionState`: Stan kamery mapy z możliwością animacji

#### 1.3 NodeListScreen
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/ui/nodes/NodeListScreen.kt`

Ekran listy wszystkich wykrytych węzłów.

**Funkcjonalności**:
- Wyświetlanie listy wszystkich węzłów (nie tylko z pozycją)
- Sortowanie: najpierw węzły online, potem offline, alfabetycznie
- Kliknięcie węzła przełącza na mapę i zaznacza go
- Wyświetlanie stanu połączenia

**Kluczowe elementy**:
- `LazyColumn`: Wydajna lista przewijalna z Compose
- `NodeItem`: Komponent wyświetlający pojedynczy węzeł

#### 1.4 NodeItem
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/ui/nodes/NodeItem.kt`

Komponent wyświetlający informacje o pojedynczym węźle w liście.

**Wyświetlane informacje**:
- Nazwa węzła (longName lub shortName)
- Status (Online/Offline)
- Poziom baterii (jeśli dostępny)
- Parametry sygnału (SNR, RSSI)
- Czas ostatniego kontaktu

#### 1.5 ConnectionStatusBar
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/ui/components/ConnectionStatusBar.kt`

Komponent wyświetlający status połączenia z Meshtastic.

**Wyświetlane informacje**:
- Stan połączenia (Connected/Disconnected/Connecting)
- Liczba wykrytych węzłów
- Kolorowe oznaczenia dla różnych stanów

### 2. Warstwa ViewModel

#### 2.1 MapViewModel
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/ui/map/MapViewModel.kt`

Główny ViewModel aplikacji zarządzający:
- Stanem węzłów (`StateFlow<Map<String, MeshNodeInfo>>`)
- Stanem połączenia (`StateFlow<ConnectionState>`)
- Wybranym węzłem (`StateFlow<String?>`)

**Kluczowe metody**:
- `initialize()`: Inicjalizacja połączenia z Meshtastic
- `refreshNodes()`: Odświeżenie listy węzłów z serwisu
- `onNodeChanged()`: Aktualizacja węzła z broadcastu
- `onMeshConnected()`: Obsługa połączenia radia
- `onMeshDisconnected()`: Obsługa rozłączenia radia
- `selectNode()`: Wybór węzła na mapie

**Zarządzanie cyklem życia**:
- Automatyczne łączenie z serwisem przy inicjalizacji
- Rejestracja BroadcastReceiver
- Okresowe odświeżanie węzłów (co 5 sekund gdy połączone)
- Sprawdzanie stanu połączenia (co 2 sekundy gdy łączy)
- Czyszczenie zasobów w `onCleared()`

### 3. Warstwa serwisowa

#### 3.1 MeshServiceManager
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/service/MeshServiceManager.kt`

Singleton zarządzający połączeniem z serwisem Meshtastic.

**Funkcjonalności**:
- Binding do serwisu Meshtastic (`com.geeksville.mesh.service.MeshService`)
- Użycie reflection do uzyskania dostępu do interfejsu AIDL
- Metody do pobierania danych:
  - `getNodes()`: Lista wszystkich węzłów
  - `getMyNodeID()`: ID własnego węzła
  - `getConnectionState()`: Stan połączenia z radiem
- Callbacki dla zmian stanu połączenia (`ConnectionListener`)

**Implementacja**:
- Wzorzec Singleton z thread-safe inicjalizacją
- `ServiceConnection` do obsługi bindingu
- Obsługa wyjątków `RemoteException`
- Użycie reflection ze względu na brak bezpośredniego dostępu do klas Meshtastic

### 4. Warstwa odbiorców (Receivers)

#### 4.1 MeshtasticBroadcastReceiver
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/receiver/MeshtasticBroadcastReceiver.kt`

BroadcastReceiver odbierający broadcasty z aplikacji Meshtastic.

**Obsługiwane akcje**:
- `ACTION_NODE_CHANGE`: Zmiana węzła (pojawienie się, zniknięcie, aktualizacja)
- `ACTION_MESH_CONNECTED`: Połączenie z radiem Meshtastic
- `ACTION_MESH_DISCONNECTED`: Rozłączenie z radiem

**Kluczowe aspekty implementacji**:
- Użycie ClassLoader z aplikacji Meshtastic do deserializacji `Parcelable` obiektów
- Konwersja obiektów Meshtastic do wewnętrznych modeli
- Obsługa błędów deserializacji
- Interfejs `MeshtasticReceiverListener` do przekazywania zdarzeń

**Wyzwania techniczne**:
- Problem z deserializacją `Parcelable` obiektów z innej aplikacji wymaga użycia odpowiedniego ClassLoader
- Reflection do dostępu do pól i metod klas Meshtastic

### 5. Warstwa modeli

#### 5.1 MeshNodeInfo
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/model/NodeInfo.kt`

Model reprezentujący węzeł w sieci Meshtastic.

**Właściwości**:
- `num`: Numer węzła
- `user`: Informacje o użytkowniku (`MeshUserInfo`)
- `position`: Pozycja GPS (`MeshPosition`)
- `snr`: Signal-to-Noise Ratio
- `rssi`: Received Signal Strength Indicator
- `lastHeard`: Czas ostatniego kontaktu (sekundy od 1970)
- `batteryLevel`: Poziom baterii
- `channel`: Kanał komunikacji
- `hopsAway`: Liczba skoków do węzła

**Metody pomocnicze**:
- `hasValidPosition()`: Sprawdza czy węzeł ma prawidłową pozycję
- `isOnline()`: Sprawdza czy węzeł jest online (ostatnio słyszany w ciągu 5 minut)
- `getDisplayName()`: Zwraca nazwę wyświetlaną
- `getId()`: Zwraca unikalny ID węzła

**Konwersja z Meshtastic**:
- `fromMeshtasticNodeInfo()`: Konwersja z `org.meshtastic.core.model.NodeInfo` używając reflection
- Obsługa różnych wariantów nazw metod i pól
- Szczegółowe logowanie dla debugowania

#### 5.2 MeshPosition
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/model/Position.kt`

Model reprezentujący pozycję GPS.

**Właściwości**:
- `latitude`: Szerokość geograficzna (stopnie)
- `longitude`: Długość geograficzna (stopnie)
- `altitude`: Wysokość (metry)
- `time`: Czas pozycji (sekundy od 1970)
- `satellitesInView`: Liczba widocznych satelitów
- `groundSpeed`: Prędkość (m/s)
- `groundTrack`: Kierunek (stopnie, 0-360)
- `precisionBits`: Precyzja pozycji

**Metody pomocnicze**:
- `isValid()`: Sprawdza czy pozycja nie jest (0,0)
- `isInRange()`: Sprawdza czy pozycja jest w prawidłowym zakresie geograficznym
- `toLatLng()`: Konwersja do `LatLng` dla Google Maps

**Konwersja z Meshtastic**:
- `fromMeshtasticPosition()`: Konwersja z `org.meshtastic.core.model.Position`
- Obsługa różnych formatów danych (Int jako stopnie * 1e7, Double, Float)
- Szczegółowe logowanie dla debugowania

#### 5.3 MeshUserInfo
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/model/MeshUser.kt`

Model reprezentujący użytkownika węzła.

**Właściwości**:
- `id`: Unikalny ID węzła (np. "!12345abc")
- `longName`: Długa nazwa użytkownika
- `shortName`: Krótka nazwa
- `hwModelString`: Model sprzętu jako String
- `isLicensed`: Czy użytkownik ma licencję
- `role`: Rola użytkownika

**Metody pomocnicze**:
- `getDisplayName()`: Zwraca longName lub shortName
- `getHardwareModel()`: Zwraca model sprzętu

**Konwersja z Meshtastic**:
- `fromMeshtasticUser()`: Konwersja z `org.meshtastic.core.model.MeshUser` używając reflection

### 6. Warstwa narzędziowa

#### 6.1 Constants
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/util/Constants.kt`

Obiekt zawierający wszystkie stałe używane do komunikacji z Meshtastic.

**Stałe**:
- Akcje broadcastów: `ACTION_NODE_CHANGE`, `ACTION_MESH_CONNECTED`, `ACTION_MESH_DISCONNECTED`
- Extras dla intentów: `EXTRA_NODE_INFO`, `EXTRA_CONNECTED`
- Stany połączenia: `STATE_CONNECTED`, `STATE_DISCONNECTED`
- Pakiet aplikacji: `MESHTASTIC_PACKAGE`
- Akcja serwisu: `MESH_SERVICE_ACTION`

#### 6.2 NodeToMarkerMapper
**Lokalizacja**: `app/src/main/java/com/example/meshtracker_v1/mapper/NodeToMarkerMapper.kt`

Mapper konwertujący `MeshNodeInfo` na `MarkerOptions` dla Google Maps.

**Funkcjonalności**:
- Konwersja pozycji do `LatLng`
- Ustawienie tytułu markera (nazwa węzła)
- Budowa snippet z informacjami o węźle
- Opcjonalne różne ikony dla różnych typów węzłów

### 7. Interfejs AIDL

#### 7.1 IMeshService.aidl
**Lokalizacja**: `app/src/main/aidl/org/meshtastic/core/service/IMeshService.aidl`

Definicja interfejsu AIDL do komunikacji z serwisem Meshtastic.

**Metody**:
- `List<NodeInfo> getNodes()`: Pobiera listę wszystkich węzłów
- `MyNodeInfo getMyNodeInfo()`: Pobiera informacje o własnym węźle
- `String getMyId()`: Pobiera ID własnego węzła
- `String connectionState()`: Sprawdza stan połączenia z radiem

**Uwagi**:
- Interfejs musi być zgodny z interfejsem w aplikacji Meshtastic
- Klasy `NodeInfo` i `MyNodeInfo` muszą być dostępne w czasie kompilacji lub używać reflection

---

## Zastosowane rozwiązania techniczne

### 1. Reflection dla klas Meshtastic

**Problem**: Aplikacja nie ma bezpośredniego dostępu do klas z pakietu `org.meshtastic.core.model` w czasie kompilacji, ponieważ są one częścią aplikacji Meshtastic.

**Rozwiązanie**: Użycie Java Reflection do dynamicznego dostępu do metod i pól klas Meshtastic w czasie wykonania.

**Implementacja**:
- W `MeshServiceManager`: Reflection do wywołania metod interfejsu AIDL
- W `MeshNodeInfo.fromMeshtasticNodeInfo()`: Reflection do odczytu właściwości `NodeInfo`
- W `MeshPosition.fromMeshtasticPosition()`: Reflection do odczytu właściwości `Position`
- W `MeshUserInfo.fromMeshtasticUser()`: Reflection do odczytu właściwości `MeshUser`

**Zalety**:
- Nie wymaga zależności od bibliotek Meshtastic
- Elastyczność w obsłudze różnych wersji aplikacji Meshtastic
- Możliwość obsługi alternatywnych nazw metod/pól

**Wady**:
- Wolniejsze niż bezpośredni dostęp
- Brak sprawdzania typów w czasie kompilacji
- Większa podatność na błędy w czasie wykonania

### 2. ClassLoader dla Parcelable

**Problem**: Przy deserializacji obiektów `Parcelable` z broadcastów, system Android wymaga odpowiedniego ClassLoader, aby móc poprawnie zdeserializować obiekty z innej aplikacji.

**Rozwiązanie**: Użycie ClassLoader z aplikacji Meshtastic do deserializacji obiektów.

**Implementacja w `MeshtasticBroadcastReceiver`**:
```kotlin
val meshtasticContext = context.createPackageContext(
    Constants.MESHTASTIC_PACKAGE,
    Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
)
val meshtasticClassLoader = meshtasticContext.classLoader
Thread.currentThread().contextClassLoader = meshtasticClassLoader
bundle.classLoader = meshtasticClassLoader
```

**Kluczowe aspekty**:
- Ustawienie Thread context ClassLoader przed dostępem do Bundle
- Ustawienie ClassLoader dla Bundle
- Przywrócenie oryginalnego ClassLoader po zakończeniu

### 3. StateFlow dla reaktywności

**Problem**: Potrzeba reaktywnego aktualizowania UI w odpowiedzi na zmiany danych z Meshtastic.

**Rozwiązanie**: Użycie `StateFlow` w ViewModel do przechowywania stanu, który jest automatycznie obserwowany przez Compose.

**Implementacja**:
- `StateFlow<Map<String, MeshNodeInfo>>` dla węzłów
- `StateFlow<ConnectionState>` dla stanu połączenia
- `StateFlow<String?>` dla wybranego węzła

**Zalety**:
- Automatyczne aktualizacje UI przy zmianie stanu
- Thread-safe operacje
- Integracja z Compose przez `collectAsState()`

### 4. Okresowe odświeżanie

**Problem**: Potrzeba okresowego sprawdzania stanu połączenia i odświeżania listy węzłów.

**Rozwiązanie**: Użycie Kotlin Coroutines z `delay()` do okresowego wykonywania operacji.

**Implementacja w `MapViewModel`**:
- `startPeriodicRefresh()`: Odświeżanie węzłów co 5 sekund gdy połączone
- `startConnectionCheck()`: Sprawdzanie stanu połączenia co 2 sekundy gdy łączy

**Zarządzanie**:
- Anulowanie jobów przy zmianie stanu
- Anulowanie wszystkich jobów w `onCleared()`

### 5. Singleton dla MeshServiceManager

**Problem**: Potrzeba jednej instancji zarządzającej połączeniem z serwisem w całej aplikacji.

**Rozwiązanie**: Wzorzec Singleton z thread-safe inicjalizacją.

**Implementacja**:
```kotlin
companion object {
    @Volatile
    private var INSTANCE: MeshServiceManager? = null
    
    fun getInstance(context: Context): MeshServiceManager {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: MeshServiceManager(context.applicationContext).also { INSTANCE = it }
        }
    }
}
```

**Zalety**:
- Jedna instancja w całej aplikacji
- Thread-safe inicjalizacja
- Użycie `applicationContext` zapobiega wyciekom pamięci

### 6. Obsługa cyklu życia

**Problem**: Prawidłowe zarządzanie zasobami (receivery, serwisy, joby) w odpowiedzi na zmiany cyklu życia.

**Rozwiązanie**: Implementacja `onCleared()` w ViewModel oraz prawidłowa rejestracja/wyrejestrowanie receiverów.

**Implementacja**:
- Rejestracja receivera w `initialize()`
- Wyrejestrowanie receivera w `onCleared()`
- Rozłączenie serwisu w `onCleared()`
- Anulowanie wszystkich jobów w `onCleared()`

### 7. Walidacja pozycji

**Problem**: Nie wszystkie węzły mają prawidłową pozycję GPS (mogą mieć 0,0 lub być poza zakresem).

**Rozwiązanie**: Metody walidacji w `MeshPosition`:
- `isValid()`: Sprawdza czy pozycja nie jest (0,0)
- `isInRange()`: Sprawdza czy pozycja jest w prawidłowym zakresie geograficznym

**Użycie**:
- Filtrowanie węzłów przed wyświetleniem na mapie
- Sprawdzanie w `MeshNodeInfo.hasValidPosition()`

---

## Struktura projektu

```
MeshTracker_v1/
├── app/
│   ├── build.gradle.kts              # Konfiguracja modułu aplikacji
│   ├── proguard-rules.pro            # Reguły ProGuard
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Manifest aplikacji
│       │   ├── aidl/
│       │   │   └── org/meshtastic/core/service/
│       │   │       └── IMeshService.aidl  # Interfejs AIDL
│       │   ├── java/com/example/meshtracker_v1/
│       │   │   ├── MainActivity.kt    # Główna aktywność
│       │   │ ├── mapper/
│       │   │   │   └── NodeToMarkerMapper.kt  # Mapper węzłów na markery
│       │   │ ├── model/
│       │   │   │   ├── NodeInfo.kt   # Model węzła
│       │   │   │   ├── Position.kt   # Model pozycji
│       │   │   │   └── MeshUser.kt   # Model użytkownika
│       │   │ ├── receiver/
│       │   │   │   └── MeshtasticBroadcastReceiver.kt  # Odbiorca broadcastów
│       │   │ ├── service/
│       │   │   │   └── MeshServiceManager.kt  # Manager serwisu
│       │   │ ├── ui/
│       │   │   │   ├── MainScreen.kt  # Główny ekran
│       │   │   │   ├── components/
│       │   │   │   │   └── ConnectionStatusBar.kt  # Pasek statusu
│       │   │   │   ├── map/
│       │   │   │   │   ├── MapScreen.kt  # Ekran mapy
│       │   │   │   │   └── MapViewModel.kt  # ViewModel mapy
│       │   │   │   ├── nodes/
│       │   │   │   │   ├── NodeListScreen.kt  # Ekran listy
│       │   │   │   │   └── NodeItem.kt  # Element listy
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt  # Kolory
│       │   │   │       ├── Theme.kt  # Motyw
│       │   │   │       └── Type.kt   # Typografia
│       │   │   └── util/
│       │   │       └── Constants.kt  # Stałe
│       │   └── res/  # Zasoby (ikony, stringi, itp.)
│       ├── androidTest/  # Testy UI
│       └── test/  # Testy jednostkowe
├── build.gradle.kts  # Konfiguracja projektu głównego
├── gradle/
│   ├── libs.versions.toml  # Wersje zależności
│   └── wrapper/  # Gradle wrapper
├── docs/  # Dokumentacja
│   ├── DOKUMENTACJA_MESHTASTIC_POZYCJA.md
│   ├── PLAN_IMPLEMENTACJI.md
│   ├── STATUS_IMPLEMENTACJI.md
│   └── DOKUMENTACJA_PROJEKTU.md  # Ten plik
└── settings.gradle.kts  # Ustawienia Gradle
```

### Opis głównych katalogów

- **app/**: Główny moduł aplikacji
- **app/src/main/java/**: Kod źródłowy Kotlin
- **app/src/main/aidl/**: Definicje interfejsów AIDL
- **app/src/main/res/**: Zasoby aplikacji (ikony, stringi, layouty)
- **docs/**: Dokumentacja projektu
- **gradle/**: Konfiguracja Gradle i wrapper

---

## Przepływ danych

### 1. Inicjalizacja aplikacji

```
MainActivity.onCreate()
    ↓
MainScreen (Composable)
    ↓
MapViewModel (inicjalizacja)
    ↓
MeshServiceManager.getInstance()
    ↓
MeshServiceManager.connect()
    ↓
Context.bindService() → Meshtastic Service
    ↓
ServiceConnection.onServiceConnected()
    ↓
MeshServiceManager.setConnectionListener()
    ↓
MapViewModel.initialize()
    ↓
Context.registerReceiver() → MeshtasticBroadcastReceiver
```

### 2. Odbieranie danych o węzłach

#### 2.1 Synchroniczne pobieranie

```
MapViewModel.refreshNodes()
    ↓
MeshServiceManager.getNodes()
    ↓
IMeshService.getNodes() (przez reflection)
    ↓
List<NodeInfo> (z Meshtastic)
    ↓
MeshNodeInfo.fromMeshtasticNodeInfo() (dla każdego węzła)
    ↓
MapViewModel._nodes.value = nodesMap
    ↓
UI automatycznie aktualizuje się (StateFlow)
```

#### 2.2 Asynchroniczne aktualizacje (Broadcasty)

```
Meshtastic App → Broadcast: ACTION_NODE_CHANGE
    ↓
MeshtasticBroadcastReceiver.onReceive()
    ↓
handleNodeChange()
    ↓
Deserializacja NodeInfo (z ClassLoader Meshtastic)
    ↓
MeshNodeInfo.fromMeshtasticNodeInfo()
    ↓
MeshtasticReceiverListener.onNodeChanged()
    ↓
MapViewModel.onNodeChanged()
    ↓
MapViewModel._nodes.value = updatedMap
    ↓
UI automatycznie aktualizuje się
```

### 3. Wyświetlanie na mapie

```
MapScreen (Composable)
    ↓
viewModel.nodes.collectAsState()
    ↓
Filtrowanie: nodes.values.filter { it.hasValidPosition() }
    ↓
Dla każdego węzła:
    ↓
Marker(
        position = node.position.toLatLng(),
        title = node.getDisplayName(),
        snippet = buildMarkerSnippet(node)
    )
    ↓
GoogleMap wyświetla markery
```

### 4. Obsługa kliknięć

```
Użytkownik klika marker
    ↓
Marker.onClick { viewModel.selectNode(nodeId) }
    ↓
MapViewModel._selectedNodeId.value = nodeId
    ↓
LaunchedEffect(selectedNodeId) w MapScreen
    ↓
cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom())
    ↓
Kamera animuje się do węzła
```

### 5. Zmiana stanu połączenia

```
Meshtastic App → Broadcast: ACTION_MESH_CONNECTED
    ↓
MeshtasticBroadcastReceiver.handleMeshConnected()
    ↓
MeshtasticReceiverListener.onMeshConnected()
    ↓
MapViewModel.onMeshConnected()
    ↓
_connectionState.value = CONNECTED
    ↓
startPeriodicRefresh() (odświeżanie co 5 sekund)
    ↓
UI aktualizuje ConnectionStatusBar
```

---

## Konfiguracja i wymagania

### Wymagania systemowe

- **Minimalna wersja Android**: 7.0 (API 24)
- **Docelowa wersja Android**: 14 (API 36)
- **Aplikacja Meshtastic**: Musi być zainstalowana i uruchomiona
- **Urządzenie radio Meshtastic**: Musi być połączone z aplikacją Meshtastic

### Konfiguracja Google Maps API

1. Utwórz projekt w [Google Cloud Console](https://console.cloud.google.com/)
2. Włącz API: **Maps SDK for Android**
3. Utwórz klucz API
4. Dodaj klucz do `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE" />
```

**Uwaga**: W projekcie znajduje się już klucz API, ale należy go zastąpić własnym dla produkcji.

### Uprawnienia

Aplikacja wymaga następujących uprawnień (zdefiniowane w `AndroidManifest.xml`):

- `INTERNET`: Do pobierania map Google Maps
- `ACCESS_FINE_LOCATION`: Do wyświetlania lokalizacji użytkownika (opcjonalnie)
- `ACCESS_COARSE_LOCATION`: Do wyświetlania lokalizacji użytkownika (opcjonalnie)

**Uwaga**: Aplikacja obecnie nie obsługuje runtime permission requests. Uprawnienia powinny być przyznane w ustawieniach systemu.

### Konfiguracja Meshtastic

1. Zainstaluj aplikację Meshtastic z Google Play Store
2. Uruchom aplikację Meshtastic
3. Połącz urządzenie radio Meshtastic z aplikacją
4. Upewnij się, że aplikacja Meshtastic działa w tle

### Query dla aplikacji Meshtastic

W `AndroidManifest.xml` znajduje się konfiguracja query wymagana w Android 11+:

```xml
<queries>
    <package android:name="com.geeksville.mesh" />
</queries>
```

Pozwala to aplikacji na:
- Binding do serwisu Meshtastic
- Odbieranie broadcastów z aplikacji Meshtastic

### Budowanie projektu

1. Sklonuj repozytorium
2. Otwórz projekt w Android Studio
3. Zsynchronizuj zależności Gradle
4. Skonfiguruj Google Maps API key (jeśli potrzebne)
5. Zbuduj projekt: `Build > Make Project`
6. Uruchom na urządzeniu lub emulatorze

### Debugowanie

Aplikacja zawiera szczegółowe logi w kluczowych miejscach:

- `MeshServiceManager`: Logi połączenia z serwisem
- `MeshtasticBroadcastReceiver`: Logi odbieranych broadcastów
- `MapViewModel`: Logi aktualizacji węzłów
- `MeshNodeInfo`, `MeshPosition`, `MeshUserInfo`: Logi konwersji danych

Filtrowanie logów w Logcat:
- Tag: `MeshServiceManager`, `MeshtasticReceiver`, `MapViewModel`, `MeshNodeInfo`, `MeshPosition`

---

## Podsumowanie

### Osiągnięcia projektu

Projekt MeshTracker v1 został pomyślnie zaimplementowany jako funkcjonalna aplikacja Android do śledzenia węzłów sieci Meshtastic. Aplikacja oferuje:

1. **Pełną integrację z Meshtastic**: Komunikacja poprzez AIDL i broadcasty systemowe
2. **Wizualizację na mapie**: Wyświetlanie pozycji węzłów na Google Maps
3. **Listę węzłów**: Przeglądanie wszystkich wykrytych węzłów z szczegółowymi informacjami
4. **Aktualizacje w czasie rzeczywistym**: Automatyczne odświeżanie pozycji
5. **Nowoczesny interfejs**: UI zbudowany w Jetpack Compose z Material Design 3

### Kluczowe rozwiązania techniczne

1. **Reflection**: Umożliwiło integrację z aplikacją Meshtastic bez bezpośrednich zależności
2. **StateFlow**: Zapewniło reaktywność i automatyczne aktualizacje UI
3. **Architektura MVVM**: Separacja logiki biznesowej od warstwy prezentacji
4. **Kotlin Coroutines**: Asynchroniczne operacje bez blokowania głównego wątku
5. **ClassLoader management**: Prawidłowa deserializacja obiektów Parcelable z innej aplikacji

### Możliwe rozszerzenia

Projekt może być rozszerzony o następujące funkcjonalności:

1. **Historia pozycji**: Przechowywanie i wyświetlanie historii pozycji węzłów
2. **Filtrowanie i wyszukiwanie**: Zaawansowane filtry dla węzłów
3. **Powiadomienia**: Powiadomienia o pojawieniu się nowych węzłów
4. **Eksport danych**: Eksport pozycji do plików (KML, GPX)
5. **Statystyki**: Wykresy sygnału, statystyki połączeń
6. **Ustawienia**: Konfiguracja interwałów odświeżania, filtrów domyślnych
7. **Obsługa wielu sieci**: Obsługa wielu sieci Meshtastic jednocześnie

### Wnioski

Projekt demonstruje skuteczną integrację aplikacji Android z zewnętrzną aplikacją poprzez mechanizmy systemowe Android (Services, Broadcasts, AIDL). Zastosowanie nowoczesnych technologii (Jetpack Compose, Kotlin Coroutines, StateFlow) zapewnia wydajną i responsywną aplikację, gotową do dalszego rozwoju.

---

**Data utworzenia dokumentacji**: 2024  
**Wersja projektu**: 1.0  
**Status**: Zakończony i gotowy do użycia

