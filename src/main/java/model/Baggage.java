package model;


public class Baggage {
    private int id;
    private int checkInId;
    private double weight;
    private int pieces;
    private String tagCode;
    private String type;

    public Baggage() {
    }

    public Baggage(int id, int checkInId, double weight, int pieces, String tagCode, String type) {
        this.id = id;
        this.checkInId = checkInId;
        this.weight = weight;
        this.pieces = pieces;
        this.tagCode = tagCode;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCheckInId() {
        return checkInId;
    }

    public void setCheckInId(int checkInId) {
        this.checkInId = checkInId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getPieces() {
        return pieces;
    }

    public void setPieces(int pieces) {
        this.pieces = pieces;
    }

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
