package org.meshtastic.core.service;

import org.meshtastic.core.model.NodeInfo;
import org.meshtastic.core.model.MyNodeInfo;

interface IMeshService {
    /**
     * Pobiera listę wszystkich węzłów w sieci Meshtastic.
     * @return Lista węzłów NodeInfo
     */
    List<NodeInfo> getNodes();
    
    /**
     * Pobiera informacje o własnym węźle.
     * @return Informacje o własnym węźle
     */
    MyNodeInfo getMyNodeInfo();
    
    /**
     * Pobiera unikalny ID własnego węzła.
     * @return ID węzła jako String
     */
    String getMyId();
    
    /**
     * Sprawdza stan połączenia z radiem Meshtastic.
     * @return Stan połączenia jako String ("CONNECTED" lub "DISCONNECTED")
     */
    String connectionState();
}

