package service.impl;

import config.db.DBConnection;
import dao.impl.*;
import model.*;
import model.Baggage;
import dao.impl.BaggageDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.QRGenerator;
import util.PDFGenerator;
import util.exception.CheckInException;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Lógica de negocio para el proceso de check-in.
 * Implementa transacciones ACID, auditoría y validaciones.
 */
public class CheckInService {
    private static final Logger logger = LoggerFactory.getLogger(CheckInService.class);
    
    private final ReservationDao reservationDao = new ReservationDao();
    private final PassengerDao passengerDao = new PassengerDao();
    private final SeatDao seatDao = new SeatDao();
    private final CheckInDao checkInDao = new CheckInDao();
    private final AuditService auditService = AuditService.getInstance();

    /**
     * Busca reserva por PNR con validación.
     */
    public Reservation buscarReservaPorPNR(String pnr) throws CheckInException {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new CheckInException("PNR no puede estar vacío", "INVALID_PNR");
        }
        String trimmed = pnr.trim();
        if (trimmed.length() > 20 || !trimmed.matches("[A-Z0-9]{1,20}")) {
            throw new CheckInException("PNR inválido: máximo 20 caracteres alfanuméricos (A-Z,0-9)", "INVALID_PNR");
        }

        Reservation res = reservationDao.findByPNR(pnr.trim());
        if (res == null) {
            logger.warn("Reservación no encontrada para PNR: {}", pnr);
            throw new CheckInException("Reservación no encontrada para PNR: " + pnr, "RESERVATION_NOT_FOUND", pnr);
        }

