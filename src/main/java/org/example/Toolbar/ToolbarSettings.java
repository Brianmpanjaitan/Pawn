package org.example.Toolbar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.Classes.User;
import org.example.Main.MainMenu;
import org.example.Start;
import org.example.Toolbar.Settings.CreateUser;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ToolbarSettings {
    private static final Logger logger = Logger.getLogger(ToolbarSettings.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";

    @FXML Button toolbarSettings_newUserButton, toolbarSettings_userSelectionButton;

    private User currentUser;
    private MainMenu parentController;

    public void setUser(User user) { this.currentUser = user; }
    public void setParentController(MainMenu parentController) { this.parentController = parentController; }

    @FXML private void initialize() {
        toolbarSettings_newUserButton.setOnAction(_ -> newUserPage());
        toolbarSettings_userSelectionButton.setOnAction(_ -> userSelectionPage());
    }

    private void newUserPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/create-user.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            CreateUser createUser = fxmlLoader.getController();
            createUser.initializeCreateUser();

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(toolbarSettings_userSelectionButton.getScene().getWindow());
            popupStage.setTitle("Create User");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void userSelectionPage() {
        try {
            // Load FXML
            FXMLLoader fxmlLoader = new FXMLLoader(Start.class.getResource("/user-login.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root);
            Stage stage = (Stage) toolbarSettings_userSelectionButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setTitle("Pawn Database");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getLanguage(FXMLLoader fxmlLoader, User selectedUser) {
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
                } else {
                    Locale defaultLocale = new Locale("en");
                    ResourceBundle bundle = ResourceBundle.getBundle("values-en.messages", defaultLocale);
                    fxmlLoader.setResources(bundle);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while retrieving language settings: " + e.getMessage(), e);
        }
    }
}
