package org.example.Main;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.logging.Logger;

public class CreateCustomer {
    private static final Logger logger = Logger.getLogger(CreateCustomer.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String getCustomerIDQuery = "SELECT \"customerID\" FROM \"Pawn\".\"Customer\"";
    private static final String insertCustomerQuery = "INSERT INTO \"Pawn\".\"Customer\" " +
            "(\"customerID\",\"customerName\",\"phoneNumber\",\"address\",\"gender\",\"dob\",\"status\",\"entryDate\",\"exitDate\",\"notes\") " +
            "VALUES (?,?,?,?,?,?,?,?,?,?)";

    @FXML Button createCustomer_createNewCustomer, createCustomer_cancelNewCustomer;
    @FXML TextField createCustomer_newCustomerName,createCustomer_phoneNumberField,createCustomer_addressField;
    @FXML TextArea createCustomer_notesField;
    @FXML DatePicker createCustomer_dobPicker,createCustomer_entryPicker,createCustomer_exitPicker;
    @FXML ChoiceBox<String> createCustomer_genderChoice,createCustomer_statusChoice;

    private CustomerCreationListener listener;
    public interface CustomerCreationListener { void onCustomerCreated(boolean success); }
    public void setCustomerCreationListener(CustomerCreationListener listener) { this.listener = listener; }

    @FXML public void initialize() {
        createCustomer_createNewCustomer.setDisable(true);
        setupChoiceBox();
        setupListenersAndButtons();
    }

    private void setupChoiceBox() {
        createCustomer_genderChoice.getItems().addAll("Male", "Female");
        createCustomer_statusChoice.getItems().addAll("Active", "Non Active");
    }

    private void setupListenersAndButtons() {
        createCustomer_newCustomerName.textProperty().addListener((observable, oldValue, newValue) -> updateCreateButtonState());
        createCustomer_statusChoice.valueProperty().addListener((observable, oldValue, newValue) -> updateCreateButtonState());
        createCustomer_createNewCustomer.setOnAction(event -> handleCreateCustomer());
        createCustomer_cancelNewCustomer.setOnAction(event -> closeWindow(createCustomer_cancelNewCustomer));
    }

    private void updateCreateButtonState() {
        boolean isNameValid = !createCustomer_newCustomerName.getText().trim().isEmpty();
        boolean isStatusValid = createCustomer_statusChoice.getValue() != null;

        createCustomer_createNewCustomer.setDisable(!(isNameValid && isStatusValid));
    }

    private void handleCreateCustomer() {
        String customerName = createCustomer_newCustomerName.getText();
        String phoneNumber = createCustomer_phoneNumberField.getText();
        String address = createCustomer_addressField.getText();
        String gender = createCustomer_genderChoice.getValue();
        String dob = createCustomer_dobPicker.getValue() != null ? createCustomer_dobPicker.getValue().toString() : null;
        String status = createCustomer_statusChoice.getValue();
        String entry = createCustomer_entryPicker.getValue() != null ? createCustomer_entryPicker.getValue().toString() : null;
        String exit = createCustomer_exitPicker.getValue() != null ? createCustomer_exitPicker.getValue().toString() : null;
        String notes = createCustomer_notesField.getText();

        // If any of the fields are empty (or blank), set them to null
        if (customerName.isEmpty()) customerName = null;
        if (phoneNumber.isEmpty()) phoneNumber = null;
        if (address.isEmpty()) address = null;
        if (gender == null || gender.isEmpty()) gender = null;
        if (notes.isEmpty()) notes = null;

        if (customerName.isEmpty()) {
            showAlert("Enter a name for the Customer.");
            if (listener != null) {
                listener.onCustomerCreated(false);
            }
        } else {
            int newCustomerID = getNextCustomerID();

            if (newCustomerID == -1 || !createCustomer(newCustomerID,customerName,phoneNumber,address,gender,dob,status,entry,exit,notes)) {
                showAlert("Error creating new customer. Please try again.");
                if (listener != null) listener.onCustomerCreated(false);
            } else {
                if (listener != null) listener.onCustomerCreated(true);
                closeWindow(createCustomer_createNewCustomer);
            }
        }
    }

    // Retrieve Next Client ID
    private int getNextCustomerID() {
        int maxNumber = 0;

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(getCustomerIDQuery)) {
            while (rs.next()) {
                String id = rs.getString("customerID");

                if (!id.isEmpty()) {
                    maxNumber = Math.max(maxNumber, Integer.parseInt(id));
                }
            }
            return maxNumber + 1;

        } catch (SQLException e) {
            logger.warning("Database error: " + e.getMessage());
        }
        return -1;
    }

    private boolean createCustomer(int customerID,String customerName,String phoneNumber,String address,String gender,
                                   String dob,String status,String entry,String exit,String notes) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(insertCustomerQuery)) {

            statement.setInt(1, customerID);
            statement.setString(2, customerName);
            statement.setString(3, phoneNumber);
            statement.setString(4, address);
            statement.setString(5, gender);
            statement.setString(6, dob);
            statement.setString(7, status);
            statement.setString(8, entry);
            statement.setString(9, exit);
            statement.setString(10, notes);
            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage());
            return false;
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
