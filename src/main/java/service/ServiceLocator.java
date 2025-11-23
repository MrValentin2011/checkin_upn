package service;

import service.impl.*;

/**
 * Localizador de servicios centralizado (Simple Service Locator Pattern).
 * Proporciona instancias únicas (Singleton) de servicios.
 * Facilita testing y desacoplamiento.
 */
public class ServiceLocator {
    private static ServiceLocator instance;
    
    private final AuditService auditService;
    private final AuthService authService;
    private final CheckInService checkInService;
    private final BaggageService baggageService;
    private final FlightService flightService;
    private final ReservationService reservationService;
    private final PassengerService passengerService;
    private final ReportService reportService;
    private final ConfigService configService;
    private final SyncService syncService;

    private ServiceLocator() {
        this.auditService = AuditService.getInstance();
        this.authService = AuthService.getInstance();
        this.checkInService = new CheckInService();
        this.baggageService = new BaggageService();
        this.flightService = new FlightService();
        this.reservationService = new ReservationService();
        this.passengerService = new PassengerService();
        this.reportService = new ReportService();
        this.configService = ConfigService.getInstance();
        this.syncService = new SyncService();
    }

    public static ServiceLocator getInstance() {
        if (instance == null) {
            synchronized (ServiceLocator.class) {
                if (instance == null) {
                    instance = new ServiceLocator();
                }
            }
        }
        return instance;
    }

    // Getters para los servicios
    public AuditService getAuditService() {
        return auditService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public CheckInService getCheckInService() {
        return checkInService;
    }

    public BaggageService getBaggageService() {
        return baggageService;
    }

    public FlightService getFlightService() {
        return flightService;
    }

    public ReservationService getReservationService() {
        return reservationService;
    }

    public PassengerService getPassengerService() {
        return passengerService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public SyncService getSyncService() {
        return syncService;
    }
}
