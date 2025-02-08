package org.example.Classes;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ItemCategory {
    private final StringProperty categoryID;
    private final StringProperty categoryName;

    public ItemCategory(String categoryID, String categoryName) {
        this.categoryID = new SimpleStringProperty(this, "categoryID", categoryID);
        this.categoryName = new SimpleStringProperty(this, "categoryName", categoryName);
    }

    public StringProperty categoryIDProperty() { return categoryID; }
    public String getCategoryID() { return categoryID.get(); }
    public void setCategoryID(String value) { categoryID.set(value); }

    public StringProperty categoryNameProperty() { return categoryName; }
    public String getCategoryName() { return categoryName.get(); }
    public void setCategoryName(String value) { categoryName.set(value); }
}
