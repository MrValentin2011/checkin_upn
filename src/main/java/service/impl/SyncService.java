/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service.impl;

import java.util.List;
import offline.localdb.OfflineDBManager;
import offline.sync.SyncQueue;

/**
 *
 * @author USER
 */
public class SyncService {

    private final OfflineDBManager localDb = new OfflineDBManager();
    private final SyncQueue syncQueue = new SyncQueue();

    public void sincronizarPendientes() {
        List<String> operaciones = localDb.obtenerPendientes();
        for (String op : operaciones) {
            if (syncQueue.enviarOperacion(op)) {
                localDb.marcarComoSincronizado(op);
            }
        }
    }
}
