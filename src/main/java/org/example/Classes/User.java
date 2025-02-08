package org.example.Classes;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class User {
    private final StringProperty userID;
    private final StringProperty username;
    private final StringProperty password;

    public User(String id, String name, String password) {
        this.userID = new SimpleStringProperty(this, "userID", id);
        this.username = new SimpleStringProperty(this, "username", name);
        this.password = new SimpleStringProperty(this, "password", password);
    }

    public StringProperty userIDProperty() { return userID; }
    public String getUserID() { return userID.get(); }
    public void setUserID(String value) { userID.set(value); }

    public StringProperty usernameProperty() { return username; }
    public String getUsername() { return username.get(); }
    public void setUsername(String value) { username.set(value); }

    public StringProperty passwordProperty() { return password; }
    public String getPassword() { return password.get(); }
    public void setPassword(String value) { password.set(value); }
}
