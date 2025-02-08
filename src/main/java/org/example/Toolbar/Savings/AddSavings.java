package org.example.Toolbar.Savings;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.Classes.Customer;
import org.example.Logic;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Logger;

public class AddSavings {
    private static final Logger logger = Logger.getLogger(AddSavings.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String getSavingsID = "SELECT \"savingsID\" FROM \"Pawn\".\"Savings\" WHERE \"savingsID\" LIKE ?";
    private static final String insertSavingsQuery = "INSERT INTO \"Pawn\".\"Savings\"(" +
            "\"savingsID\",\"customerID\",\"customerName\",\"date\",\"principal\",\"mandatory\",\"capital\",\"voluntary\"," +
            "\"others\",\"total\",\"notes\",\"status\") " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @FXML VBox addSavings_VBoxContent;
    @FXML Label addSavings_titleLabel;
    @FXML TextField addSavings_customerNameField,addSavings_totalField,
            addSavings_principalField,addSavings_mandatoryField,addSavings_capitalField,addSavings_voluntaryField,addSavings_otherField;
    @FXML TextArea addSavings_notesField;
    @FXML DatePicker addSavings_datePicker;
    @FXML ChoiceBox<String> addSavings_statusChoice;
    @FXML Button addSavings_confirmAddButton,addSavings_cancelAddButton;

    private AddSavings.AddSavingsListener listener;
    public interface AddSavingsListener { void onAddCreated(boolean success); }
    public void setAddSavingsListener(AddSavings.AddSavingsListener listener) { this.listener = listener; }

    private Customer selectedCustomer;
    Logic logic = new Logic();

    public void setCustomer(Customer customer) {
        this.selectedCustomer = customer;
    }

    public void initializeAddSavings() {
        addSavings_confirmAddButton.setDisable(true);
        addSavings_customerNameField.setDisable(true);

        setupTextFields();
        populateFields();
        setupFieldListeners();
        setupButtonHandlers();
    }

    private void setupTextFields() {
        addSavings_totalField.setDisable(true);
        logic.setupNumericField(addSavings_principalField);
        logic.setupNumericField(addSavings_mandatoryField);
        logic.setupNumericField(addSavings_capitalField);
        logic.setupNumericField(addSavings_voluntaryField);
        logic.setupNumericField(addSavings_otherField);

        addSavings_principalField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(addSavings_principalField,addSavings_mandatoryField,addSavings_capitalField,addSavings_voluntaryField,addSavings_otherField,addSavings_totalField));
        addSavings_mandatoryField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(addSavings_principalField,addSavings_mandatoryField,addSavings_capitalField,addSavings_voluntaryField,addSavings_otherField,addSavings_totalField));
        addSavings_capitalField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(addSavings_principalField,addSavings_mandatoryField,addSavings_capitalField,addSavings_voluntaryField,addSavings_otherField,addSavings_totalField));
        addSavings_voluntaryField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(addSavings_principalField,addSavings_mandatoryField,addSavings_capitalField,addSavings_voluntaryField,addSavings_otherField,addSavings_totalField));
        addSavings_otherField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(addSavings_principalField,addSavings_mandatoryField,addSavings_capitalField,addSavings_voluntaryField,addSavings_otherField,addSavings_totalField));

    }

    private void populateFields() {
        addSavings_customerNameField.setText(selectedCustomer.getCustomerName());

        addSavings_statusChoice.getItems().addAll("Active","Redeemed");
        addSavings_statusChoice.setValue("Active");
        addSavings_datePicker.setValue(java.time.LocalDate.now());
    }

    private void setupFieldListeners() {
        // Create a ChangeListener to check if fields are filled
        ChangeListener<Object> fieldListener = (ObservableValue<?> observable, Object oldValue, Object newValue) -> checkAllFieldsFilled();

        // Add the listener to the relevant fields
        addSavings_customerNameField.textProperty().addListener(fieldListener);
        addSavings_datePicker.valueProperty().addListener(fieldListener);
        addSavings_statusChoice.valueProperty().addListener(fieldListener);

        addSavings_principalField.textProperty().addListener(fieldListener);
        addSavings_mandatoryField.textProperty().addListener(fieldListener);
        addSavings_capitalField.textProperty().addListener(fieldListener);
        addSavings_voluntaryField.textProperty().addListener(fieldListener);
        addSavings_otherField.textProperty().addListener(fieldListener);
    }

    private void checkAllFieldsFilled() {
        boolean allFieldsFilled =
                !addSavings_customerNameField.getText().isEmpty() &&
                        addSavings_datePicker.getValue() != null &&
                        addSavings_statusChoice.getValue() != null &&

                        isValidFormattedNumber(addSavings_principalField.getText()) &&
                        isValidFormattedNumber(addSavings_mandatoryField.getText()) &&
                        isValidFormattedNumber(addSavings_capitalField.getText()) &&
                        isValidFormattedNumber(addSavings_voluntaryField.getText()) &&
                        isValidFormattedNumber(addSavings_otherField.getText());
        addSavings_confirmAddButton.setDisable(!allFieldsFilled);
    }

    private boolean isValidFormattedNumber(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            // Remove commas (thousands separators) and attempt to parse the number
            String cleanedText = text.replace(",", "");
            Float.parseFloat(cleanedText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setupButtonHandlers() {
        addSavings_confirmAddButton.setOnAction(event -> handleConfirmAddButton());
        addSavings_cancelAddButton.setOnAction(event -> closeWindow(addSavings_cancelAddButton));
    }

    private void handleConfirmAddButton() {
        String itemID = getNextItemID(addSavings_datePicker.getValue());
        addSavings_titleLabel.setText(addSavings_titleLabel.getText() + ": " + itemID);
        logic.printReceipt(addSavings_VBoxContent, itemID, "Customer Savings");

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement preparedStatement = connection.prepareStatement(insertSavingsQuery)) {

            preparedStatement.setString(1, itemID);
            preparedStatement.setString(2, selectedCustomer.getCustomerID());
            preparedStatement.setString(3, selectedCustomer.getCustomerName());
            preparedStatement.setString(4, addSavings_datePicker.getValue().toString());
            preparedStatement.setString(5, addSavings_principalField.getText());
            preparedStatement.setString(6, addSavings_mandatoryField.getText());
            preparedStatement.setString(7, addSavings_capitalField.getText());
            preparedStatement.setString(8, addSavings_voluntaryField.getText());
            preparedStatement.setString(9, addSavings_otherField.getText());
            preparedStatement.setString(10, addSavings_totalField.getText());
            preparedStatement.setString(11, addSavings_notesField.getText());
            preparedStatement.setString(12, addSavings_statusChoice.getValue());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        listener.onAddCreated(true);
        closeWindow(addSavings_confirmAddButton);
    }

    private String getNextItemID(LocalDate pawnDate) {
        String nextSavingsID = null;
        char monthLetter = (char) ('A' + pawnDate.getMonthValue() - 1);
        String yearSuffix = String.valueOf(pawnDate.getYear()).substring(2);

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(getSavingsID)) {

            ps.setString(1, monthLetter + yearSuffix + "-S%");

            ResultSet rs = ps.executeQuery();
            int maxNumber = 0;

            // Iterate through the results and find the max number
            while (rs.next()) {
                String savingsID = rs.getString("savingsID");
                if (savingsID != null && savingsID.contains("-S")) {
                    String[] parts = savingsID.split("-S");
                    if (parts.length == 2) {
                        try {
                            int number = Integer.parseInt(parts[1]);
                            maxNumber = Math.max(maxNumber, number);
                        } catch (NumberFormatException e) {
                            // Ignore invalid numbers
                            e.printStackTrace();
                        }
                    }
                }
            }
            nextSavingsID = monthLetter + yearSuffix + "-S" + (maxNumber + 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nextSavingsID;
    }

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
