/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author USER
 */
public class Seat {
    private int id;
    private int flightId;
    private String seatCode;
    private String seatClass;
    private boolean occupied;
    private Integer reservationId;

    public Seat() {
    }

    public Seat(int id, int flightId, String seatCode, String seatClass, boolean occupied, Integer reservationId) {
        this.id = id;
        this.flightId = flightId;
        this.seatCode = seatCode;
        this.seatClass = seatClass;
        this.occupied = occupied;
        this.reservationId = reservationId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFlightId() {
        return flightId;
    }

    public void setFlightId(int flightId) {
        this.flightId = flightId;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }
    
    
}
