package org.example.Classes;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Expired {
    private final StringProperty expiredID;
    private final StringProperty date;
    private final StringProperty collateral;
    private final StringProperty sale;
    private final StringProperty profit;
    private final StringProperty itemID;
    private final StringProperty itemName;
    private final StringProperty customerID;
    private final StringProperty customerName;
    private final StringProperty notes;

    public Expired(String expiredID, String date, String collateral, String sale, String profit, String itemID,
                   String itemName, String customerID, String customerName, String notes) {
        this.expiredID = new SimpleStringProperty(this, "expiredID", expiredID);
        this.date = new SimpleStringProperty(this, "date", date);
        this.collateral = new SimpleStringProperty(this, "collateral", collateral);
        this.sale = new SimpleStringProperty(this, "sale", sale);
        this.profit = new SimpleStringProperty(this, "profit", profit);
        this.itemID = new SimpleStringProperty(this, "itemID", itemID);
        this.itemName = new SimpleStringProperty(this, "itemName", itemName);
        this.customerID = new SimpleStringProperty(this, "customerID", customerID);
        this.customerName = new SimpleStringProperty(this, "customerName", customerName);
        this.notes = new SimpleStringProperty(this, "notes", notes);
    }

    public StringProperty expiredIDProperty() { return expiredID; }
    public String getExpiredID() { return expiredID.get(); }
    public void setExpiredID(String value) { expiredID.set(value); }

    public StringProperty dateProperty() { return date; }
    public String getDate() { return date.get(); }
    public void setDate(String value) { date.set(value); }

    public StringProperty collateralProperty() { return collateral; }
    public String getCollateral() { return collateral.get(); }
    public void setCollateral(String value) { collateral.set(value); }

    public StringProperty saleProperty() { return sale; }
    public String getSale() { return sale.get(); }
    public void setSale(String value) { sale.set(value); }

    public StringProperty profitProperty() { return profit; }
    public String getProfit() { return profit.get(); }
    public void setProfit(String value) { profit.set(value); }

    public StringProperty itemIDProperty() { return itemID; }
    public String getItemID() { return itemID.get(); }
    public void setItemID(String value) { itemID.set(value); }

    public StringProperty itemNameProperty() { return itemName; }
    public String getItemName() { return itemName.get(); }
    public void setItemName(String value) { itemName.set(value); }

    public StringProperty customerIDProperty() { return customerID; }
    public String getCustomerID() { return customerID.get(); }
    public void setCustomerID(String value) { customerID.set(value); }

    public StringProperty customerNameProperty() { return customerName; }
    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String value) { customerName.set(value); }

    public StringProperty notesProperty() { return notes; }
    public String getNotes() { return notes.get(); }
    public void setNotes(String value) { notes.set(value); }

}
