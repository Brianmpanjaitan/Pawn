package org.example.Toolbar.Settings;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Objects;
import java.util.logging.Logger;

public class CreateUser {
    private static final Logger logger = Logger.getLogger(CreateUser.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String userCheckQuery = "SELECT COUNT(*) FROM \"Pawn\".\"User\" WHERE \"username\" = ?";
    private static final String getUserIDQuery = "SELECT \"userID\" FROM \"Pawn\".\"User\"";
    private static final String insertUserQuery = "INSERT INTO \"Pawn\".\"User\"(\"userID\", username, \"password\") VALUES (?, ?, ?);";
    private static final String insertLanguageQuery = "INSERT INTO \"Pawn\".\"Language\"(\"userID\", \"locale\", \"bundle\") VALUES (?, ?, ?)";

    @FXML TextField createUser_userField,createUser_passwordField;
    @FXML Button createUser_confirmButton,createUser_cancelButton;
    @FXML ChoiceBox<String> createUser_languageChoice;

    public void initializeCreateUser() {
        createUser_confirmButton.setDisable(true);

        createUser_languageChoice.getItems().addAll("English", "Indonesian");
        createUser_confirmButton.setOnAction(event -> handleCreateUser());
        createUser_cancelButton.setOnAction(event -> closeAddPopup(createUser_cancelButton));

        // Enable the button when a user field is filled, language is selected, and password field is filled
        createUser_userField.textProperty().addListener((observable, oldValue, newValue) -> {
            createUser_confirmButton.setDisable(
                    createUser_userField.getText().isEmpty() ||
                            createUser_passwordField.getText().isEmpty() ||
                            createUser_languageChoice.getValue() == null
            );
        });

        createUser_passwordField.textProperty().addListener((observable, oldText, newText) -> {
            createUser_confirmButton.setDisable(
                    createUser_userField.getText().isEmpty() ||
                            createUser_passwordField.getText().isEmpty() ||
                            createUser_languageChoice.getValue() == null
            );
        });

        createUser_languageChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            createUser_confirmButton.setDisable(
                    createUser_userField.getText().isEmpty() ||
                            createUser_passwordField.getText().isEmpty() ||
                            newValue == null
            );
        });
    }

    private void handleCreateUser() {
        String userName = createUser_userField.getText().trim();
        String language = createUser_languageChoice.getValue();

        if (userName.isEmpty()) {
            showAlert("Enter a name for the user.");
        } else if (!isUserNameAvailable(userName)) {
            showAlert("User with that name already exists.");
        } else {
            int newUserID = getNextUserID();

            if (newUserID == -1 || !createUser(newUserID, userName, language)) {
                showAlert("Error creating new user. Please try again.");
            } else {
                closeAddPopup(createUser_confirmButton);
            }
        }
    }

    private boolean isUserNameAvailable(String clientName) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(userCheckQuery)) {

            statement.setString(1, clientName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() && resultSet.getInt(1) == 0;

        } catch (SQLException e) {
            logger.warning("Database error: " + e.getMessage());
            return false;
        }
    }

    private int getNextUserID() {
        int maxNumber = 0;

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(getUserIDQuery)) {
            while (rs.next()) {
                String id = rs.getString("userID");

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

    private boolean createUser(int newUserID, String userName, String language) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, sqluser, sqlpassword);
            connection.setAutoCommit(false); // Begin transaction

            // Insert into "User"
            try (PreparedStatement psUser = connection.prepareStatement(insertUserQuery)) {
                psUser.setInt(1, newUserID);
                psUser.setString(2, userName);
                psUser.setString(3, createUser_passwordField.getText());
                psUser.executeUpdate();
            }

            // Insert into "Language"
            try (PreparedStatement psLanguage = connection.prepareStatement(insertLanguageQuery)) {
                psLanguage.setInt(1, newUserID);
                if (Objects.equals(language, "English")) {
                    psLanguage.setString(2, "en");
                    psLanguage.setString(3, "values-en.messages");
                } else {
                    psLanguage.setString(2, "id");
                    psLanguage.setString(3, "values-id.messages");
                }
                psLanguage.executeUpdate();
            }

            connection.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            logger.warning("Database error: " + e.getMessage());
            if (connection != null) {
                try {
                    connection.rollback(); // Rollback on error
                } catch (SQLException rollbackEx) {
                    logger.warning("Rollback error: " + rollbackEx.getMessage());
                }
            }
            return false;

        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Reset auto-commit
                    connection.close();
                } catch (SQLException closeEx) {
                    logger.warning("Connection close error: " + closeEx.getMessage());
                }
            }
        }
    }

    private void closeAddPopup(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
