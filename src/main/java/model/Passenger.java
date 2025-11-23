package model;

import java.time.LocalDate;

public class Passenger {
     private int id;
    private String firstName;
    private String lastName;
    private String documentType;
    private String documentNumber;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private int frequentCounter;

    public Passenger() {
    }

    public Passenger(int id, String firstName, String lastName, String documentType, String documentNumber, LocalDate dateOfBirth, String email, String phone, int frequentCounter) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phone = phone;
        this.frequentCounter = frequentCounter;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getFrequentCounter() {
        return frequentCounter;
    }

    public void setFrequentCounter(int frequentCounter) {
        this.frequentCounter = frequentCounter;
    }

    
    
    
}
