package org.example.Classes;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Savings {
    private final StringProperty savingsID;
    private final StringProperty customerID;
    private final StringProperty customerName;
    private final StringProperty date;
    private final StringProperty principal;
    private final StringProperty mandatory;
    private final StringProperty capital;
    private final StringProperty voluntary;
    private final StringProperty others;
    private final StringProperty total;
    private final StringProperty notes;
    private final StringProperty status;


    public Savings(String savingsID, String customerID, String customerName, String date,
                   String principal, String mandatory, String capital, String voluntary,
                   String others, String total, String notes, String status) {
        this.savingsID = new SimpleStringProperty(this, "savingsID", savingsID);
        this.customerID = new SimpleStringProperty(this, "customerID", customerID);
        this.customerName = new SimpleStringProperty(this, "customerName", customerName);
        this.date = new SimpleStringProperty(this, "date", date);
        this.principal = new SimpleStringProperty(this, "principal", principal);
        this.mandatory = new SimpleStringProperty(this, "mandatory", mandatory);
        this.capital = new SimpleStringProperty(this, "capital", capital);
        this.voluntary = new SimpleStringProperty(this, "voluntary", voluntary);
        this.others = new SimpleStringProperty(this, "others", others);
        this.total = new SimpleStringProperty(this, "total", total);
        this.notes = new SimpleStringProperty(this, "notes", notes);
        this.status = new SimpleStringProperty(this, "status", status);
    }

    public StringProperty savingsIDProperty() { return savingsID; }
    public String getSavingsID() { return savingsID.get(); }
    public void setSavingsID(String value) { savingsID.set(value); }

    public StringProperty customerIDProperty() { return customerID; }
    public String getCustomerID() { return customerID.get(); }
    public void setCustomerID(String value) { customerID.set(value); }

    public StringProperty customerNameProperty() { return customerName; }
    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String value) { customerName.set(value); }

    public StringProperty dateProperty() { return date; }
    public String getDate() { return date.get(); }
    public void setDate(String value) { date.set(value); }

    public StringProperty principalProperty() { return principal; }
    public String getPrincipal() { return principal.get(); }
    public void setPrincipal(String value) { principal.set(value); }

    public StringProperty mandatoryProperty() { return mandatory; }
    public String getMandatory() { return mandatory.get(); }
    public void setMandatory(String value) { mandatory.set(value); }

    public StringProperty capitalProperty() { return capital; }
    public String getCapital() { return capital.get(); }
    public void setCapital(String value) { capital.set(value); }

    public StringProperty voluntaryProperty() { return voluntary; }
    public String getVoluntary() { return voluntary.get(); }
    public void setVoluntary(String value) { voluntary.set(value); }

    public StringProperty othersProperty() { return others; }
    public String getOthers() { return others.get(); }
    public void setOthers(String value) { others.set(value); }

    public StringProperty totalProperty() { return total; }
    public String getTotal() { return total.get(); }
    public void setTotal(String value) { total.set(value); }

    public StringProperty notesProperty() { return notes; }
    public String getNotes() { return notes.get(); }
    public void setNotes(String value) { notes.set(value); }

    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
}
