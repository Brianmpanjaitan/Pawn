package org.example.Toolbar.Help;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import org.example.Classes.User;

import java.sql.*;
import java.util.logging.Logger;

public class LanguageOption {
    private static final Logger logger = Logger.getLogger(LanguageOption.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";
    private static final String updateLanguageQuery = "UPDATE \"Pawn\".\"Language\" SET \"locale\"=?, \"bundle\"=? WHERE \"userID\"=?";

    @FXML ChoiceBox<String> language_languageChoice;
    @FXML Button language_confirmButton,language_cancelButton;

    private User currentUser;
    public void setUser(User user) { this.currentUser = user; }

    public void initializeLanguageOption() {
        language_confirmButton.setDisable(true);

        setupChoiceBox();
        getLanguage();
        setupListenersAndButtons();
    }

    private void setupChoiceBox() {
        language_languageChoice.getItems().addAll("English", "Indonesian");
    }

    private void getLanguage() {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(languageQuery)) {

            statement.setString(1, currentUser.getUserID());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String locale = resultSet.getString("locale");
                if ("en".equals(locale)) {
                    language_languageChoice.setValue("English");
                } else if ("id".equals(locale)) {
                    language_languageChoice.setValue("Indonesian");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error while retrieving language settings: " + e.getMessage(), e);
        }
    }

    private void setupListenersAndButtons() {
        language_languageChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            language_confirmButton.setDisable(newValue == null);
        });

        language_confirmButton.setOnAction(event -> updateLanguage());
        language_cancelButton.setOnAction(event -> closeWindow(language_cancelButton));
    }

    private void updateLanguage() {
        String selectedLanguage = language_languageChoice.getValue();
        if (selectedLanguage == null) return;

        String locale = "en";
        String bundle = "values-en.messages";
        if ("Indonesian".equals(selectedLanguage)) {
            locale = "id";
            bundle = "values-id.messages";
        }

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(updateLanguageQuery)) {

            statement.setString(1, locale);
            statement.setString(2, bundle);
            statement.setString(3, currentUser.getUserID());
            statement.executeUpdate();

        } catch (SQLException e) {
            logger.warning("Database error while updating language: " + e.getMessage());
        }
        closeWindow(language_confirmButton);
    }

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
