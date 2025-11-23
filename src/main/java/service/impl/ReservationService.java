package service.impl;

import dao.impl.ReservationDao;
import dao.impl.PassengerDao;
import model.Reservation;
import model.Passenger;

import java.util.List;

public class ReservationService {
    private final ReservationDao reservationDao = new ReservationDao();
    private final PassengerDao passengerDao = new PassengerDao();

    public Reservation buscarPorPNR(String pnr) {
        return reservationDao.findByPNR(pnr);
    }

    public Reservation buscarPorDocumento(String doc) {
        Passenger p = passengerDao.findByDocument(doc);
        if (p == null) return null;
        return reservationDao.findByPassengerId(p.getId());
    }

    public List<Reservation> listarPorVuelo(int flightId) {
        return reservationDao.listByFlight(flightId);
    }

    public boolean cancelarReserva(int id) {
        return reservationDao.updateStatus(id, "Cancelled");
    }

    public boolean crearReserva(Reservation r) {
        return reservationDao.insert(r);
    }
}
