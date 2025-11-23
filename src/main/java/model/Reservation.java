package model;

import java.time.LocalDateTime;


public class Reservation {
    private int id;
    private String pnr;
    private int passengerId;
    private int flightId;
    private String status;
    private String seatPreference;
    private LocalDateTime createdAt;
    
    // Relación con pasajero (útil en UI y PDF)
    private Passenger passenger;

    public Reservation() {
    }

    public Passenger getPassenger() { return passenger; }
    public void setPassenger(Passenger passenger) { this.passenger = passenger; }

    public String getSeatPreference() { return seatPreference; }
    public void setSeatPreference(String seatPreference) { this.seatPreference = seatPreference; }

    public Reservation(int id, String pnr, int passengerId, int flightId, String status, String seatPreference, LocalDateTime createdAt, Passenger passenger) {
        this.id = id;
        this.pnr = pnr;
        this.passengerId = passengerId;
        this.flightId = flightId;
        this.status = status;
        this.seatPreference = seatPreference;
        this.createdAt = createdAt;
        this.passenger = passenger;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public int getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(int passengerId) {
        this.passengerId = passengerId;
    }

    public int getFlightId() {
        return flightId;
    }

    public void setFlightId(int flightId) {
        this.flightId = flightId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    
}
