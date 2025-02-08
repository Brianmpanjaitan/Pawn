package org.example.Classes;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Item {
    private final StringProperty itemID;
    private final StringProperty itemName;
    private final StringProperty customerID;
    private final StringProperty categoryID;
    private final StringProperty categoryName;

    private final StringProperty status;
    private final StringProperty pawnDate;
    private final StringProperty expireDate;
    private final StringProperty price;
    private final StringProperty tariff;
    private final StringProperty admin;
    private final StringProperty provision;
    private final StringProperty storageFee;
    private final StringProperty damageFee;
    private final StringProperty total;
    private final StringProperty additionalNotes;

    public Item(String itemID, String itemName, String customerID, String categoryID, String categoryName,
                String status, String pawnDate, String expireDate, String price, String tariff, String admin,
                String provision, String storageFee, String damageFee, String total, String additionalNotes) {
        this.itemID = new SimpleStringProperty(this, "itemID", itemID);
        this.itemName = new SimpleStringProperty(this, "itemName", itemName);
        this.customerID = new SimpleStringProperty(this, "customerID", customerID);
        this.categoryID = new SimpleStringProperty(this, "categoryID", categoryID);
        this.categoryName = new SimpleStringProperty(this, "categoryName", categoryName);
        this.status = new SimpleStringProperty(this, "status", status);
        this.pawnDate = new SimpleStringProperty(this, "pawnDate", pawnDate);
        this.expireDate = new SimpleStringProperty(this, "expireDate", expireDate);
        this.price = new SimpleStringProperty(this, "price", price);
        this.tariff = new SimpleStringProperty(this, "tariff", tariff);
        this.admin = new SimpleStringProperty(this, "admin", admin);
        this.provision = new SimpleStringProperty(this, "provision", provision);
        this.storageFee = new SimpleStringProperty(this, "storageFee", storageFee);
        this.damageFee = new SimpleStringProperty(this, "damageFee", damageFee);
        this.total = new SimpleStringProperty(this, "total", total);
        this.additionalNotes = new SimpleStringProperty(this, "additionalNotes", additionalNotes);
    }

    public StringProperty itemIDProperty() { return itemID; }
    public String getItemID() { return itemID.get(); }
    public void setItemID(String value) { itemID.set(value); }

    public StringProperty itemNameProperty() { return itemName; }
    public String getItemName() { return itemName.get(); }
    public void setItemName(String value) { itemName.set(value); }

    public StringProperty customerIDProperty() { return customerID; }
    public String getCustomerID() { return customerID.get(); }
    public void setCustomerID(String value) { customerID.set(value); }

    public StringProperty categoryIDProperty() { return categoryID; }
    public String getCategoryID() { return categoryID.get(); }
    public void setCategoryID(String value) { categoryID.set(value); }

    public StringProperty categoryNameProperty() { return categoryName; }
    public String getCategoryName() { return categoryName.get(); }
    public void setCategoryName(String value) { categoryName.set(value); }

    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }

    public StringProperty pawnDateProperty() { return pawnDate; }
    public String getPawnDate() { return pawnDate.get(); }
    public void setPawnDate(String value) { pawnDate.set(value); }

    public StringProperty expireDateProperty() { return expireDate; }
    public String getExpireDate() { return expireDate.get(); }
    public void setExpireDate(String value) { expireDate.set(value); }

    public StringProperty priceProperty() { return price; }
    public String getPrice() { return price.get(); }
    public void setPrice(String value) { price.set(value); }

    public StringProperty tariffProperty() { return tariff; }
    public String getTariff() { return tariff.get(); }
    public void setTariff(String value) { tariff.set(value); }

    public StringProperty adminProperty() { return admin; }
    public String getAdmin() { return admin.get(); }
    public void setAdmin(String value) { admin.set(value); }

    public StringProperty provisionProperty() { return provision; }
    public String getProvision() { return provision.get(); }
    public void setProvision(String value) { provision.set(value); }

    public StringProperty storageFeeProperty() { return storageFee; }
    public String getStorageFee() { return storageFee.get(); }
    public void setStorageFee(String value) { storageFee.set(value); }

    public StringProperty damageFeeProperty() { return damageFee; }
    public String getDamageFee() { return damageFee.get(); }
    public void setDamageFee(String value) { damageFee.set(value); }

    public StringProperty totalProperty() { return total; }
    public String getTotal() { return total.get(); }
    public void setTotal(String value) { total.set(value); }

    public StringProperty additionalNotesProperty() { return additionalNotes; }
    public String getAdditionalNotes() { return additionalNotes.get(); }
    public void setAdditionalNotes(String value) { additionalNotes.set(value); }
}
