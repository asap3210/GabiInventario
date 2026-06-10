package com.inventario.app;

/** Representa una fila de la tabla de inventario: un objeto ubicado en una sala. */
public class InventoryItem {
    public long id;
    public String objectCode;   // código del objeto
    public String roomCode;     // código/sala donde se encuentra
    public String firstSeen;    // timestamp del primer escaneo
    public String lastSeen;     // timestamp del último escaneo
    public int scanCount;       // número de veces escaneado

    public InventoryItem(long id, String objectCode, String roomCode,
                         String firstSeen, String lastSeen, int scanCount) {
        this.id = id;
        this.objectCode = objectCode;
        this.roomCode = roomCode;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.scanCount = scanCount;
    }
}
