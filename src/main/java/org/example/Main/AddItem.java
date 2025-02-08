package org.example.Main;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.Classes.Customer;
import org.example.Classes.ItemCategory;
import org.example.Logic;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddItem {
    private static final Logger logger = Logger.getLogger(AddItem.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String getItemID = "SELECT \"itemID\" FROM \"Pawn\".\"Item\" WHERE \"itemID\" LIKE ?";
    private static final String itemCategoryQuery = "SELECT * FROM \"Pawn\".\"Item_Category\"";
    private static final String insertItemQuery = "INSERT INTO \"Pawn\".\"Item\"(" +
            "\"itemID\",\"itemName\",\"customerID\",\"categoryID\",\"status\",\"pawnDate\",\"expireDate\",\"price\"," +
            "\"tariff\",\"admin\",\"provision\",\"storageFee\",\"damageFee\",\"total\",\"additionalNotes\") " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private final ObservableList<ItemCategory> itemCategoryList = FXCollections.observableArrayList();

    @FXML Label addItem_titleLabel,addItem_customerID,addItem_customerName,addItem_customerAddress;
    @FXML VBox addItem_VBoxContent;
    @FXML TextField addItem_itemNameField,addItem_priceField,addItem_tariffField,addItem_adminField,addItem_provisionField,
            addItem_storageField,addItem_damageField,addItem_totalField;
    @FXML TextArea addItem_notesField;
    @FXML ChoiceBox<ItemCategory> addItem_categoryChoice;
    @FXML ChoiceBox<String> addItem_statusChoice;
    @FXML DatePicker addItem_pawnDatePicker,addItem_expireDatePicker;
    @FXML Button addItem_confirmAddButton,addItem_cancelAddButton;

    private AddItem.AddItemListener listener;
    public interface AddItemListener { void onAddCreated(boolean success); }
    public void setAddItemListener(AddItem.AddItemListener listener) { this.listener = listener; }

    private Customer selectedCustomer;
    Logic logic = new Logic();

    public void setCustomer(Customer customer) {
        this.selectedCustomer = customer;
        addItem_customerID.setText(addItem_customerID.getText() + (selectedCustomer.getCustomerID() != null ? selectedCustomer.getCustomerID() : ""));
        addItem_customerName.setText(addItem_customerName.getText() + (selectedCustomer.getCustomerName() != null ? selectedCustomer.getCustomerName() : ""));
        addItem_customerAddress.setText(addItem_customerAddress.getText() + (selectedCustomer.getAddress() != null ? selectedCustomer.getAddress() : ""));
    }

    public void initializeAddItem() {
        addItem_confirmAddButton.setDisable(true);

        setupTextFields();
        setupChoiceBox();
        setupDatePickers();
        setupFieldListeners();
        setupButtonHandlers();
    }

    private void setupTextFields() {
        addItem_totalField.setDisable(true);
        logic.setupNumericField(addItem_priceField);
        logic.setupPercentage(addItem_tariffField);
        logic.setupPercentage(addItem_provisionField);
        logic.setupPercentage(addItem_storageField);
        logic.setupPercentage(addItem_damageField);
        logic.setupPercentage(addItem_adminField);

        addItem_priceField.textProperty().addListener((observable, oldValue, newValue) ->
            logic.calculateTotal(addItem_priceField,addItem_tariffField,addItem_adminField,addItem_provisionField,
                    addItem_storageField,addItem_damageField,addItem_totalField));
        addItem_tariffField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(addItem_priceField,addItem_tariffField,addItem_adminField,addItem_provisionField,
                        addItem_storageField,addItem_damageField,addItem_totalField));
        addItem_provisionField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(addItem_priceField,addItem_tariffField,addItem_adminField,addItem_provisionField,
                        addItem_storageField,addItem_damageField,addItem_totalField));
        addItem_storageField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(addItem_priceField,addItem_tariffField,addItem_adminField,addItem_provisionField,
                        addItem_storageField,addItem_damageField,addItem_totalField));
        addItem_damageField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(addItem_priceField,addItem_tariffField,addItem_adminField,addItem_provisionField,
                        addItem_storageField,addItem_damageField,addItem_totalField));
        addItem_adminField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateTotal(addItem_priceField,addItem_tariffField,addItem_adminField,addItem_provisionField,
                        addItem_storageField,addItem_damageField,addItem_totalField));
    }

    private void setupChoiceBox() {
        // Status Choice
        addItem_statusChoice.getItems().addAll("Active","Redeemed","Expired");

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
            addItem_categoryChoice.getItems().addAll(itemCategoryList);

            // Set a StringConverter to display categoryName
            addItem_categoryChoice.setConverter(new StringConverter<ItemCategory>() {
                @Override
                public String toString(ItemCategory itemCategory) {
                    return itemCategory != null ? itemCategory.getCategoryName() : "";
                }

                @Override
                public ItemCategory fromString(String string) {
                    return null;
                }
            });

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }
    }

    private void setupDatePickers() {
        addItem_pawnDatePicker.setDisable(true);
        addItem_pawnDatePicker.setValue(LocalDate.now());
        addItem_expireDatePicker.setValue(addItem_pawnDatePicker.getValue().plusMonths(1));
    }

    private void setupFieldListeners() {
        // Create a ChangeListener to check if fields are filled
        ChangeListener<Object> fieldListener = (ObservableValue<?> observable, Object oldValue, Object newValue) -> checkAllFieldsFilled();

        // Add the listener to the relevant fields
        addItem_itemNameField.textProperty().addListener(fieldListener);
        addItem_categoryChoice.valueProperty().addListener(fieldListener);
        addItem_statusChoice.valueProperty().addListener(fieldListener);
        addItem_pawnDatePicker.valueProperty().addListener(fieldListener);
        addItem_expireDatePicker.valueProperty().addListener(fieldListener);

        addItem_priceField.textProperty().addListener(fieldListener);
        addItem_tariffField.textProperty().addListener(fieldListener);
        addItem_adminField.textProperty().addListener(fieldListener);
        addItem_provisionField.textProperty().addListener(fieldListener);
        addItem_storageField.textProperty().addListener(fieldListener);
        addItem_damageField.textProperty().addListener(fieldListener);
    }

    private void checkAllFieldsFilled() {
        boolean allFieldsFilled =
                !addItem_itemNameField.getText().isEmpty() &&
                        addItem_categoryChoice.getValue() != null &&
                        addItem_statusChoice.getValue() != null &&
                        addItem_pawnDatePicker.getValue() != null &&
                        addItem_expireDatePicker.getValue() != null &&
                        isValidFormattedNumber(addItem_priceField.getText()) &&
                        isValidPercent(addItem_tariffField.getText()) &&
                        isValidFormattedNumber(addItem_adminField.getText()) &&
                        isValidPercent(addItem_provisionField.getText()) &&
                        isValidPercent(addItem_storageField.getText()) &&
                        isValidPercent(addItem_damageField.getText());

        // Enable or disable the confirm button based on whether all fields are filled
        addItem_confirmAddButton.setDisable(!allFieldsFilled);
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

    private boolean isValidPercent(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            // Parse the tariff as a float and check if it's between 0 and 100
            float value = Float.parseFloat(text);
            return value >= 0 && value <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setupButtonHandlers() {
        addItem_confirmAddButton.setOnAction(event -> handleConfirmAddButton());
        addItem_cancelAddButton.setOnAction(event -> closeWindow(addItem_cancelAddButton));
    }

    private void handleConfirmAddButton() {
        String itemID = getNextItemID(addItem_pawnDatePicker.getValue());
        addItem_titleLabel.setText(addItem_titleLabel.getText() + ": " + itemID);
        logic.printReceipt(addItem_VBoxContent, itemID, "Customer Items");

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement preparedStatement = connection.prepareStatement(insertItemQuery)) {

            preparedStatement.setString(1, itemID);
            preparedStatement.setString(2, addItem_itemNameField.getText());
            preparedStatement.setString(3, selectedCustomer.getCustomerID());
            preparedStatement.setString(4, addItem_categoryChoice.getValue().getCategoryID());
            preparedStatement.setString(5, addItem_statusChoice.getValue());
            preparedStatement.setString(6, addItem_pawnDatePicker.getValue().toString());
            preparedStatement.setString(7, addItem_expireDatePicker.getValue().toString());
            preparedStatement.setString(8, addItem_priceField.getText());
            preparedStatement.setString(9, addItem_tariffField.getText());
            preparedStatement.setString(10, addItem_adminField.getText());
            preparedStatement.setString(11, addItem_provisionField.getText());
            preparedStatement.setString(12, addItem_storageField.getText());
            preparedStatement.setString(13, addItem_damageField.getText());
            preparedStatement.setString(14, addItem_totalField.getText());
            preparedStatement.setString(15, addItem_notesField.getText());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        listener.onAddCreated(true);
        closeWindow(addItem_confirmAddButton);
    }

    private String getNextItemID(LocalDate pawnDate) {
        String nextItemID = null;

        // Map the month to a letter
        char monthLetter = (char) ('A' + pawnDate.getMonthValue() - 1);

        // Get the last two digits of the year
        String yearSuffix = String.valueOf(pawnDate.getYear()).substring(2);

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(getItemID)) {

            // Add the month letter and year suffix with a wildcard to the query
            ps.setString(1, monthLetter + yearSuffix + "-%");

            ResultSet rs = ps.executeQuery();
            int maxNumber = 0;

            // Iterate through the results and find the max number
            while (rs.next()) {
                String itemID = rs.getString("itemID");
                if (itemID != null && itemID.contains("-")) {
                    String[] parts = itemID.split("-");
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

            // Format the next itemID as "MonthLetterYearSuffix-Number"
            nextItemID = monthLetter + yearSuffix + "-" + (maxNumber + 1);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return nextItemID;
    }

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
