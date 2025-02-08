package org.example.Classes;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Customer {
    private final StringProperty customerID;
    private final StringProperty customerName;
    private final StringProperty phoneNumber;
    private final StringProperty address;
    private final StringProperty gender;
    private final StringProperty dob;
    private final StringProperty status;
    private final StringProperty entryDate;
    private final StringProperty exitDate;
    private final StringProperty notes;

    public Customer(String customerID, String customerName, String phoneNumber, String address, String gender,
            String dob, String status, String entryDate, String exitDate, String notes) {
        this.customerID = new SimpleStringProperty(this, "customerID", customerID);
        this.customerName = new SimpleStringProperty(this, "customerName", customerName);
        this.phoneNumber = new SimpleStringProperty(this, "phoneNumber", phoneNumber);
        this.address = new SimpleStringProperty(this, "address", address);
        this.gender = new SimpleStringProperty(this, "gender", gender);
        this.dob = new SimpleStringProperty(this, "dob", dob);
        this.status = new SimpleStringProperty(this, "status", status);
        this.entryDate = new SimpleStringProperty(this, "entryDate", entryDate);
        this.exitDate = new SimpleStringProperty(this, "exitDate", exitDate);
        this.notes = new SimpleStringProperty(this, "notes", notes);
    }

    public StringProperty customerIDProperty() { return customerID; }
    public String getCustomerID() { return customerID.get(); }
    public void setCustomerID(String value) { customerID.set(value); }

    public StringProperty customerNameProperty() { return customerName; }
    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String value) { customerName.set(value); }

    public StringProperty phoneNumberProperty() { return phoneNumber; }
    public String getPhoneNumber() { return phoneNumber.get(); }
    public void setPhoneNumber(String value) { phoneNumber.set(value); }

    public StringProperty addressProperty() { return address; }
    public String getAddress() { return address.get(); }
    public void setAddress(String value) { address.set(value); }

    public StringProperty genderProperty() { return gender; }
    public String getGender() { return gender.get(); }
    public void setGender(String value) { gender.set(value); }

    public StringProperty dobProperty() { return dob; }
    public String getDOB() { return dob.get(); }
    public void setDOB(String value) { dob.set(value); }

    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }

    public StringProperty entryDateProperty() { return entryDate; }
    public String getEntryDate() { return entryDate.get(); }
    public void setEntryDate(String value) { entryDate.set(value); }

    public StringProperty exitDateProperty() { return exitDate; }
    public String getExitDate() { return exitDate.get(); }
    public void setExitDate(String value) { exitDate.set(value); }

    public StringProperty notesProperty() { return notes; }
    public String getNotes() { return notes.get(); }
    public void setNotes(String value) { notes.set(value); }
}


