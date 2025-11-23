package model;

import java.time.LocalDateTime;

/**
 *
 * @author USER
 */
public class CheckIn {

    private int id;
    private int reservationId;
    private int agentUserId;
    private int seatId;
    private LocalDateTime checkInDate;
    private String status;
    private String boardingPassCode;
    private String boardingPassPath;

    public CheckIn() {
    }

    public CheckIn(int id, int reservationId, int agentUserId, int seatId, LocalDateTime checkInDate, String status, String boardingPassCode, String boardingPassPath) {
        this.id = id;
        this.reservationId = reservationId;
        this.agentUserId = agentUserId;
        this.seatId = seatId;
        this.checkInDate = checkInDate;
        this.status = status;
        this.boardingPassCode = boardingPassCode;
        this.boardingPassPath = boardingPassPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public int getAgentUserId() {
        return agentUserId;
    }

    public void setAgentUserId(int agentUserId) {
        this.agentUserId = agentUserId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public LocalDateTime getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDateTime checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBoardingPassCode() {
        return boardingPassCode;
    }

    public void setBoardingPassCode(String boardingPassCode) {
        this.boardingPassCode = boardingPassCode;
    }

    public String getBoardingPassPath() {
        return boardingPassPath;
    }

    public void setBoardingPassPath(String boardingPassPath) {
        this.boardingPassPath = boardingPassPath;
    }

    
}
