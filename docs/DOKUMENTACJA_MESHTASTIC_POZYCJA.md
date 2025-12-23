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

Aplikacja Meshtastic ATAK Plugin składa się z następujących kluczowych komponentów:

1. **MeshServiceManager** - Zarządza połączeniem z serwisem IMeshService aplikacji Meshtastic
2. **MeshtasticReceiver** - Odbiera broadcasty z Meshtastic (pozycje, węzły, wiadomości)
3. **MeshtasticMapComponent** - Główny komponent integrujący z ATAK
4. **CotEventProcessor** - Przetwarza zdarzenia CoT (Cursor on Target)

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
NodeInfo → Position → CoT Event
    ↓
CotMapComponent.dispatch()
    ↓
ATAK Map (wyświetlenie markera)
```

---

## IMeshService - Interfejs komunikacji

### Lokalizacja pliku AIDL

Plik interfejsu znajduje się w:
```
C:\Users\slawi\repos\ATAK-Plugin\app\src\main\aidl\org\meshtastic\core\service\IMeshService.aidl
```

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
import com.atakmap.android.meshtastic.service.MeshServiceManager;
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
import com.atakmap.android.meshtastic.util.Constants;

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

Dla aplikacji ATAK, pozycje są wyświetlane jako CoT Events:

```java
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.android.cot.CotMapComponent;

