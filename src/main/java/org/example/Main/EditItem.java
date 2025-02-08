package org.example.Main;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.Classes.Item;
import org.example.Classes.ItemCategory;
import org.example.Logic;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditItem {
    private static final Logger logger = Logger.getLogger(EditItem.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String itemCategoryQuery = "SELECT * FROM \"Pawn\".\"Item_Category\"";
    private static final String updateItemQuery = "UPDATE \"Pawn\".\"Item\" " +
            "SET \"itemName\"=?,\"categoryID\"=?,\"status\"=?,\"pawnDate\"=?,\"expireDate\"=?,\"price\"=?,\"tariff\"=?," +
            "\"admin\"=?,\"provision\"=?,\"storageFee\"=?,\"damageFee\"=?,\"total\"=?,\"additionalNotes\"=? " +
            "WHERE \"itemID\"=?";

    private final ObservableList<ItemCategory> itemCategoryList = FXCollections.observableArrayList();

    @FXML TextField editItem_itemNameField,editItem_priceField,editItem_tariffField,editItem_adminField,
            editItem_provisionField,editItem_storageField,editItem_damageField,editItem_totalField;
    @FXML TextArea editItem_notesField;
    @FXML ChoiceBox<ItemCategory> editItem_categoryChoice;
    @FXML ChoiceBox<String> editItem_statusChoice;
    @FXML DatePicker editItem_pawnDatePicker,editItem_expireDatePicker;
    @FXML Button editItem_confirmEditButton,editItem_cancelEditButton;

    Logic logic = new Logic();
    private EditItem.EditItemListener listener;
    public interface EditItemListener { void onEditCreated(boolean success); }
    public void setEditItemListener(EditItem.EditItemListener listener) { this.listener = listener; }

    private Item selectedItem;
    public void setItem(Item item) { this.selectedItem = item; }

    public void initializeEditItem() {
        populateFields();
        setupTextFields();
        setupFieldListeners();
        setupButtonHandlers();
    }

    private void populateFields() {
        // Status and Item Category
        setupChoiceBox();

        // Item Name
        editItem_itemNameField.setText(selectedItem.getItemName());

        // Pawn Date
        String pawnDateStr = selectedItem.getPawnDate();
        if (pawnDateStr != null && !pawnDateStr.isEmpty()) {
            LocalDate pawnDate = LocalDate.parse(pawnDateStr);
            editItem_pawnDatePicker.setValue(pawnDate);
        } else {
            editItem_pawnDatePicker.setValue(null);
        }

        // Expire Date
        String expireDateStr = selectedItem.getExpireDate();
        if (pawnDateStr != null && !pawnDateStr.isEmpty()) {
            LocalDate expireDate = LocalDate.parse(expireDateStr);
            editItem_expireDatePicker.setValue(expireDate);
        } else {
            editItem_expireDatePicker.setValue(null);
        }

        // Price, Tariff, Admin, Total
        editItem_priceField.setText(selectedItem.getPrice());
        editItem_tariffField.setText(selectedItem.getTariff());
        editItem_adminField.setText(selectedItem.getAdmin());
        editItem_provisionField.setText(selectedItem.getProvision());
        editItem_storageField.setText(selectedItem.getStorageFee());
        editItem_damageField.setText(selectedItem.getDamageFee());
        editItem_totalField.setText(selectedItem.getTotal());

        // Additional Notes
        editItem_notesField.setText(selectedItem.getAdditionalNotes());
    }

    private void setupTextFields() {
        editItem_pawnDatePicker.setDisable(true);
        editItem_totalField.setDisable(true);

        logic.setupNumericField(editItem_priceField);
        logic.setupPercentage(editItem_tariffField);
        logic.setupPercentage(editItem_provisionField);
        logic.setupPercentage(editItem_storageField);
        logic.setupPercentage(editItem_damageField);
        logic.setupPercentage(editItem_adminField);

        editItem_priceField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(editItem_priceField,editItem_tariffField,editItem_adminField,editItem_provisionField,
                        editItem_storageField,editItem_damageField,editItem_totalField));
        editItem_tariffField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(editItem_priceField,editItem_tariffField,editItem_adminField,editItem_provisionField,
                        editItem_storageField,editItem_damageField,editItem_totalField));
        editItem_provisionField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(editItem_priceField,editItem_tariffField,editItem_adminField,editItem_provisionField,
                        editItem_storageField,editItem_damageField,editItem_totalField));
        editItem_storageField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(editItem_priceField,editItem_tariffField,editItem_adminField,editItem_provisionField,
                        editItem_storageField,editItem_damageField,editItem_totalField));
        editItem_damageField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(editItem_priceField,editItem_tariffField,editItem_adminField,editItem_provisionField,
                        editItem_storageField,editItem_damageField,editItem_totalField));
        editItem_adminField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(editItem_priceField,editItem_tariffField,editItem_adminField,editItem_provisionField,
                        editItem_storageField,editItem_damageField,editItem_totalField));
    }

    private void setupChoiceBox() {
        // Status Choice
        editItem_statusChoice.getItems().addAll("Active","Redeemed","Expired");
        editItem_statusChoice.setValue(selectedItem.getStatus());

        // Item Category Choice
        Map<String, ItemCategory> categoryMap = new HashMap<>();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(itemCategoryQuery)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemCategory ic = new ItemCategory(
                        rs.getString("categoryID"),
                        rs.getString("categoryName")
                );
                itemCategoryList.add(ic);
                categoryMap.put(ic.getCategoryID(), ic); // Add to the map
            }

            // Populate the ChoiceBox
            editItem_categoryChoice.getItems().addAll(itemCategoryList);

            // Set a StringConverter to display categoryName
            editItem_categoryChoice.setConverter(new StringConverter<ItemCategory>() {
                @Override
                public String toString(ItemCategory itemCategory) {
                    return itemCategory != null ? itemCategory.getCategoryName() : "";
                }

                @Override
                public ItemCategory fromString(String string) {
                    return null;
                }
            });

            // Set the value of the ChoiceBox based on selectedItem's categoryID
            if (selectedItem != null) {
                String selectedCategoryID = selectedItem.getCategoryID();
                ItemCategory selectedCategory = categoryMap.get(selectedCategoryID); // Direct lookup
                editItem_categoryChoice.setValue(selectedCategory);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }
    }

    private void setupFieldListeners() {
        // Create a ChangeListener to check if fields are filled
        ChangeListener<Object> fieldListener = (ObservableValue<?> observable, Object oldValue, Object newValue) -> checkAllFieldsFilled();

        // Add the listener to the relevant fields
        editItem_itemNameField.textProperty().addListener(fieldListener);
        editItem_categoryChoice.valueProperty().addListener(fieldListener);
        editItem_statusChoice.valueProperty().addListener(fieldListener);
        editItem_pawnDatePicker.valueProperty().addListener(fieldListener);
        editItem_expireDatePicker.valueProperty().addListener(fieldListener);

        editItem_priceField.textProperty().addListener(fieldListener);
        editItem_tariffField.textProperty().addListener(fieldListener);
        editItem_adminField.textProperty().addListener(fieldListener);
        editItem_provisionField.textProperty().addListener(fieldListener);
        editItem_storageField.textProperty().addListener(fieldListener);
        editItem_damageField.textProperty().addListener(fieldListener);
    }

    private void checkAllFieldsFilled() {
        boolean allFieldsFilled =
                !editItem_itemNameField.getText().isEmpty() &&
                        editItem_categoryChoice.getValue() != null &&
                        editItem_statusChoice.getValue() != null &&
                        editItem_pawnDatePicker.getValue() != null &&
                        editItem_expireDatePicker.getValue() != null &&
                        isValidFormattedNumber(editItem_priceField.getText()) &&
                        isValidFormattedNumber(editItem_tariffField.getText()) &&
                        isValidFormattedNumber(editItem_adminField.getText()) &&
                        isValidFormattedNumber(editItem_provisionField.getText()) &&
                        isValidFormattedNumber(editItem_storageField.getText()) &&
                        isValidFormattedNumber(editItem_damageField.getText());
        editItem_confirmEditButton.setDisable(!allFieldsFilled);
    }

    private boolean isValidFormattedNumber(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            String cleanedText = text.replace(",", "");
            Float.parseFloat(cleanedText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setupButtonHandlers() {
        editItem_itemNameField.textProperty().addListener((observable, oldValue, newValue) -> updateConfirmButtonState());
        editItem_confirmEditButton.setOnAction(event -> handleConfirmEditButton());
        editItem_cancelEditButton.setOnAction(event -> closeWindow(editItem_cancelEditButton));
    }

    private void updateConfirmButtonState() {
        boolean isNameValid = !editItem_itemNameField.getText().trim().isEmpty();
        editItem_confirmEditButton.setDisable(!(isNameValid));
    }

    private void handleConfirmEditButton() {
        String itemName = editItem_itemNameField.getText();
        String categoryID = editItem_categoryChoice.getValue().getCategoryID();
        String status = editItem_statusChoice.getValue();
        String pawnDate = editItem_pawnDatePicker.getValue() != null ? editItem_pawnDatePicker.getValue().toString() : null;
        String expireDate = editItem_expireDatePicker.getValue() != null ? editItem_expireDatePicker.getValue().toString() : null;
        String price = editItem_priceField.getText();
        String tariff = editItem_tariffField.getText();
        String admin = editItem_adminField.getText();
        String provision = editItem_provisionField.getText();
        String storage = editItem_storageField.getText();
        String damage = editItem_damageField.getText();
        String total = editItem_totalField.getText();
        String additionalNotes = editItem_notesField.getText();
        String itemID = selectedItem.getItemID();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(updateItemQuery)) {

            ps.setString(1, itemName);
            ps.setString(2, categoryID);
            ps.setString(3, status);
            ps.setString(4, pawnDate);
            ps.setString(5, expireDate);
            ps.setString(6, price);
            ps.setString(7, tariff);
            ps.setString(8, admin);
            ps.setString(9, provision);
            ps.setString(10, storage);
            ps.setString(11, damage);
            ps.setString(12, total);
            ps.setString(13, additionalNotes);
            ps.setString(14, itemID);
            ps.executeUpdate();

        } catch (SQLException e) {
            listener.onEditCreated(false);
            e.printStackTrace();
        }
        listener.onEditCreated(true);
        closeWindow(editItem_confirmEditButton);
    }

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
