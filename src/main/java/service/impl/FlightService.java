// service.impl.FlightService
package service.impl;

import dao.impl.FlightDao;
import java.util.LinkedHashMap;
import java.util.List;
import model.Flight;

public class FlightService {

    private final FlightDao dao = new FlightDao();

    public List<Flight> listarVuelos(String filtro) {
        return dao.listAll(filtro);
    }

    public boolean registrarVuelo(Flight vuelo, int airlineId, int originId, int destinationId) {
        // Mantengo el método existente (sin asientos) por compatibilidad
        return dao.insert(vuelo, airlineId, originId, destinationId);
    }

    // === NUEVO: registra vuelo + asientos ===
    public boolean registrarVueloConAsientos(Flight vuelo, int airlineId, int originId, int destinationId,
                                             LinkedHashMap<String,Integer> classDistribution, int seatsPerRow) {
        return dao.insertWithSeats(vuelo, airlineId, originId, destinationId, classDistribution, seatsPerRow);
    }

    public boolean actualizarVuelo(Flight vuelo, int airlineId, int originId, int destinationId) {
        return dao.update(vuelo, airlineId, originId, destinationId);
    }

    public boolean eliminarVuelo(int id) {
        return dao.delete(id);
    }
}
