package org.example.Toolbar.Savings;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.Classes.Customer;
import org.example.Classes.Savings;
import org.example.Classes.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SavingsInformation {
    private static final Logger logger = Logger.getLogger(SavingsInformation.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String updateSavingsStatusQuery = "UPDATE \"Pawn\".\"Savings\" " +
            "SET \"status\"=? WHERE \"savingsID\"=?";

    @FXML Label savingsInformation_savingsLabel,savingsInformation_nameLabel,savingsInformation_statusLabel,
            savingsInformation_dateLabel,savingsInformation_principalLabel,savingsInformation_mandatoryLabel,
            savingsInformation_capitalLabel,savingsInformation_voluntaryLabel,savingsInformation_othersLabel,savingsInformation_totalLabel;
    @FXML TextArea savingsInformation_notesField;
    @FXML Button savingsInformation_redeemButton,savingsInformation_cancelButton;

    private User currentUser;
    private Customer currentCustomer;
    private Savings selectedSavings;

    public void setUser(User user) {
        this.currentUser = user;
    }
    public void setCustomer(Customer customer) { this.currentCustomer = customer; }
    public void setSavings(Savings savings) {
        this.selectedSavings = savings;
        savingsInformation_savingsLabel.setText("Savings ID: " + selectedSavings.getSavingsID());
        savingsInformation_nameLabel.setText(selectedSavings.getCustomerName());
    }

    public void initializePage() {
        setupTextArea();
        setupLabels();
        setupButtonActions();
    }

    private void setupTextArea() {
        savingsInformation_notesField.setEditable(false);
        savingsInformation_notesField.setText(selectedSavings.getNotes());
    }

    private void setupLabels() {
        // Set the labels directly
        savingsInformation_statusLabel.setText(savingsInformation_statusLabel.getText() + selectedSavings.getStatus());
        savingsInformation_dateLabel.setText(savingsInformation_dateLabel.getText() + selectedSavings.getDate());
        savingsInformation_principalLabel.setText(savingsInformation_principalLabel.getText() + selectedSavings.getPrincipal());
        savingsInformation_mandatoryLabel.setText(savingsInformation_mandatoryLabel.getText() + selectedSavings.getMandatory());
        savingsInformation_capitalLabel.setText(savingsInformation_capitalLabel.getText() + selectedSavings.getCapital());
        savingsInformation_voluntaryLabel.setText(savingsInformation_voluntaryLabel.getText() + selectedSavings.getVoluntary());
        savingsInformation_othersLabel.setText(savingsInformation_othersLabel.getText() + selectedSavings.getOthers());
        savingsInformation_totalLabel.setText(savingsInformation_totalLabel.getText() + selectedSavings.getTotal());
    }

    private void setupButtonActions() {
        Set<String> disabledStatuses = Set.of("Redeemed");
        if (disabledStatuses.contains(selectedSavings.getStatus())) {
            savingsInformation_redeemButton.setDisable(true);
        }

        savingsInformation_redeemButton.setOnAction(event -> updateStatus("Redeemed",savingsInformation_redeemButton));
        savingsInformation_cancelButton.setOnAction(event -> closePopup(savingsInformation_cancelButton));
    }

    private void updateStatus(String status, Button button) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(updateSavingsStatusQuery)) {

            ps.setString(1, status);
            ps.setString(2, selectedSavings.getSavingsID());
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating status", e);
        }
        closePopup(button);
    }

    private void closePopup(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