        res.setPassenger(passengerDao.findById(res.getPassengerId()));
        logger.info("Reservación encontrada: PNR={}, PassengerID={}", pnr, res.getPassengerId());
        return res;
    }

    /**
     * Busca reserva por número de documento.
     */
    public Reservation buscarReservaPorDoc(String docNumber) throws CheckInException {
        Passenger p = passengerDao.findByDocument(docNumber);
        if (p == null) {
            logger.warn("Pasajero no encontrado con documento: {}", docNumber);
            throw new CheckInException("Pasajero no encontrado", "PASSENGER_NOT_FOUND", docNumber);
        }

        Reservation res = reservationDao.findByPassengerId(p.getId());
        if (res == null) {
            logger.warn("Reservación no encontrada para pasajero: {}", p.getId());
            throw new CheckInException("Sin reservación para pasajero", "RESERVATION_NOT_FOUND", p.getId());
        }

        res.setPassenger(p);
        logger.info("Reservación encontrada: PassengerID={}, Document={}", p.getId(), docNumber);
        return res;
    }

    /**
     * Valida que el pasajero tenga documento.
     */
    public boolean validarDocumento(Reservation res) {
        if (res == null || res.getPassenger() == null) {
            logger.warn("Validación de documento fallida: reservación o pasajero nulo");
            return false;
        }
        String doc = res.getPassenger().getDocumentNumber();
        boolean valid = doc != null && !doc.isBlank();
        logger.info("Validación de documento: {}", valid ? "exitosa" : "fallida");
        return valid;
    }

    /**
     * Verifica si ya hay check-in para esa reserva.
     */
    public boolean yaCheckeado(int reservationId) {
        boolean checked = checkInDao.existsByReservation(reservationId);
        logger.info("Check-in previo verificado para reservación {}: {}", reservationId, checked);
        return checked;
    }

    /**
     * Devuelve lista de asientos disponibles para el vuelo.
     */
    public List<Seat> listarAsientosDisponibles(int flightId) {
        List<Seat> seats = seatDao.listAvailableSeatsByFlight(flightId);
        logger.info("Asientos disponibles para vuelo {}: {}", flightId, seats.size());
        return seats;
    }

    /**
     * Devuelve todos los asientos del vuelo (ocupados y libres) para mostrar mapa visual.
     */
    public List<Seat> listarTodosAsientosPorVuelo(int flightId) {
        List<Seat> seats = seatDao.listSeatsByFlight(flightId);
        logger.info("Asientos totales para vuelo {}: {}", flightId, seats.size());
        return seats;
    }

    /**
     * Asigna asiento manual de forma segura.
     */
    public boolean asignarAsientoManual(int seatId, int reservationId) throws CheckInException {
        try {
            boolean assigned = seatDao.assignSeat(seatId, reservationId);
            if (assigned) {
                logger.info("Asiento {} asignado manualmente a reservación: {}", seatId, reservationId);
                return true;
            } else {
                logger.warn("No se pudo asignar asiento {} - posiblemente ocupado", seatId);
                throw new CheckInException("Asiento no disponible o ya ocupado", "SEAT_OCCUPIED", seatId);
            }
        } catch (Exception e) {
            logger.error("Error al asignar asiento manualmente", e);
            throw new CheckInException("Error al asignar asiento", "SEAT_ASSIGNMENT_ERROR", e);
        }
    }

    /**
     * Asignación automática: toma primer asiento disponible.
     */
    public Integer asignarAsientoAutomatico(int flightId, int reservationId) throws CheckInException {
        List<Seat> seats = listarAsientosDisponibles(flightId);
        if (seats.isEmpty()) {
            logger.error("No hay asientos disponibles para vuelo: {}", flightId);
            throw new CheckInException("No hay asientos disponibles", "NO_AVAILABLE_SEATS", flightId);
        }

        Seat s = seats.get(0);
        try {
            boolean ok = asignarAsientoManual(s.getId(), reservationId);
            return ok ? s.getId() : null;
        } catch (CheckInException e) {
            logger.error("Error en asignación automática de asiento", e);
            throw e;
        }
    }

    /**
     * Realiza el check-in completo con transacción ACID.
     */
    public CheckInResult realizarCheckIn(Reservation reservation, int agentUserId, Integer seatId, java.util.List<Baggage> baggageList) 
            throws CheckInException {
        if (reservation == null) {
            throw new CheckInException("Reservación no válida", "INVALID_RESERVATION");
        }

        if (yaCheckeado(reservation.getId())) {
            logger.warn("Intento de check-in duplicado para reservación: {}", reservation.getId());
            throw new CheckInException("Ya existe check-in para esta reservación", "DUPLICATE_CHECKIN");
        }

        try {
            // Validar documento del pasajero
            if (!validarDocumento(reservation)) {
                auditService.logAccessDenied(agentUserId, "CHECK_IN", "Documento inválido o faltante");
                throw new CheckInException("Documento del pasajero inválido", "INVALID_DOCUMENT");
            }

            // Asignar asiento: si el seatId viene null, asignación automática; si viene no null
            // asumimos que la asignación manual ya se realizó por la UI (evitamos doble asignación).
            Integer assignedSeatId = seatId;
            if (assignedSeatId == null) {
                assignedSeatId = asignarAsientoAutomatico(reservation.getFlightId(), reservation.getId());
            }

            if (assignedSeatId == null) {
                throw new CheckInException("No se asignó asiento", "SEAT_ASSIGNMENT_FAILED");
            }

            // Generar código de boarding
            String boardingCode = "BP-" + reservation.getPnr() + "-" + System.currentTimeMillis();

            // Insertar check-in
            // Verificar que el agente existe (evitar FK violation silenciosa)
            dao.impl.UserDao userDao = new dao.impl.UserDao();
            if (userDao.findById(agentUserId) == null) {
                logger.error("Agente no encontrado: {}", agentUserId);
                throw new CheckInException("Agente no encontrado", "AGENT_NOT_FOUND");
            }

            logger.info("Intentando insertar check-in: ReservationID={}, AgentID={}, SeatID={}, BoardingCode={}", 
                reservation.getId(), agentUserId, assignedSeatId, boardingCode);
            int checkinId = checkInDao.insert(reservation.getId(), agentUserId, assignedSeatId, boardingCode);
            if (checkinId <= 0) {
                logger.error("Insert retornó: {}", checkinId);
                throw new CheckInException("Error al registrar check-in", "CHECKIN_INSERT_FAILED");
            }

            // Generar QR
            File qrFile = QRGenerator.generateQRCode("BP:" + boardingCode, "qr_bp_" + boardingCode + ".png");

            // Obtener código de asiento (antes de generar PDF para incluirlo)
            String seatCode = fetchSeatCode(assignedSeatId);

            // Generar PDF (incluye asiento cuando está disponible)
            String pdfPath = PDFGenerator.generateBoardingPass(reservation, qrFile, seatCode);

            // Registrar equipaje, si aplica
            double totalBaggageCharge = 0.0;
            if (baggageList != null && !baggageList.isEmpty()) {
                BaggageDao baggageDao = new BaggageDao();
                // Leer tarifas desde configuración (uso seguro de parseo)
                ConfigService cfg = ConfigService.getInstance();
                double pricePerKg = cfg.getParameterDouble("baggage.price.perKg", 5.0);
                double pricePerPiece = cfg.getParameterDouble("baggage.price.perPiece", 10.0);

                for (Baggage b : baggageList) {
                    b.setCheckInId(checkinId);
                    boolean ok = baggageDao.insert(b);
                    if (!ok) {
                        logger.warn("No se pudo insertar baggage para checkin {}: {}", checkinId, b.getTagCode());
                    }
                    double charge = b.getWeight() * pricePerKg + b.getPieces() * pricePerPiece;
                    totalBaggageCharge += charge;
                }
                logger.info("Cargos por equipaje calculados: {} para CheckIn {}", totalBaggageCharge, checkinId);
            }

            // Registrar en auditoría
            auditService.logCheckIn(agentUserId, reservation.getId(), reservation.getPnr(), assignedSeatId);
            logger.info("Check-in completado exitosamente: CheckInID={}, ReservationID={}", checkinId, reservation.getId());

            return new CheckInResult(checkinId, pdfPath, boardingCode, seatCode, totalBaggageCharge);

        } catch (CheckInException e) {
            auditService.logError(agentUserId, "CHECK_IN", e.getMessage());
            throw e;
        } catch (Exception ex) {
            logger.error("Error inesperado en check-in", ex);
            auditService.logError(agentUserId, "CHECK_IN", ex.getMessage());
            throw new CheckInException("Error durante el check-in: " + ex.getMessage(), "CHECKIN_ERROR", ex);
        }
    }

    /**
     * Obtiene código de asiento de forma segura.
     */
    private String fetchSeatCode(Integer seatId) throws CheckInException {
        if (seatId == null) return null;
        String sql = "SELECT seat_code FROM Seats WHERE seat_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("seat_code");
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            logger.error("Error al obtener código de asiento: {}", seatId, ex);
            throw new CheckInException("Error al obtener código de asiento", "SEAT_CODE_FETCH_ERROR", ex);
        }
        return null;
    }
}