private void displayNodePosition(NodeInfo nodeInfo) {
    if (nodeInfo == null || nodeInfo.getUser() == null) {
        return;
    }
    
    Position position = nodeInfo.getPosition();
    if (position == null) {
        Log.d(TAG, "Węzeł " + nodeInfo.getUser().getId() + " nie ma pozycji");
        return;
    }
    
    // Sprawdź czy pozycja jest prawidłowa
    if (position.getLatitude() == 0.0 && position.getLongitude() == 0.0) {
        Log.d(TAG, "Węzeł " + nodeInfo.getUser().getId() + " ma nieprawidłową pozycję (0,0)");
        return;
    }
    
    // Utwórz CoT Event
    CotEvent cotEvent = new CotEvent();
    
    // Ustaw czas
    CoordinatedTime time = new CoordinatedTime();
    cotEvent.setTime(time);
    cotEvent.setStart(time);
    cotEvent.setStale(time.addMinutes(10)); // Pozycja ważna przez 10 minut
    
    // Ustaw UID (używamy ID węzła Meshtastic)
    cotEvent.setUID(nodeInfo.getUser().getId());
    
    // Ustaw typ (a-f-G-E-S = sensor)
    cotEvent.setType("a-f-G-E-S");
    
    // Ustaw sposób (m-g = mesh/gps)
    cotEvent.setHow("m-g");
    
    // Ustaw pozycję
    CotPoint cotPoint = new CotPoint(
        position.getLatitude(),
        position.getLongitude(),
        position.getAltitude(),
        CotPoint.UNKNOWN, // precision
        CotPoint.UNKNOWN  // hae
    );
    cotEvent.setPoint(cotPoint);
    
    // Dodaj szczegóły
    CotDetail cotDetail = new CotDetail("detail");
    
    // Informacje o kontakcie
    CotDetail contactDetail = new CotDetail("contact");
    contactDetail.setAttribute("callsign", nodeInfo.getUser().getLongName());
    contactDetail.setAttribute("endpoint", "0.0.0.0:4242:tcp");
    cotDetail.addChild(contactDetail);
    
    // Grupa (opcjonalnie)
    CotDetail groupDetail = new CotDetail("__group");
    groupDetail.setAttribute("role", "Team Member");
    groupDetail.setAttribute("name", "Meshtastic");
    cotDetail.addChild(groupDetail);
    
    // Status (bateria, jeśli dostępna)
    if (nodeInfo.getDeviceMetrics() != null) {
        CotDetail statusDetail = new CotDetail("status");
        statusDetail.setAttribute("battery", 
            String.valueOf(nodeInfo.getDeviceMetrics().getBatteryLevel()));
        cotDetail.addChild(statusDetail);
    }
    
    // Informacje o urządzeniu
    CotDetail takvDetail = new CotDetail("takv");
    takvDetail.setAttribute("platform", "Meshtastic");
    takvDetail.setAttribute("device", nodeInfo.getUser().getHwModelString());
    takvDetail.setAttribute("version", "1.0");
    cotDetail.addChild(takvDetail);
    
    // UID detail
    CotDetail uidDetail = new CotDetail("uid");
    uidDetail.setAttribute("Droid", nodeInfo.getUser().getLongName());
    cotDetail.addChild(uidDetail);
    
    // Oznacz jako pochodzący z Meshtastic (aby uniknąć pętli)
    CotDetail meshDetail = new CotDetail("__meshtastic");
    cotDetail.addChild(meshDetail);
    
    cotEvent.setDetail(cotDetail);
    
    // Wyślij do ATAK
    if (cotEvent.isValid()) {
        CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
        Log.d(TAG, "Wyświetlono pozycję węzła: " + nodeInfo.getUser().getLongName());
    } else {
        Log.e(TAG, "CoT Event nie jest prawidłowy");
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

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.meshtastic.service.MeshServiceManager;
import com.atakmap.android.meshtastic.util.Constants;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import org.meshtastic.core.model.NodeInfo;
import org.meshtastic.core.model.Position;

import java.util.List;

public class MeshtasticNodePositionDisplay {
    private static final String TAG = "MeshtasticNodePos";
    
    private Context context;
    private MeshServiceManager meshServiceManager;
    private BroadcastReceiver receiver;
    
    public MeshtasticNodePositionDisplay(Context context) {
        this.context = context;
        initialize();
    }
    
    private void initialize() {
        // Inicjalizuj MeshServiceManager
        meshServiceManager = MeshServiceManager.getInstance(context);
        
        // Ustaw listener połączenia
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
        
        // Utwórz i zarejestruj receiver
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
        
        // Połącz z serwisem
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
        if (!meshServiceManager.isConnected()) {
            return;
        }
        
        List<NodeInfo> nodes = meshServiceManager.getNodes();
        if (nodes != null) {
            for (NodeInfo node : nodes) {
                displayNode(node);
            }
        }
    }
    
    private void displayNode(NodeInfo nodeInfo) {
        if (nodeInfo == null || nodeInfo.getUser() == null) {
            return;
        }
        
        Position position = nodeInfo.getPosition();
        if (position == null || 
            (position.getLatitude() == 0.0 && position.getLongitude() == 0.0)) {
            return; // Brak prawidłowej pozycji
        }
        
        // Utwórz CoT Event
        CotEvent cotEvent = createCotEvent(nodeInfo, position);
        
        if (cotEvent.isValid()) {
            CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
            Log.d(TAG, "Wyświetlono węzeł: " + nodeInfo.getUser().getLongName());
        }
    }
    
    private CotEvent createCotEvent(NodeInfo nodeInfo, Position position) {
        CotEvent cotEvent = new CotEvent();
        
        CoordinatedTime time = new CoordinatedTime();
        cotEvent.setTime(time);
        cotEvent.setStart(time);
        cotEvent.setStale(time.addMinutes(10));
        
        cotEvent.setUID(nodeInfo.getUser().getId());
        cotEvent.setType("a-f-G-E-S");
        cotEvent.setHow("m-g");
        
        CotPoint cotPoint = new CotPoint(
            position.getLatitude(),
            position.getLongitude(),
            position.getAltitude(),
            CotPoint.UNKNOWN,
            CotPoint.UNKNOWN
        );
        cotEvent.setPoint(cotPoint);
        
        CotDetail detail = new CotDetail("detail");
        
        // Contact
        CotDetail contact = new CotDetail("contact");
        contact.setAttribute("callsign", nodeInfo.getUser().getLongName());
        contact.setAttribute("endpoint", "0.0.0.0:4242:tcp");
        detail.addChild(contact);
        
        // Group
        CotDetail group = new CotDetail("__group");
        group.setAttribute("role", "Team Member");
        group.setAttribute("name", "Meshtastic");
        detail.addChild(group);
        
        // Status (bateria)
        if (nodeInfo.getDeviceMetrics() != null) {
            CotDetail status = new CotDetail("status");
            status.setAttribute("battery", 
                String.valueOf(nodeInfo.getDeviceMetrics().getBatteryLevel()));
            detail.addChild(status);
        }
        
        // TAKV
        CotDetail takv = new CotDetail("takv");
        takv.setAttribute("platform", "Meshtastic");
        takv.setAttribute("device", nodeInfo.getUser().getHwModelString());
        detail.addChild(takv);
        
        // UID
        CotDetail uid = new CotDetail("uid");
        uid.setAttribute("Droid", nodeInfo.getUser().getLongName());
        detail.addChild(uid);
        
        // Marker Meshtastic
        CotDetail mesh = new CotDetail("__meshtastic");
        detail.addChild(mesh);
        
        cotEvent.setDetail(detail);
        return cotEvent;
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
   - Dla każdego węzła z prawidłową pozycją utwórz `CotEvent`:
     - UID: `nodeInfo.getUser().getId()`
     - Type: `"a-f-G-E-S"` (sensor)
     - How: `"m-g"` (mesh/gps)
     - Point: `new CotPoint(latitude, longitude, altitude, UNKNOWN, UNKNOWN)`
     - Detail zawierający:
       - `contact` z callsign i endpoint
       - `__group` z role="Team Member"
       - `status` z battery (jeśli dostępne)
       - `takv` z platform="Meshtastic"
       - `uid` z Droid=nazwa
       - `__meshtastic` (marker aby uniknąć pętli)
   - Wyślij przez `CotMapComponent.getInternalDispatcher().dispatch(cotEvent)`

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
// Utwórz i wyślij CotEvent
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
4. ✅ Tworzenie i wysyłanie CotEvent do wyświetlenia na mapie
5. ✅ Zarządzanie cyklem życia i zasobami

Gotowy prompt można użyć bezpośrednio z AI do automatycznego wygenerowania kodu.

