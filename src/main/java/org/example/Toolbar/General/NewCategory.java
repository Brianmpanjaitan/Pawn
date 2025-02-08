package org.example.Toolbar.General;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.Main.CreateCustomer;

import java.sql.*;
import java.util.logging.Logger;

public class NewCategory {
    private static final Logger logger = Logger.getLogger(NewCategory.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String categoryCheckQuery = "SELECT COUNT(*) FROM \"Pawn\".\"Item_Category\" WHERE \"categoryName\" = ?";
    private static final String getCategoryIDQuery = "SELECT \"categoryID\" FROM \"Pawn\".\"Item_Category\"";
    private static final String insertCategoryQuery = "INSERT INTO \"Pawn\".\"Item_Category\" (\"categoryID\", \"categoryName\") VALUES (?, ?)";

    @FXML Button newCategory_confirmButton, newCategory_cancelButton;
    @FXML TextField newCategory_newCategoryField;

    private CreateCustomer.CustomerCreationListener listener;
    public interface CustomerCreationListener { void onCustomerCreated(boolean success); }
    public void setCustomerCreationListener(CreateCustomer.CustomerCreationListener listener) { this.listener = listener; }

    @FXML private void initialize() {
        newCategory_confirmButton.setDisable(true);
        setupButtonsAndListeners();
    }

    private void setupButtonsAndListeners() {
        newCategory_newCategoryField.textProperty().addListener((observable, oldValue, newValue) -> updateCreateButtonState());
        newCategory_confirmButton.setOnAction(event -> handleCreateCategory());
        newCategory_cancelButton.setOnAction(event -> closeWindow(newCategory_cancelButton));
    }

    private void updateCreateButtonState() {
        boolean isNameValid = !newCategory_newCategoryField.getText().trim().isEmpty();
        newCategory_confirmButton.setDisable(!(isNameValid));
    }

    private void handleCreateCategory() {
        String categoryName = newCategory_newCategoryField.getText().trim();

        if (categoryName.isEmpty()) {
            showAlert("Enter a name for the Category.");
            if (listener != null) listener.onCustomerCreated(false);
        } else if (!isCategoryNameAvailable(categoryName)) {
            showAlert("Category with that name already exists.");
            if (listener != null) listener.onCustomerCreated(false);
        } else {
            int newCategoryID = getNextCategoryID();

            if (newCategoryID == -1 || !createCustomerRecord(newCategoryID, categoryName)) {
                showAlert("Error creating new customer. Please try again.");
                if (listener != null) listener.onCustomerCreated(false);
            } else {
                if (listener != null) listener.onCustomerCreated(true);
                closeWindow(newCategory_confirmButton);
            }
        }
    }

    // Check Database for Existing Client Name
    private boolean isCategoryNameAvailable(String customerName) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(categoryCheckQuery)) {

            statement.setString(1, customerName);
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) == 0;

        } catch (SQLException e) {
            logger.warning("Database error: " + e.getMessage());
            return false;
        }
    }

    // Retrieve Next Client ID
    private int getNextCategoryID() {
        int maxNumber = 0;

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(getCategoryIDQuery)) {
            while (rs.next()) {
                String id = rs.getString("categoryID");

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

    private boolean createCustomerRecord(int categoryID, String categoryName) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(insertCategoryQuery)) {

            statement.setInt(1, categoryID);
            statement.setString(2, categoryName);
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
