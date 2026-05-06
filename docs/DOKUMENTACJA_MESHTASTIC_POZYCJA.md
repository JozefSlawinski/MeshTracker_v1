# Dokumentacja: Integracja z Meshtastic - Wyświetlanie Pozycji Węzłów

## Spis treści
1. [Przegląd architektury](#przegląd-architektury)
2. [IMeshService - Interfejs komunikacji](#imeshservice---interfejs-komunikacji)
3. [Struktura danych](#struktura-danych)
4. [Implementacja - Krok po kroku](#implementacja---krok-po-kroku)
5. [Gotowe fragmenty kodu](#gotowe-fragmenty-kodu)
6. [Gotowy prompt do implementacji](#gotowy-prompt-do-implementacji)

---

## Przegląd architektury

### Komponenty systemu

Aplikacja MeshTracker składa się z następujących kluczowych komponentów:

1. **MeshServiceManager** - Zarządza połączeniem z serwisem IMeshService aplikacji Meshtastic
2. **MeshtasticReceiver** - Odbiera broadcasty z Meshtastic (pozycje, węzły, wiadomości)
3. **MapViewModel** - Przetwarza dane węzłów i aktualizuje warstwę mapy

### Przepływ danych

```
Meshtastic Device (Radio)
    ↓
Meshtastic Android App
    ↓
IMeshService (AIDL)
    ↓
MeshServiceManager (bindService)
    ↓
BroadcastReceiver (ACTION_NODE_CHANGE)
    ↓
MeshtasticReceiver.onReceive()
    ↓
NodeInfo → Position
    ↓
MapViewModel (LiveData)
    ↓
MapFragment (wyświetlenie markera)
```

---

## IMeshService - Interfejs komunikacji

### Lokalizacja pliku AIDL

### Kluczowe metody dla pozycji węzłów

```java
// Pobierz listę wszystkich węzłów w sieci
List<NodeInfo> getNodes();

// Pobierz informacje o własnym węźle
MyNodeInfo getMyNodeInfo();

// Pobierz unikalny ID własnego węzła
String getMyId();

// Sprawdź stan połączenia z radiem
String connectionState();
```

### Broadcasty do nasłuchiwania

Aplikacja Meshtastic wysyła następujące broadcasty:

```java
// Zmiana węzła (pojawienie się, zniknięcie, aktualizacja pozycji)
"com.geeksville.mesh.NODE_CHANGE"
    Extra: "com.geeksville.mesh.NodeInfo" (Parcelable NodeInfo)

// Zmiana stanu połączenia z radiem
"com.geeksville.mesh.MESH_CONNECTED"
    Extra: "com.geeksville.mesh.Connected" (String: "CONNECTED"/"DISCONNECTED")

"com.geeksville.mesh.MESH_DISCONNECTED"
    Extra: "com.geeksville.mesh.disconnected" (String)
```

---

## Struktura danych

### NodeInfo

Reprezentuje węzeł w sieci Meshtastic:

```kotlin
data class NodeInfo(
    val num: Int,                    // Unikalny numer węzła (używany jako klucz)
    var user: MeshUser? = null,       // Informacje o użytkowniku
    var position: Position? = null,  // Pozycja GPS
    var snr: Float = Float.MAX_VALUE, // Signal-to-Noise Ratio
    var rssi: Int = Int.MAX_VALUE,    // Received Signal Strength Indicator
    var lastHeard: Int = 0,          // Ostatni czas słyszalności (sekundy od 1970)
    var deviceMetrics: DeviceMetrics? = null, // Metryki urządzenia (bateria, etc.)
    var channel: Int = 0,             // Kanał komunikacji
    var environmentMetrics: EnvironmentMetrics? = null,
    var hopsAway: Int = 0             // Liczba skoków do węzła
)
```

### Position

Reprezentuje pozycję GPS:

```kotlin
data class Position(
    val latitude: Double,      // Szerokość geograficzna (stopnie)
    val longitude: Double,     // Długość geograficzna (stopnie)
    val altitude: Int,        // Wysokość (metry)
    val time: Int = currentTime(), // Czas w sekundach (NIE milisekundach!)
    val satellitesInView: Int = 0,
    val groundSpeed: Int = 0,  // Prędkość (m/s)
    val groundTrack: Int = 0,  // Kierunek (stopnie, 0-360)
    val precisionBits: Int = 0
)
```

### MeshUser

Informacje o użytkowniku węzła:

```kotlin
data class MeshUser(
    val id: String,                    // Unikalny ID węzła (np. "!12345abc")
    val longName: String,              // Długa nazwa użytkownika
    val shortName: String,             // Krótka nazwa
    val hwModel: HardwareModel,        // Model sprzętu
    val isLicensed: Boolean = false,
    val role: Int = 0
)
```

---

## Implementacja - Krok po kroku

### Krok 1: Konfiguracja AndroidManifest.xml

Dodaj query dla aplikacji Meshtastic (wymagane w Android 11+):

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <queries>
        <package android:name="com.geeksville.mesh" />
    </queries>
    
    <application>
        <!-- Twoja aplikacja -->
    </application>
</manifest>
```

### Krok 2: Inicjalizacja MeshServiceManager

```java
import org.meshtastic.core.model.NodeInfo;
import org.meshtastic.core.model.Position;

public class YourMapComponent {
    private MeshServiceManager meshServiceManager;
    private Context context;
    
    public void onCreate(Context context) {
        this.context = context;
        
        // Pobierz singleton MeshServiceManager
        meshServiceManager = MeshServiceManager.getInstance(context);
        
        // Ustaw listener dla zmian połączenia
        meshServiceManager.setConnectionListener(new MeshServiceManager.ConnectionListener() {
            @Override
            public void onServiceConnected() {
                Log.d(TAG, "Połączono z Meshtastic service");
                // Możesz teraz pobrać listę węzłów
                loadNodes();
            }
            
            @Override
            public void onServiceDisconnected() {
                Log.d(TAG, "Rozłączono z Meshtastic service");
            }
        });
        
        // Połącz z serwisem
        meshServiceManager.connect();
    }
    
    private void loadNodes() {
        if (!meshServiceManager.isConnected()) {
            Log.w(TAG, "Serwis nie jest połączony");
            return;
        }
        
        List<NodeInfo> nodes = meshServiceManager.getNodes();
        if (nodes != null) {
            for (NodeInfo node : nodes) {
                displayNodePosition(node);
            }
        }
    }
}
```

### Krok 3: Rejestracja BroadcastReceiver

```java
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class YourMapComponent {
    private BroadcastReceiver meshtasticReceiver;
    
    public void onCreate(Context context) {
        // ... inicjalizacja ...
        
        // Utwórz receiver
        meshtasticReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (Constants.ACTION_NODE_CHANGE.equals(action)) {
                    handleNodeChange(intent);
                } else if (Constants.ACTION_MESH_CONNECTED.equals(action)) {
                    handleConnectionChange(intent);
                }
            }
        };
        
        // Zarejestruj receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NODE_CHANGE);
        filter.addAction(Constants.ACTION_MESH_CONNECTED);
        filter.addAction(Constants.ACTION_MESH_DISCONNECTED);
        
        context.registerReceiver(meshtasticReceiver, filter, Context.RECEIVER_EXPORTED);
    }
    
    private void handleNodeChange(Intent intent) {
        NodeInfo nodeInfo = intent.getParcelableExtra("com.geeksville.mesh.NodeInfo");
        if (nodeInfo != null && nodeInfo.getUser() != null) {
            displayNodePosition(nodeInfo);
        }
    }
    
    private void handleConnectionChange(Intent intent) {
        String state = intent.getStringExtra(Constants.EXTRA_CONNECTED);
        if (Constants.STATE_CONNECTED.equalsIgnoreCase(state)) {
            Log.d(TAG, "Radio Meshtastic połączone");
            // Odśwież listę węzłów
            loadNodes();
        }
    }
}
```

### Krok 4: Wyświetlanie pozycji na mapie

Pobrane dane węzła przekazujemy do ViewModelu, który aktualizuje warstwę mapy:

```java
private void displayNodePosition(NodeInfo nodeInfo) {
    if (nodeInfo == null || nodeInfo.getUser() == null) {
        return;
    }

    Position position = nodeInfo.getPosition();
    if (position == null) {
        Log.d(TAG, "Węzeł " + nodeInfo.getUser().getId() + " nie ma pozycji");
        return;
    }

    if (position.getLatitude() == 0.0 && position.getLongitude() == 0.0) {
        Log.d(TAG, "Węzeł " + nodeInfo.getUser().getId() + " ma nieprawidłową pozycję (0,0)");
        return;
    }

    String nodeId   = nodeInfo.getUser().getId();
    String nodeName = nodeInfo.getUser().getLongName();
    double lat      = position.getLatitude();
    double lon      = position.getLongitude();
    int    alt      = position.getAltitude();

    Log.d(TAG, "Pozycja węzła: " + nodeName + " (" + lat + ", " + lon + ")");

    // Przekaż do warstwy UI (np. przez ViewModel lub callback)
    if (listener != null) {
        listener.onNodePositionUpdated(nodeId, nodeName, lat, lon, alt);
    }
}
```

### Krok 5: Czyszczenie zasobów

```java
public void onDestroy(Context context) {
    // Odłącz od serwisu
    if (meshServiceManager != null) {
        meshServiceManager.disconnect();
    }
    
    // Wyrejestruj receiver
    if (meshtasticReceiver != null) {
        context.unregisterReceiver(meshtasticReceiver);
    }
}
```

---

## Gotowe fragmenty kodu

### Pełna klasa do wyświetlania pozycji węzłów

```java
package com.example.meshtastic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.meshtastic.core.model.NodeInfo;
import org.meshtastic.core.model.Position;

import java.util.List;

public class MeshtasticNodePositionDisplay {
    private static final String TAG = "MeshtasticNodePos";

    public interface NodePositionListener {
        void onNodePositionUpdated(String nodeId, String nodeName,
                                   double lat, double lon, int altitudeMeters);
    }

    private Context context;
    private MeshServiceManager meshServiceManager;
    private BroadcastReceiver receiver;
    private final NodePositionListener listener;

    public MeshtasticNodePositionDisplay(Context context, NodePositionListener listener) {
        this.context  = context;
        this.listener = listener;
        initialize();
    }

    private void initialize() {
        meshServiceManager = MeshServiceManager.getInstance(context);

        meshServiceManager.setConnectionListener(new MeshServiceManager.ConnectionListener() {
            @Override
            public void onServiceConnected() {
                Log.d(TAG, "Połączono z Meshtastic");
                refreshNodes();
            }

            @Override
            public void onServiceDisconnected() {
                Log.d(TAG, "Rozłączono z Meshtastic");
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleBroadcast(intent);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_NODE_CHANGE);
        filter.addAction(Constants.ACTION_MESH_CONNECTED);
        filter.addAction(Constants.ACTION_MESH_DISCONNECTED);

        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        meshServiceManager.connect();
    }

    private void handleBroadcast(Intent intent) {
        String action = intent.getAction();

        if (Constants.ACTION_NODE_CHANGE.equals(action)) {
            NodeInfo nodeInfo = intent.getParcelableExtra("com.geeksville.mesh.NodeInfo");
            if (nodeInfo != null) {
                displayNode(nodeInfo);
            }
        } else if (Constants.ACTION_MESH_CONNECTED.equals(action)) {
            String state = intent.getStringExtra(Constants.EXTRA_CONNECTED);
            if (Constants.STATE_CONNECTED.equalsIgnoreCase(state)) {
                refreshNodes();
            }
        }
    }

    private void refreshNodes() {
        if (!meshServiceManager.isConnected()) return;

        List<NodeInfo> nodes = meshServiceManager.getNodes();
        if (nodes != null) {
            for (NodeInfo node : nodes) {
                displayNode(node);
            }
        }
    }

    private void displayNode(NodeInfo nodeInfo) {
        if (nodeInfo == null || nodeInfo.getUser() == null) return;

        Position position = nodeInfo.getPosition();
        if (position == null ||
            (position.getLatitude() == 0.0 && position.getLongitude() == 0.0)) {
            return;
        }

        Log.d(TAG, "Wyświetlono węzeł: " + nodeInfo.getUser().getLongName());

        if (listener != null) {
            listener.onNodePositionUpdated(
                nodeInfo.getUser().getId(),
                nodeInfo.getUser().getLongName(),
                position.getLatitude(),
                position.getLongitude(),
                position.getAltitude()
            );
        }
    }

    public void cleanup() {
        if (meshServiceManager != null) {
            meshServiceManager.disconnect();
        }
        if (receiver != null && context != null) {
            context.unregisterReceiver(receiver);
        }
    }
}
```

### Użycie w komponencie mapy

```java
public class YourMapComponent {
    private MeshtasticNodePositionDisplay positionDisplay;
    
    public void onCreate(Context context) {
        positionDisplay = new MeshtasticNodePositionDisplay(context);
    }
    
    public void onDestroy(Context context) {
        if (positionDisplay != null) {
            positionDisplay.cleanup();
        }
    }
}
```

---

## Gotowy prompt do implementacji

Poniższy prompt możesz użyć bezpośrednio z AI do stworzenia funkcjonalności:

---

**PROMPT:**

Stwórz aplikację Android, która łączy się z aplikacją Meshtastic przez interfejs IMeshService i wyświetla pozycje węzłów na mapie.

**Wymagania:**

1. **Połączenie z Meshtastic:**
   - Użyj `MeshServiceManager` do połączenia z serwisem `com.geeksville.mesh.service.MeshService`
   - Pakiet aplikacji Meshtastic: `com.geeksville.mesh`
   - Zarejestruj `BroadcastReceiver` dla akcji:
     - `com.geeksville.mesh.NODE_CHANGE` - zmiany węzłów
     - `com.geeksville.mesh.MESH_CONNECTED` - połączenie z radiem
     - `com.geeksville.mesh.MESH_DISCONNECTED` - rozłączenie z radiem

2. **Odbieranie pozycji:**
   - W `BroadcastReceiver.onReceive()` dla `ACTION_NODE_CHANGE`:
     - Pobierz `NodeInfo` z extra: `"com.geeksville.mesh.NodeInfo"`
     - Sprawdź czy `NodeInfo.getUser()` i `NodeInfo.getPosition()` nie są null
     - Sprawdź czy pozycja jest prawidłowa (latitude != 0.0 || longitude != 0.0)

3. **Wyświetlanie na mapie:**
   - Dla każdego węzła z prawidłową pozycją wyodrębnij dane:
     - `nodeInfo.getUser().getId()` — unikalny ID
     - `nodeInfo.getUser().getLongName()` — nazwa wyświetlana
     - `position.getLatitude()`, `position.getLongitude()`, `position.getAltitude()`
   - Przekaż dane do ViewModelu lub callbacku warstwy mapy (np. Google Maps `MarkerOptions`)

4. **Struktury danych:**
   - `NodeInfo` zawiera: `user` (MeshUser), `position` (Position), `deviceMetrics`
   - `Position` zawiera: `latitude` (double), `longitude` (double), `altitude` (int)
   - `MeshUser` zawiera: `id` (String), `longName` (String), `hwModelString` (String)

5. **Zarządzanie cyklem życia:**
   - W `onCreate()`: inicjalizuj `MeshServiceManager`, zarejestruj receiver, wywołaj `connect()`
   - W `onDestroy()`: wywołaj `disconnect()` i `unregisterReceiver()`
   - Ustaw `ConnectionListener` aby odświeżać listę węzłów przy połączeniu

6. **AndroidManifest.xml:**
   - Dodaj `<queries><package android:name="com.geeksville.mesh" /></queries>`
   - Dodaj uprawnienia jeśli potrzebne

**Gotowe fragmenty kodu do użycia:**

```java
// Inicjalizacja
MeshServiceManager manager = MeshServiceManager.getInstance(context);
manager.setConnectionListener(new MeshServiceManager.ConnectionListener() {
    @Override
    public void onServiceConnected() {
        List<NodeInfo> nodes = manager.getNodes();
        // Przetwórz węzły
    }
    @Override
    public void onServiceDisconnected() {}
});
manager.connect();

// BroadcastReceiver
IntentFilter filter = new IntentFilter();
filter.addAction("com.geeksville.mesh.NODE_CHANGE");
context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);

// W onReceive:
NodeInfo node = intent.getParcelableExtra("com.geeksville.mesh.NodeInfo");
Position pos = node.getPosition();
// Przekaż pozycję do warstwy mapy przez ViewModel lub callback
```

**Oczekiwany rezultat:**
Aplikacja powinna automatycznie wyświetlać markery na mapie dla wszystkich węzłów Meshtastic, które mają prawidłową pozycję GPS. Markery powinny aktualizować się automatycznie gdy węzły zmieniają pozycję lub pojawiają się nowe węzły w sieci.

---

## Dodatkowe informacje

### Sprawdzanie stanu połączenia

```java
if (meshServiceManager.isConnected()) {
    String myNodeId = meshServiceManager.getMyNodeID();
    MyNodeInfo myInfo = meshServiceManager.getMyNodeInfo();
    String connectionState = meshServiceManager.getService().connectionState();
}
```

### Filtrowanie węzłów

Możesz filtrować węzły według różnych kryteriów:

```java
// Tylko węzły z prawidłową pozycją
if (nodeInfo.getPosition() != null && 
    nodeInfo.getPosition().isValid()) {
    // Wyświetl
}

// Tylko węzły online (ostatnio słyszane)
if (nodeInfo.isOnline()) {
    // Wyświetl
}

// Tylko węzły na określonym kanale
if (nodeInfo.getChannel() == preferredChannel) {
    // Wyświetl
}
```

### Aktualizacja pozycji

Pozycje są automatycznie aktualizowane przez broadcast `ACTION_NODE_CHANGE`. Każda aktualizacja pozycji węzła wywołuje `onReceive()` z nowym `NodeInfo`, który możesz użyć do zaktualizowania markera na mapie.

---

## Podsumowanie

Ta dokumentacja zawiera wszystkie informacje potrzebne do stworzenia aplikacji wyświetlającej pozycje węzłów Meshtastic. Kluczowe elementy:

1. ✅ Połączenie z IMeshService przez MeshServiceManager
2. ✅ Nasłuchiwanie broadcastów ACTION_NODE_CHANGE
3. ✅ Przetwarzanie NodeInfo i Position
4. ✅ Przekazywanie pozycji do warstwy mapy przez callback/ViewModel
5. ✅ Zarządzanie cyklem życia i zasobami

Gotowy prompt można użyć bezpośrednio z AI do automatycznego wygenerowania kodu.

