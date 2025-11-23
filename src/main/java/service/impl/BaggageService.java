/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service.impl;

import dao.impl.BaggageDao;
import java.util.List;
import java.util.UUID;
import model.Baggage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio para gestión de equipaje.
 * Baggage está vinculado a CheckIn, no a Passenger directamente.
 */
public class BaggageService {
    private static final Logger logger = LoggerFactory.getLogger(BaggageService.class);
    private final BaggageDao dao = new BaggageDao();

    /**
     * Registra equipaje para un check-in específico.
     * @param checkInId ID del check-in
     * @param peso Peso en kg
     * @param piezas Número de piezas
     * @param tipo Tipo de equipaje (maleta, mochila, etc.)
     * @return true si se insertó correctamente
     */
    public boolean registrarEquipaje(int checkInId, double peso, int piezas, String tipo) {
        String tag = "TAG-" + UUID.randomUUID();
        
        Baggage b = new Baggage(0, checkInId, peso, piezas, tag, tipo);
        boolean result = dao.insert(b);
        
        if (result) {
            logger.info("Equipaje registrado: CheckInID={}, Peso={}, Piezas={}, Tag={}", 
                checkInId, peso, piezas, tag);
        } else {
            logger.warn("Error al registrar equipaje para CheckInID={}", checkInId);
        }
        
        return result;
    }

    /**
     * Lista equipaje por check-in.
     * @param checkInId ID del check-in
     * @return Lista de equipaje
     */
    public List<Baggage> listarPorCheckIn(int checkInId) {
        logger.debug("Listando equipaje para CheckInID={}", checkInId);
        return dao.listByCheckIn(checkInId);
    }
}
