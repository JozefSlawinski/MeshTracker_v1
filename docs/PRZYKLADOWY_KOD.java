package com.example.meshtastic.position;

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

/**
 * Klasa do wyświetlania pozycji węzłów Meshtastic na mapie ATAK.
 * 
 * Użycie:
 *   MeshtasticPositionDisplay display = new MeshtasticPositionDisplay(context);
 *   // ... w onDestroy():
 *   display.cleanup();
 */
public class MeshtasticPositionDisplay {
    private static final String TAG = "MeshtasticPos";
    
    private final Context context;
    private MeshServiceManager meshServiceManager;
    private BroadcastReceiver meshtasticReceiver;
    private boolean isInitialized = false;
    
    public MeshtasticPositionDisplay(Context context) {
        this.context = context.getApplicationContext();
        initialize();
    }
    
    /**
     * Inicjalizuje połączenie z Meshtastic i rejestruje receiver.
     */
    private void initialize() {
        if (isInitialized) {
            Log.w(TAG, "Już zainicjalizowane");
            return;
        }
        
        try {
            // 1. Inicjalizuj MeshServiceManager
            meshServiceManager = MeshServiceManager.getInstance(context);
            
            // 2. Ustaw listener dla zmian połączenia
            meshServiceManager.setConnectionListener(new MeshServiceManager.ConnectionListener() {
                @Override
                public void onServiceConnected() {
                    Log.d(TAG, "Połączono z Meshtastic service");
                    // Odśwież listę węzłów przy połączeniu
                    refreshAllNodes();
                }
                
                @Override
                public void onServiceDisconnected() {
                    Log.d(TAG, "Rozłączono z Meshtastic service");
                }
            });
            
            // 3. Utwórz i zarejestruj BroadcastReceiver
            meshtasticReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handleMeshtasticBroadcast(intent);
                }
            };
            
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.ACTION_NODE_CHANGE);
            filter.addAction(Constants.ACTION_MESH_CONNECTED);
            filter.addAction(Constants.ACTION_MESH_DISCONNECTED);
            
            context.registerReceiver(meshtasticReceiver, filter, Context.RECEIVER_EXPORTED);
            Log.d(TAG, "Zarejestrowano BroadcastReceiver");
            
            // 4. Połącz z serwisem
            boolean connected = meshServiceManager.connect();
            if (connected) {
                Log.d(TAG, "Rozpoczęto połączenie z Meshtastic service");
            } else {
                Log.w(TAG, "Nie udało się rozpocząć połączenia z Meshtastic service");
            }
            
            isInitialized = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas inicjalizacji", e);
        }
    }
    
    /**
     * Obsługuje broadcasty z Meshtastic.
     */
    private void handleMeshtasticBroadcast(Intent intent) {
        if (intent == null) {
            return;
        }
        
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        
        Log.d(TAG, "Otrzymano broadcast: " + action);
        
        switch (action) {
            case Constants.ACTION_NODE_CHANGE:
                handleNodeChange(intent);
                break;
                
            case Constants.ACTION_MESH_CONNECTED:
                handleConnectionChange(intent, true);
                break;
                
            case Constants.ACTION_MESH_DISCONNECTED:
                handleConnectionChange(intent, false);
                break;
                
            default:
                Log.d(TAG, "Nieobsługiwana akcja: " + action);
                break;
        }
    }
    
    /**
     * Obsługuje zmianę węzła (pojawienie się, zniknięcie, aktualizacja pozycji).
     */
    private void handleNodeChange(Intent intent) {
        try {
            NodeInfo nodeInfo = intent.getParcelableExtra("com.geeksville.mesh.NodeInfo");
            
            if (nodeInfo == null) {
                Log.d(TAG, "NodeInfo jest null");
                return;
            }
            
            if (nodeInfo.getUser() == null) {
                Log.d(TAG, "NodeInfo.getUser() jest null");
                return;
            }
            
            String nodeId = nodeInfo.getUser().getId();
            Log.d(TAG, "Zmiana węzła: " + nodeId);
            
            // Wyświetl pozycję węzła na mapie
            displayNodePosition(nodeInfo);
            
        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas obsługi zmiany węzła", e);
        }
    }
    
    /**
     * Obsługuje zmianę stanu połączenia z radiem.
     */
    private void handleConnectionChange(Intent intent, boolean connected) {
        if (connected) {
            String state = intent.getStringExtra(Constants.EXTRA_CONNECTED);
            if (Constants.STATE_CONNECTED.equalsIgnoreCase(state)) {
                Log.d(TAG, "Radio Meshtastic połączone");
                // Odśwież wszystkie węzły
                refreshAllNodes();
            }
        } else {
            Log.d(TAG, "Radio Meshtastic rozłączone");
        }
    }
    
    /**
     * Odświeża listę wszystkich węzłów i wyświetla ich pozycje.
     */
    private void refreshAllNodes() {
        if (!meshServiceManager.isConnected()) {
            Log.w(TAG, "Serwis nie jest połączony, nie można odświeżyć węzłów");
            return;
        }
        
        try {
            List<NodeInfo> nodes = meshServiceManager.getNodes();
            if (nodes == null) {
                Log.d(TAG, "Lista węzłów jest null");
                return;
            }
            
            Log.d(TAG, "Odświeżanie " + nodes.size() + " węzłów");
            
            for (NodeInfo node : nodes) {
                displayNodePosition(node);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas odświeżania węzłów", e);
        }
    }
    
    /**
     * Wyświetla pozycję węzła na mapie jako CoT Event.
     */
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
        
        // Sprawdź czy pozycja jest w prawidłowym zakresie
        if (position.getLatitude() < -90.0 || position.getLatitude() > 90.0 ||
            position.getLongitude() < -180.0 || position.getLongitude() > 180.0) {
            Log.w(TAG, "Węzeł " + nodeInfo.getUser().getId() + " ma pozycję poza zakresem");
            return;
        }
        
        try {
            // Utwórz CoT Event
            CotEvent cotEvent = createCotEventFromNode(nodeInfo, position);
            
            if (cotEvent.isValid()) {
                // Wyślij do ATAK do wyświetlenia na mapie
                CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
                Log.d(TAG, "Wyświetlono pozycję węzła: " + nodeInfo.getUser().getLongName() + 
                          " (" + position.getLatitude() + ", " + position.getLongitude() + ")");
            } else {
                Log.e(TAG, "CoT Event nie jest prawidłowy dla węzła: " + nodeInfo.getUser().getId());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas wyświetlania pozycji węzła", e);
        }
    }
    
    /**
     * Tworzy CoT Event z informacji o węźle i jego pozycji.
     */
    private CotEvent createCotEventFromNode(NodeInfo nodeInfo, Position position) {
        CotEvent cotEvent = new CotEvent();
        
        // Ustaw czas
        CoordinatedTime time = new CoordinatedTime();
        cotEvent.setTime(time);
        cotEvent.setStart(time);
        cotEvent.setStale(time.addMinutes(10)); // Pozycja ważna przez 10 minut
        
        // Ustaw UID (używamy ID węzła Meshtastic)
        String nodeId = nodeInfo.getUser().getId();
        cotEvent.setUID(nodeId);
        
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
            CotPoint.UNKNOWN  // hae (height above ellipsoid)
        );
        cotEvent.setPoint(cotPoint);
        
        // Utwórz szczegóły
        CotDetail cotDetail = new CotDetail("detail");
        
        // Informacje o kontakcie
        CotDetail contactDetail = new CotDetail("contact");
        contactDetail.setAttribute("callsign", nodeInfo.getUser().getLongName());
        contactDetail.setAttribute("endpoint", "0.0.0.0:4242:tcp");
        cotDetail.addChild(contactDetail);
        
        // Grupa
        CotDetail groupDetail = new CotDetail("__group");
        groupDetail.setAttribute("role", "Team Member");
        groupDetail.setAttribute("name", "Meshtastic");
        cotDetail.addChild(groupDetail);
        
        // Status (bateria, jeśli dostępna)
        if (nodeInfo.getDeviceMetrics() != null && 
            nodeInfo.getDeviceMetrics().getBatteryLevel() > 0) {
            CotDetail statusDetail = new CotDetail("status");
            statusDetail.setAttribute("battery", 
                String.valueOf(nodeInfo.getDeviceMetrics().getBatteryLevel()));
            cotDetail.addChild(statusDetail);
        }
        
        // Informacje o urządzeniu (TAKV)
        CotDetail takvDetail = new CotDetail("takv");
        takvDetail.setAttribute("platform", "Meshtastic");
        String hwModel = nodeInfo.getUser().getHwModelString();
        if (hwModel != null) {
            takvDetail.setAttribute("device", hwModel);
        }
        takvDetail.setAttribute("version", "1.0");
        cotDetail.addChild(takvDetail);
        
        // UID detail
        CotDetail uidDetail = new CotDetail("uid");
        uidDetail.setAttribute("Droid", nodeInfo.getUser().getLongName());
        cotDetail.addChild(uidDetail);
        
        // Track (prędkość i kierunek, jeśli dostępne)
        if (position.getGroundSpeed() > 0 || position.getGroundTrack() > 0) {
            CotDetail trackDetail = new CotDetail("track");
            trackDetail.setAttribute("speed", String.valueOf(position.getGroundSpeed()));
            trackDetail.setAttribute("course", String.valueOf(position.getGroundTrack()));
            cotDetail.addChild(trackDetail);
        }
        
        // Oznacz jako pochodzący z Meshtastic (aby uniknąć pętli przesyłania)
        CotDetail meshDetail = new CotDetail("__meshtastic");
        cotDetail.addChild(meshDetail);
        
        cotEvent.setDetail(cotDetail);
        
        return cotEvent;
    }
    
    /**
     * Czyści zasoby i rozłącza się z serwisem.
     * Wywołaj w onDestroy() komponentu.
     */
    public void cleanup() {
        Log.d(TAG, "Czyszczenie zasobów");
        
        try {
            // Rozłącz od serwisu
            if (meshServiceManager != null) {
                meshServiceManager.disconnect();
                meshServiceManager = null;
            }
            
            // Wyrejestruj receiver
            if (meshtasticReceiver != null && context != null) {
                context.unregisterReceiver(meshtasticReceiver);
                meshtasticReceiver = null;
            }
            
            isInitialized = false;
            Log.d(TAG, "Zasoby wyczyszczone");
            
        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas czyszczenia zasobów", e);
        }
    }
    
    /**
     * Sprawdza czy komponent jest zainicjalizowany.
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Sprawdza czy serwis jest połączony.
     */
    public boolean isConnected() {
        return meshServiceManager != null && meshServiceManager.isConnected();
    }
    
    /**
     * Pobiera ID własnego węzła.
     */
    public String getMyNodeId() {
        if (meshServiceManager != null && meshServiceManager.isConnected()) {
            return meshServiceManager.getMyNodeID();
        }
        return null;
    }
}

