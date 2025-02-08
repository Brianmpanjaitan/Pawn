package org.example.Toolbar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.example.Classes.User;
import org.example.Main.MainMenu;
import org.example.Toolbar.Expired.ExpiredPage;
import org.example.Toolbar.Expired.ManageExpired;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ToolbarExpired {
    private static final Logger logger = Logger.getLogger(ToolbarExpired.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";

    @FXML Button toolbarExpired_expiredPage, toolbarExpired_manageExpired;

    private User currentUser;
    private MainMenu parentController;

    public void setUser(User user) { this.currentUser = user; }
    public void setParentController(MainMenu parentController) { this.parentController = parentController; }

    @FXML private void initialize()  {
        toolbarExpired_expiredPage.setOnAction(_ -> savingsPage());
        toolbarExpired_manageExpired.setOnAction(_ -> manageSavings());
    }

    private void savingsPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/expired-page.fxml"));
            String language = getLanguage(fxmlLoader, currentUser);
            Node manageItemsContent = fxmlLoader.load();

            ExpiredPage expiredPage = fxmlLoader.getController();
            if (expiredPage == null) {
                throw new IllegalStateException("Controller for expired-page.fxml is null.");
            }
            expiredPage.setUser(currentUser);
            expiredPage.setLanguage(language);
            expiredPage.initializeExpiredPage();

            Locale locale = "id".equals(language) ? new Locale("id") : new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle("values-"+language+".messages", locale);
            String tabTitle = bundle.getString("toolbar.expiredPage");
            if (parentController.checkExistingTab(tabTitle)) {
                return;
            }
            parentController.addNewTab(tabTitle, manageItemsContent);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void manageSavings() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/manage-expired.fxml"));
            String language = getLanguage(fxmlLoader, currentUser);
            Node manageItemsContent = fxmlLoader.load();

            ManageExpired manageExpired = fxmlLoader.getController();
            if (manageExpired == null) {
                throw new IllegalStateException("Controller for manage-savings.fxml is null.");
            }
            manageExpired.setUser(currentUser);
            manageExpired.setLanguage(language);
            manageExpired.initializeManageExpired();

            Locale locale = "id".equals(language) ? new Locale("id") : new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle("values-"+language+".messages", locale);
            String tabTitle = bundle.getString("toolbar.manageExpired");
            if (parentController.checkExistingTab(tabTitle)) {
                return;
            }
            parentController.addNewTab(tabTitle, manageItemsContent);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
}
