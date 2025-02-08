package org.example.Toolbar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.example.Classes.User;
import org.example.Main.MainMenu;
import org.example.Toolbar.Savings.ManageSavings;
import org.example.Toolbar.Savings.SavingsPage;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ToolbarSavings {
    private static final Logger logger = Logger.getLogger(ToolbarSavings.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";

    @FXML Button toolbarSavings_savingsMain,toolbarSavings_manageSavings;

    private User currentUser;
    private MainMenu parentController;

    public void setUser(User user) { this.currentUser = user; }
    public void setParentController(MainMenu parentController) { this.parentController = parentController; }

    @FXML private void initialize()  {
        toolbarSavings_savingsMain.setOnAction(_ -> savingsPage());
        toolbarSavings_manageSavings.setOnAction(_ -> manageSavings());
    }

    private void savingsPage() {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/savings-page.fxml"));
            String language = getLanguage(fxmlLoader, currentUser);
            Node manageItemsContent = fxmlLoader.load();

            SavingsPage savingsPage = fxmlLoader.getController();
            if (savingsPage == null) {
                throw new IllegalStateException("Controller for savings-page.fxml is null.");
            }
            savingsPage.setUser(currentUser);
            savingsPage.initializeSavingsPage();

            Locale locale = "id".equals(language) ? new Locale("id") : new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle("values-"+language+".messages", locale);
            String tabTitle = bundle.getString("toolbar.savingsPage");
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
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/manage-savings.fxml"));
            String language = getLanguage(fxmlLoader, currentUser);
            Node manageItemsContent = fxmlLoader.load();

            ManageSavings manageSavings = fxmlLoader.getController();
            if (manageSavings == null) {
                throw new IllegalStateException("Controller for manage-savings.fxml is null.");
            }
            manageSavings.setUser(currentUser);
            manageSavings.setLanguage(language);
            manageSavings.initializeManageSavings();

            Locale locale = "id".equals(language) ? new Locale("id") : new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle("values-"+language+".messages", locale);
            String tabTitle = bundle.getString("toolbar.manageSavings");
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
