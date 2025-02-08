package org.example.Main;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.Classes.Customer;
import org.example.Classes.Item;
import org.example.Classes.User;
import org.example.Logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemInformation {
    private static final Logger logger = Logger.getLogger(ItemInformation.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String updateItemStatusQuery = "UPDATE \"Pawn\".\"Item\" " +
            "SET \"status\"=? WHERE \"itemID\"=?";

    @FXML VBox contentVBox;
    @FXML Label itemInformation_itemLabel,itemInformation_itemIDLabel,itemInformation_statusLabel,itemInformation_pawnDateLabel,
            itemInformation_expireDateLabel,itemInformation_priceLabel,itemInformation_tariffLabel,itemInformation_adminLabel,
            itemInformation_provisionLabel,itemInformation_storageLabel,itemInformation_damageLabel,itemInformation_totalLabel;
    @FXML TextArea itemInformation_notesField;
    @FXML Button itemInformation_printButton,itemInformation_redeemButton,itemInformation_cancelButton;

    Logic logic = new Logic();
    private User currentUser;
    private Customer currentCustomer;
    private Item selectedItem;

    public void setUser(User user) {
        this.currentUser = user;
    }
    public void setCustomer(Customer customer) { this.currentCustomer = customer; }
    public void setItem(Item item) {
        this.selectedItem = item;
        itemInformation_itemLabel.setText(selectedItem.getItemName());
        itemInformation_itemIDLabel.setText("Item ID: " + selectedItem.getItemID());
    }

    public void initializePage() {
        setupTextArea();
        setupLabels();
        setupButtonActions();
    }

    private void setupTextArea() {
        itemInformation_notesField.setEditable(false);
        itemInformation_notesField.setText(selectedItem.getAdditionalNotes());
    }

    private void setupLabels() {
        // Set the labels directly
        itemInformation_statusLabel.setText(itemInformation_statusLabel.getText() + selectedItem.getStatus());
        itemInformation_pawnDateLabel.setText(itemInformation_pawnDateLabel.getText() + selectedItem.getPawnDate());
        itemInformation_expireDateLabel.setText(itemInformation_expireDateLabel.getText() + selectedItem.getExpireDate());

        itemInformation_priceLabel.setText(itemInformation_priceLabel.getText() + selectedItem.getPrice());
        itemInformation_tariffLabel.setText(itemInformation_tariffLabel.getText() + selectedItem.getTariff() + "%");
        itemInformation_adminLabel.setText(itemInformation_adminLabel.getText() + selectedItem.getAdmin() + "%");
        itemInformation_provisionLabel.setText(itemInformation_provisionLabel.getText() + selectedItem.getProvision() + "%");
        itemInformation_storageLabel.setText(itemInformation_storageLabel.getText() + selectedItem.getStorageFee() + "%");
        itemInformation_damageLabel.setText(itemInformation_damageLabel.getText() + selectedItem.getDamageFee() + "%");

        itemInformation_totalLabel.setText(itemInformation_totalLabel.getText() + selectedItem.getTotal());

    }

    private void setupButtonActions() {
        Set<String> disabledStatuses = Set.of("Redeemed", "Expired");
        if (disabledStatuses.contains(selectedItem.getStatus())) {
            itemInformation_redeemButton.setDisable(true);
        }

        itemInformation_printButton.setOnAction(event -> logic.printItemInformation(contentVBox, selectedItem.getItemID()));
        itemInformation_redeemButton.setOnAction(event -> updateStatus("Redeemed",itemInformation_redeemButton));
        itemInformation_cancelButton.setOnAction(event -> closePopup(itemInformation_cancelButton));
    }

    private void updateStatus(String status, Button button) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(updateItemStatusQuery)) {

            ps.setString(1, status);
            ps.setString(2, selectedItem.getItemID());
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
