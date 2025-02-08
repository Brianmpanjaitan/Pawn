package org.example.Toolbar.General;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.Classes.ItemCategory;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditCategory {
    private static final Logger logger = Logger.getLogger(EditCategory.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String categoryQuery = "SELECT * FROM \"Pawn\".\"Item_Category\"";
    private static final String categoryCheckQuery = "SELECT COUNT(*) FROM \"Pawn\".\"Item_Category\" WHERE \"categoryName\" = ?";
    private static final String updateCategoryQuery = "UPDATE \"Pawn\".\"Item_Category\" " +
            "SET \"categoryName\"=? WHERE \"categoryID\"=?";

    @FXML ChoiceBox<ItemCategory> editCategory_categoryChoiceBox;
    @FXML TextField editCategory_categoryNameField;
    @FXML Button editCategory_confirmButton, editCategory_cancelButton;
    private final ObservableList<ItemCategory> categoryList = FXCollections.observableArrayList();

    private EditCategory.CategoryEditListener listener;
    public interface CategoryEditListener { void onCategoryEdited(boolean success); }
    public void setCategoryEditListener(EditCategory.CategoryEditListener listener) { this.listener = listener; }

    @FXML private void initialize() {
        editCategory_confirmButton.setDisable(true);
        setupChoiceBox();
        setupButtonAndListener();
    }

    public void setupChoiceBox() {
        categoryList.clear();
        editCategory_categoryChoiceBox.getItems().clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(categoryQuery)) {

            while (rs.next()) {
                ItemCategory ic = new ItemCategory(
                        rs.getString("categoryID"),
                        rs.getString("categoryName"));
                categoryList.add(ic);
            }

            // Set the items in the ListView with formatted strings
            editCategory_categoryChoiceBox.setItems(FXCollections.observableArrayList(categoryList));

            editCategory_categoryChoiceBox.setConverter(new StringConverter<ItemCategory>() {
                @Override
                public String toString(ItemCategory ic) {
                    return ic != null ? ic.getCategoryName() : "";
                }

                @Override
                public ItemCategory fromString(String string) {
                    return null;
                }
            });

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading category data", e);
        }
    }

    private void setupButtonAndListener() {
        // Buttons
        editCategory_confirmButton.setOnAction(event -> handleEditCustomer());
        editCategory_cancelButton.setOnAction(event -> closeWindow(editCategory_cancelButton));

        // Add a method to check if both fields are valid
        Runnable validateFields = () -> {
            boolean isNameValid = editCategory_categoryNameField.getText() != null && !editCategory_categoryNameField.getText().trim().isEmpty();
            boolean isChoiceBoxValid = editCategory_categoryChoiceBox.getValue() != null;
            editCategory_confirmButton.setDisable(!(isNameValid && isChoiceBoxValid));
        };

        // Text Field Listener for customerNameField
        editCategory_categoryNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateFields.run();
        });

        // Choice Box Listener
        editCategory_categoryChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Populate the text fields with the selected customer's details
                editCategory_categoryNameField.setText(newValue.getCategoryName());
            }
            validateFields.run();
        });

        // Initial state validation
        validateFields.run();
    }

    private void handleEditCustomer() {
        String categoryName = editCategory_categoryNameField.getText().trim();
        String categoryID = editCategory_categoryChoiceBox.getValue().getCategoryID();

        if (categoryName.isEmpty()) {
            showAlert("Enter a name for the Category.");
            if (listener != null) listener.onCategoryEdited(false);
        } else if (!isCustomerNameAvailable(categoryName)) {
            showAlert("Category with that name already exists.");
            if (listener != null) listener.onCategoryEdited(false);
        } else {
            if (!editCategoryRecord(categoryName, categoryID)) {
                showAlert("Error editing category. Please try again.");
                if (listener != null) listener.onCategoryEdited(false);
            } else {
                if (listener != null) listener.onCategoryEdited(true);
                closeWindow(editCategory_confirmButton);
            }
        }
    }

    private boolean isCustomerNameAvailable(String categoryName) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(categoryCheckQuery)) {

            statement.setString(1, categoryName);
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) == 0;

        } catch (SQLException e) {
            logger.warning("Database error: " + e.getMessage());
            return false;
        }
    }

    private boolean editCategoryRecord(String categoryName, String categoryID) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(updateCategoryQuery)) {

            statement.setString(1, categoryName);
            statement.setString(2, categoryID);
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
