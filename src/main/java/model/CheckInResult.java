package model;

public class CheckInResult {
    private int checkinId;
    private String pdfPath;
    private String boardingCode;
    private String seatCode;
    private double baggageCharge;

    public CheckInResult() {}

    public CheckInResult(int checkinId, String pdfPath, String boardingCode, String seatCode) {
        this(checkinId, pdfPath, boardingCode, seatCode, 0.0);
    }

    public CheckInResult(int checkinId, String pdfPath, String boardingCode, String seatCode, double baggageCharge) {
        this.checkinId = checkinId;
        this.pdfPath = pdfPath;
        this.boardingCode = boardingCode;
        this.seatCode = seatCode;
        this.baggageCharge = baggageCharge;
    }

    // Getters / setters
    public int getCheckinId() { return checkinId; }
    public void setCheckinId(int checkinId) { this.checkinId = checkinId; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public String getBoardingCode() { return boardingCode; }
    public void setBoardingCode(String boardingCode) { this.boardingCode = boardingCode; }
    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }
    public double getBaggageCharge() { return baggageCharge; }
    public void setBaggageCharge(double baggageCharge) { this.baggageCharge = baggageCharge; }
}
