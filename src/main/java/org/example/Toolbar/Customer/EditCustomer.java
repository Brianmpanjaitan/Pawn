package org.example.Toolbar.Customer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.Classes.Customer;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditCustomer {
    private static final Logger logger = Logger.getLogger(EditCustomer.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String customerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private static final String customerCheckQuery = "SELECT COUNT(*) FROM \"Pawn\".\"Customer\" WHERE \"customerName\" = ?";
    private static final String updateCustomerQuery = "UPDATE \"Pawn\".\"Customer\" " +
            "SET \"customerName\"=?, \"phoneNumber\"=?, \"address\"=?, \"gender\"=? , \"dob\"=?, \"status\"=?, \"entryDate\"=?, \"exitDate\"=?, \"notes\"=? " +
            "WHERE \"customerID\"=?";

    @FXML TextField editCustomer_customerNameField,editCustomer_phoneNumberField,editCustomer_addressField;
    @FXML DatePicker editCustomer_dobPicker,editCustomer_entryPicker,editCustomer_exitPicker;
    @FXML Button editCustomer_confirmButton,editCustomer_deleteButton,editCustomer_cancelButton;
    @FXML ChoiceBox<String> editCustomer_genderChoiceBox,editCustomer_statusChoiceBox;
    @FXML ChoiceBox<Customer> editCustomer_customerChoiceBox;
    @FXML TextArea editCustomer_notesField;
    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();

    private EditCustomer.CustomerEditListener listener;
    public interface CustomerEditListener { void onCustomerEdited(boolean success); }
    public void setCustomerEditListener(EditCustomer.CustomerEditListener listener) { this.listener = listener; }

    private Customer currentCustomer;
    public void setCustomer(Customer customer) { currentCustomer = customer; }

    public void initializePage() {
        // Check if this is being accessed by the toolbar or customer info
        if (currentCustomer != null) {
            editCustomer_customerChoiceBox.setValue(currentCustomer);
            editCustomer_customerChoiceBox.setDisable(true);
            editCustomer_customerNameField.setText(currentCustomer.getCustomerName());
            editCustomer_phoneNumberField.setText(currentCustomer.getPhoneNumber());
            editCustomer_addressField.setText(currentCustomer.getAddress());
            editCustomer_genderChoiceBox.setValue(currentCustomer.getGender());
            editCustomer_statusChoiceBox.setValue(currentCustomer.getStatus());
            editCustomer_notesField.setText(currentCustomer.getNotes());

            String dobStr = currentCustomer.getDOB();
            String entryStr = currentCustomer.getEntryDate();
            String exitStr = currentCustomer.getExitDate();

            if (dobStr != null && !dobStr.isEmpty()) {
                LocalDate dobDate = LocalDate.parse(dobStr);
                editCustomer_dobPicker.setValue(dobDate);
            } else {
                editCustomer_dobPicker.setValue(null);
            }
            if (entryStr != null && !entryStr.isEmpty()) {
                LocalDate entryDate = LocalDate.parse(entryStr);
                editCustomer_entryPicker.setValue(entryDate);
            } else {
                editCustomer_entryPicker.setValue(null);
            }
            if (exitStr != null && !exitStr.isEmpty()) {
                LocalDate exitDate = LocalDate.parse(exitStr);
                editCustomer_exitPicker.setValue(exitDate);
            } else {
                editCustomer_exitPicker.setValue(null);
            }
            setupOptionChoiceBox();

        } else {
            editCustomer_confirmButton.setDisable(true);
            editCustomer_deleteButton.setDisable(true);
            setupCustomerChoiceBox();
        }
        formatChoiceBox();
        setupButtonAndListener();
    }

    private void setupOptionChoiceBox() {
        customerList.clear();
        editCustomer_customerChoiceBox.getItems().clear();

        editCustomer_genderChoiceBox.getItems().addAll("Male", "Female");
        editCustomer_statusChoiceBox.getItems().addAll("Active", "Non Active");
    }

    public void setupCustomerChoiceBox() {
        setupOptionChoiceBox();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(customerQuery)) {

            while (rs.next()) {
                Customer customer = new Customer(
                        rs.getString("customerID"),
                        rs.getString("customerName"),
                        rs.getString("phoneNumber"),
                        rs.getString("address"),
                        rs.getString("gender"),
                        rs.getString("dob"),
                        rs.getString("status"),
                        rs.getString("entryDate"),
                        rs.getString("exitDate"),
                        rs.getString("notes"));
                customerList.add(customer);
            }

            editCustomer_customerChoiceBox.setItems(FXCollections.observableArrayList(customerList));
            editCustomer_confirmButton.setDisable(customerList.isEmpty());
            editCustomer_deleteButton.setDisable(customerList.isEmpty());

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading customer data", e);
        }
    }

    private void formatChoiceBox() {
        editCustomer_customerChoiceBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer != null ? customer.getCustomerName() : "";
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        });
    }

    private void setupButtonAndListener() {
        String delete_message = "Are you sure you want to delete this Customer?\nThis will also delete all Items and Savings under this Customer.";

        // Buttons
        editCustomer_confirmButton.setOnAction(event -> handleEditCustomer());
        editCustomer_deleteButton.setOnAction(event -> {
            if (confirmAction("Delete Customer", delete_message)) {
                handleDeleteCustomer();
            }
        });
        editCustomer_cancelButton.setOnAction(event -> closeWindow(editCustomer_cancelButton));

        // Add a method to check if both fields are valid
        Runnable validateFields = () -> {
            boolean isNameValid = editCustomer_customerNameField.getText() != null && !editCustomer_customerNameField.getText().trim().isEmpty();
            boolean isChoiceBoxValid = editCustomer_customerChoiceBox.getValue() != null;
            editCustomer_confirmButton.setDisable(!(isNameValid && isChoiceBoxValid));
        };

        // Text Field Listener for customerNameField
        editCustomer_customerNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateFields.run();
        });

        // Choice Box Listener
        editCustomer_customerChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            editCustomer_deleteButton.setDisable(newValue == null);
            editCustomer_confirmButton.setDisable(newValue == null || editCustomer_customerNameField.getText().trim().isEmpty());

            if (newValue == null) {
                editCustomer_customerNameField.clear();
                editCustomer_phoneNumberField.clear();
                editCustomer_addressField.clear();
                editCustomer_genderChoiceBox.setValue(null);
                editCustomer_dobPicker.setValue(null);
                editCustomer_statusChoiceBox.setValue(null);
                editCustomer_entryPicker.setValue(null);
                editCustomer_exitPicker.setValue(null);
                editCustomer_notesField.clear();
            } else {
                // Populate the text fields with the selected customer's details
                editCustomer_customerNameField.setText(newValue.getCustomerName());
                editCustomer_phoneNumberField.setText(newValue.getPhoneNumber());
                editCustomer_addressField.setText(newValue.getAddress());
                editCustomer_genderChoiceBox.setValue(newValue.getGender());
                editCustomer_statusChoiceBox.setValue(newValue.getStatus());
                editCustomer_notesField.setText(newValue.getNotes());

                String dobStr = newValue.getDOB();
                String entryStr = newValue.getEntryDate();
                String exitStr = newValue.getExitDate();

                if (dobStr != null && !dobStr.isEmpty()) {
                    LocalDate dobDate = LocalDate.parse(dobStr);
                    editCustomer_dobPicker.setValue(dobDate);
                } else {
                    editCustomer_dobPicker.setValue(null);
                }
                if (entryStr != null && !entryStr.isEmpty()) {
                    LocalDate entryDate = LocalDate.parse(entryStr);
                    editCustomer_entryPicker.setValue(entryDate);
                } else {
                    editCustomer_entryPicker.setValue(null);
                }
                if (exitStr != null && !exitStr.isEmpty()) {
                    LocalDate exitDate = LocalDate.parse(exitStr);
                    editCustomer_exitPicker.setValue(exitDate);
                } else {
                    editCustomer_exitPicker.setValue(null);
                }
            }
        });
        validateFields.run();
    }

    private void handleEditCustomer() {
        if (editCustomer_customerChoiceBox.getValue() == null) {
            showAlert("Please select a customer to edit.");
            return; // Stop further execution
        }

        String customerID = editCustomer_customerChoiceBox.getValue().getCustomerID();
        String customerName = editCustomer_customerNameField.getText().trim();
        String phoneNumber = editCustomer_phoneNumberField.getText();
        String address = editCustomer_addressField.getText();
        String gender = editCustomer_genderChoiceBox.getValue();
        String dob = editCustomer_dobPicker.getValue().toString();
        String status = editCustomer_statusChoiceBox.getValue();
        String entry = editCustomer_entryPicker.getValue().toString();
        String exit = editCustomer_exitPicker.getValue().toString();
        String notes = editCustomer_notesField.getText();

        if (customerName.isEmpty()) {
            showAlert("Enter a name for the Customer.");
            if (listener != null) listener.onCustomerEdited(false);
        } else if (!isCustomerNameAvailable(customerName)) {
            showAlert("Customer with that name already exists.");
            if (listener != null) listener.onCustomerEdited(false);
        } else {
            if (!editCustomerRecord(customerID,customerName,phoneNumber,address,gender,dob,status,entry,exit,notes)) {
                showAlert("Error editing customer. Please try again.");
                if (listener != null) listener.onCustomerEdited(false);
            } else {
                if (listener != null) listener.onCustomerEdited(true);
                closeWindow(editCustomer_confirmButton);
            }
        }
    }

    private boolean isCustomerNameAvailable(String customerName) {
        if (currentCustomer != null && customerName.equals(currentCustomer.getCustomerName())) {
            // The name matches the current customer's name, so it's valid.
            return true;
        }

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(customerCheckQuery)) {

            statement.setString(1, customerName);
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) == 0;

        } catch (SQLException e) {
            logger.warning("Database error: " + e.getMessage());
            return false;
        }
    }


    private boolean editCustomerRecord(String customerID,String customerName,String phoneNumber,String address,String gender,
                                       String dob,String status,String entry,String exit,String notes) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(updateCustomerQuery)) {

            statement.setString(1, customerName);
            statement.setString(2, phoneNumber);
            statement.setString(3, address);
            statement.setString(4, gender);
            statement.setString(5, dob);
            statement.setString(6, status);
            statement.setString(7, entry);
            statement.setString(8, exit);
            statement.setString(9, notes);
            statement.setString(10, customerID);
            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage());
            return false;
        }
    }

    private void handleDeleteCustomer() {
        Customer selectedCustomer = editCustomer_customerChoiceBox.getValue();

        if (selectedCustomer == null) {
            showAlert("No customer selected.");
            return;
        }

        String stored_procedure = "CALL \"Pawn\".delete_customer(?)";
        boolean success = false;

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement preparedProcedure = connection.prepareStatement(stored_procedure)) {

            preparedProcedure.setString(1, selectedCustomer.getCustomerID());
            preparedProcedure.execute();
            success = true;

        } catch (SQLException e) {
            showAlert("An error occurred while deleting the customer.");
            logger.log(Level.SEVERE, "Error deleting customer: " + e.getMessage(), e);
        }

        // Notify the listener if the deletion is successful
        if (success) {
            if (listener != null) listener.onCustomerEdited(true);
            closeWindow(editCustomer_deleteButton);
        } else {
            if (listener != null) listener.onCustomerEdited(false);
        }
    }


    private boolean confirmAction(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "This action cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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
