package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.Classes.User;
import org.example.Main.MainMenu;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserLogin {
    private static final Logger logger = Logger.getLogger(UserLogin.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";
    private static final String userQuery = "SELECT * FROM \"Pawn\".\"User\"";
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";

    @FXML ChoiceBox<User> userLogin_userChoiceBox;
    @FXML TextField userLogin_passwordField;
    @FXML Button userLogin_enterButton;

    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML public void initialize() {
        // Populate user data in ChoiceBox
        getUserData();

        // Set converter to display username in ChoiceBox
        userLogin_userChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user != null ? user.getUsername() : "";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        // Initially disable the enter button
        userLogin_enterButton.setDisable(true);

        // Enable the button when a user is selected and password field is filled
        userLogin_userChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            userLogin_enterButton.setDisable(newValue == null || userLogin_passwordField.getText().isEmpty());
        });

        userLogin_passwordField.textProperty().addListener((observable, oldText, newText) -> {
            userLogin_enterButton.setDisable(userLogin_userChoiceBox.getValue() == null || newText.isEmpty());
        });

        // Action on enterButton to verify password and load next scene
        userLogin_enterButton.setOnAction(event -> {
            if (checkPassword()) {
                loadMainMenu();
            } else {
                showAlert("The entered password is incorrect.");
            }
        });
    }

    private void getUserData() {
        userList.clear(); // Clear existing items to prevent duplication
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(userQuery)) {

            while (resultSet.next()) {
                String userID = resultSet.getString("userID");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                User user = new User(userID, username, password);
                userList.add(user);
            }
            userLogin_userChoiceBox.setItems(userList);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error populating user list", e);
        }
    }

    private boolean checkPassword() {
        User selectedUser = userLogin_userChoiceBox.getValue();
        String enteredPassword = userLogin_passwordField.getText();
        return selectedUser != null && enteredPassword.equals(selectedUser.getPassword());
    }

    private void loadMainMenu() {
        try {
            User selectedUser = userLogin_userChoiceBox.getValue();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main-menu.fxml"));
            String language = getLanguage(fxmlLoader, selectedUser);
            Parent root = fxmlLoader.load();
            MainMenu mainMenuController = fxmlLoader.getController();

            if (selectedUser != null) {
                mainMenuController.setUser(selectedUser);
                mainMenuController.setLanguage(language);
                mainMenuController.initializeMainMenu();
            }

            Scene scene = new Scene(root, 1200, 800);
            Stage stage = (Stage) userLogin_enterButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setTitle(stage.getTitle() + ": " + selectedUser.getUsername());
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading client selection view", e);
        }
    }

    private String getLanguage(FXMLLoader fxmlLoader, User selectedUser) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(languageQuery)) {

            // Bind the parameter to the query
            ps.setString(1, selectedUser.getUserID());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Retrieve locale and bundle from the database
                    String localeString = rs.getString("locale");
                    String bundleName = rs.getString("bundle");

                    // Create a Locale object
                    Locale defaultLocale = new Locale(localeString);
                    ResourceBundle bundle = ResourceBundle.getBundle(bundleName, defaultLocale);
                    fxmlLoader.setResources(bundle);
                    return localeString;
                } else {
                    Locale defaultLocale = new Locale("en");
                    ResourceBundle bundle = ResourceBundle.getBundle("values-en.messages", defaultLocale);
                    fxmlLoader.setResources(bundle);
                    return "en";
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while retrieving language settings: " + e.getMessage(), e);
        }
    }

    // Display alert with specified message
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
