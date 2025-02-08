package org.example.Toolbar.Savings;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.Classes.Customer;
import org.example.Classes.Savings;
import org.example.Logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Logger;

public class EditSavings {
    private static final Logger logger = Logger.getLogger(EditSavings.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";
    private static final String updateSavingsQuery = "UPDATE \"Pawn\".\"Savings\"" +
            "SET \"date\"=?,\"principal\"=?,\"mandatory\"=?,\"capital\"=?,\"voluntary\"=?,\"others\"=?,\"total\"=?,\"notes\"=?,\"status\"=? " +
            "WHERE \"savingsID\"=?";

    @FXML TextField editSavings_customerNameField,editSavings_totalField,
            editSavings_principalField,editSavings_mandatoryField,editSavings_capitalField,editSavings_voluntaryField,editSavings_otherField;
    @FXML TextArea editSavings_notesField;
    @FXML DatePicker editSavings_datePicker;
    @FXML ChoiceBox<String> editSavings_statusChoice;
    @FXML Button editSavings_confirmAddButton,editSavings_cancelAddButton;

    private EditSavings.EditSavingsListener listener;
    public interface EditSavingsListener { void onEditCreated(boolean success); }
    public void setEditSavingsListener(EditSavings.EditSavingsListener listener) { this.listener = listener; }

    private Customer selectedCustomer;
    private Savings selectedSavings;

    Logic logic = new Logic();

    public void setCustomer(Customer customer) {
        this.selectedCustomer = customer;
    }
    public void setSavings(Savings savings) {
        this.selectedSavings = savings;
    }

    public void initializeEditSavings() {
        editSavings_customerNameField.setDisable(true);

        populateFields();
        setupTextFields();
        setupFieldListeners();
        setupButtonHandlers();
    }

    private void populateFields() {
        // Status and Item Category
        setupChoiceBox();

        editSavings_customerNameField.setText(selectedCustomer.getCustomerName());
        String dateStr = selectedSavings.getDate();
        if (dateStr != null && !dateStr.isEmpty()) {
            LocalDate date = LocalDate.parse(dateStr);
            editSavings_datePicker.setValue(date);
        } else {
            editSavings_datePicker.setValue(null);
        }
        editSavings_principalField.setText(selectedSavings.getPrincipal());
        editSavings_mandatoryField.setText(selectedSavings.getMandatory());
        editSavings_capitalField.setText(selectedSavings.getCapital());
        editSavings_voluntaryField.setText(selectedSavings.getVoluntary());
        editSavings_otherField.setText(selectedSavings.getOthers());
        editSavings_totalField.setText(selectedSavings.getTotal());
        editSavings_notesField.setText(selectedSavings.getNotes());

        editSavings_statusChoice.setValue(selectedSavings.getStatus());
    }

    private void setupChoiceBox() {
        // Status Choice
        editSavings_statusChoice.getItems().addAll("Active", "Redeemed");
        editSavings_statusChoice.setValue(selectedSavings.getStatus());
    }

    private void setupTextFields() {
        editSavings_totalField.setDisable(true);
        logic.setupNumericField(editSavings_principalField);
        logic.setupNumericField(editSavings_mandatoryField);
        logic.setupNumericField(editSavings_capitalField);
        logic.setupNumericField(editSavings_voluntaryField);
        logic.setupNumericField(editSavings_otherField);

        editSavings_principalField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(editSavings_principalField,editSavings_mandatoryField,editSavings_capitalField,editSavings_voluntaryField,editSavings_otherField,editSavings_totalField));
        editSavings_mandatoryField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(editSavings_principalField,editSavings_mandatoryField,editSavings_capitalField,editSavings_voluntaryField,editSavings_otherField,editSavings_totalField));
        editSavings_capitalField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(editSavings_principalField,editSavings_mandatoryField,editSavings_capitalField,editSavings_voluntaryField,editSavings_otherField,editSavings_totalField));
        editSavings_voluntaryField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(editSavings_principalField,editSavings_mandatoryField,editSavings_capitalField,editSavings_voluntaryField,editSavings_otherField,editSavings_totalField));
        editSavings_otherField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateSavings(editSavings_principalField,editSavings_mandatoryField,editSavings_capitalField,editSavings_voluntaryField,editSavings_otherField,editSavings_totalField));
    }

    private void setupFieldListeners() {
        // Create a ChangeListener to check if fields are filled
        ChangeListener<Object> fieldListener = (ObservableValue<?> observable, Object oldValue, Object newValue) -> checkAllFieldsFilled();

        // Add the listener to the relevant fields
        editSavings_customerNameField.textProperty().addListener(fieldListener);
        editSavings_datePicker.valueProperty().addListener(fieldListener);
        editSavings_statusChoice.valueProperty().addListener(fieldListener);

        editSavings_principalField.textProperty().addListener(fieldListener);
        editSavings_mandatoryField.textProperty().addListener(fieldListener);
        editSavings_capitalField.textProperty().addListener(fieldListener);
        editSavings_voluntaryField.textProperty().addListener(fieldListener);
        editSavings_otherField.textProperty().addListener(fieldListener);
    }

    private void checkAllFieldsFilled() {
        boolean allFieldsFilled =
                !editSavings_customerNameField.getText().isEmpty() &&
                        editSavings_datePicker.getValue() != null &&
                        editSavings_statusChoice.getValue() != null &&

                        isValidFormattedNumber(editSavings_principalField.getText()) &&
                        isValidFormattedNumber(editSavings_mandatoryField.getText()) &&
                        isValidFormattedNumber(editSavings_capitalField.getText()) &&
                        isValidFormattedNumber(editSavings_voluntaryField.getText()) &&
                        isValidFormattedNumber(editSavings_otherField.getText());
        editSavings_confirmAddButton.setDisable(!allFieldsFilled);
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
        editSavings_confirmAddButton.setOnAction(event -> handleConfirmEditButton());
        editSavings_cancelAddButton.setOnAction(event -> closeWindow(editSavings_cancelAddButton));
    }

    private void handleConfirmEditButton() {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(updateSavingsQuery)) {

            String date = editSavings_datePicker.getValue() != null ? editSavings_datePicker.getValue().toString() : null;

            ps.setString(1, date);
            ps.setString(2, editSavings_principalField.getText());
            ps.setString(3, editSavings_mandatoryField.getText());
            ps.setString(4, editSavings_capitalField.getText());
            ps.setString(5, editSavings_voluntaryField.getText());
            ps.setString(6, editSavings_otherField.getText());
            ps.setString(7, editSavings_totalField.getText());
            ps.setString(8, editSavings_notesField.getText());
            ps.setString(9, editSavings_statusChoice.getValue());
            ps.setString(10, selectedSavings.getSavingsID());
            ps.executeUpdate();

        } catch (SQLException e) {
            listener.onEditCreated(false);
            e.printStackTrace();
        }
        listener.onEditCreated(true);
        closeWindow(editSavings_confirmAddButton);
    }

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
