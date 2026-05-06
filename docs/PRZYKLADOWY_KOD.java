package com.example.meshtastic.position;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.meshtastic.core.model.NodeInfo;
import org.meshtastic.core.model.Position;

import java.util.List;

/**
 * Klasa do odbierania pozycji węzłów Meshtastic i przekazywania ich do warstwy mapy.
 *
 * Użycie:
 *   MeshtasticPositionDisplay display = new MeshtasticPositionDisplay(context, listener);
 *   // ... w onDestroy():
 *   display.cleanup();
 */
public class MeshtasticPositionDisplay {
    private static final String TAG = "MeshtasticPos";

    public interface NodePositionListener {
        void onNodePositionUpdated(String nodeId, String nodeName, double lat, double lon, int altitudeMeters);
    }

    private final Context context;
    private final NodePositionListener listener;
    private MeshServiceManager meshServiceManager;
    private BroadcastReceiver meshtasticReceiver;
    private boolean isInitialized = false;

    public MeshtasticPositionDisplay(Context context, NodePositionListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        initialize();
    }

    private void initialize() {
        if (isInitialized) {
            Log.w(TAG, "Już zainicjalizowane");
            return;
        }

        try {
            meshServiceManager = MeshServiceManager.getInstance(context);

            meshServiceManager.setConnectionListener(new MeshServiceManager.ConnectionListener() {
                @Override
                public void onServiceConnected() {
                    Log.d(TAG, "Połączono z Meshtastic service");
                    refreshAllNodes();
                }

                @Override
                public void onServiceDisconnected() {
                    Log.d(TAG, "Rozłączono z Meshtastic service");
                }
            });

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

    private void handleMeshtasticBroadcast(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

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

    private void handleNodeChange(Intent intent) {
        try {
            NodeInfo nodeInfo = intent.getParcelableExtra("com.geeksville.mesh.NodeInfo");
            if (nodeInfo == null || nodeInfo.getUser() == null) return;

            Log.d(TAG, "Zmiana węzła: " + nodeInfo.getUser().getId());
            notifyPosition(nodeInfo);

        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas obsługi zmiany węzła", e);
        }
    }

    private void handleConnectionChange(Intent intent, boolean connected) {
        if (connected) {
            String state = intent.getStringExtra(Constants.EXTRA_CONNECTED);
            if (Constants.STATE_CONNECTED.equalsIgnoreCase(state)) {
                Log.d(TAG, "Radio Meshtastic połączone");
                refreshAllNodes();
            }
        } else {
            Log.d(TAG, "Radio Meshtastic rozłączone");
        }
    }

    private void refreshAllNodes() {
        if (!meshServiceManager.isConnected()) {
            Log.w(TAG, "Serwis nie jest połączony, nie można odświeżyć węzłów");
            return;
        }

        try {
            List<NodeInfo> nodes = meshServiceManager.getNodes();
            if (nodes == null) return;

            Log.d(TAG, "Odświeżanie " + nodes.size() + " węzłów");
            for (NodeInfo node : nodes) {
                notifyPosition(node);
            }

        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas odświeżania węzłów", e);
        }
    }

    private void notifyPosition(NodeInfo nodeInfo) {
        if (nodeInfo == null || nodeInfo.getUser() == null) return;

        Position position = nodeInfo.getPosition();
        if (position == null) {
            Log.d(TAG, "Węzeł " + nodeInfo.getUser().getId() + " nie ma pozycji");
            return;
        }

        if (position.getLatitude() == 0.0 && position.getLongitude() == 0.0) {
            Log.d(TAG, "Węzeł " + nodeInfo.getUser().getId() + " ma nieprawidłową pozycję (0,0)");
            return;
        }

        if (position.getLatitude() < -90.0 || position.getLatitude() > 90.0 ||
            position.getLongitude() < -180.0 || position.getLongitude() > 180.0) {
            Log.w(TAG, "Węzeł " + nodeInfo.getUser().getId() + " ma pozycję poza zakresem");
            return;
        }

        Log.d(TAG, "Pozycja węzła: " + nodeInfo.getUser().getLongName() +
                   " (" + position.getLatitude() + ", " + position.getLongitude() + ")");

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
        Log.d(TAG, "Czyszczenie zasobów");
        try {
            if (meshServiceManager != null) {
                meshServiceManager.disconnect();
                meshServiceManager = null;
            }
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

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isConnected() {
        return meshServiceManager != null && meshServiceManager.isConnected();
    }

    public String getMyNodeId() {
        if (meshServiceManager != null && meshServiceManager.isConnected()) {
            return meshServiceManager.getMyNodeID();
        }
        return null;
    }
}
