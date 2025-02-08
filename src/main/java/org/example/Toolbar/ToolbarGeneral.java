package org.example.Toolbar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.Classes.User;
import org.example.Main.MainMenu;
import org.example.Toolbar.General.EditCategory;
import org.example.Toolbar.General.ManageItems;
import org.example.Toolbar.General.NewCategory;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ToolbarGeneral {
    private static final Logger logger = Logger.getLogger(ToolbarGeneral.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";

    @FXML Button toolbarGeneral_manageItemsButton, toolbarGeneral_newCategoryButton, toolbarGeneral_editCategoryButton;

    private User currentUser;
    private MainMenu parentController;

    public void setUser(User user) { this.currentUser = user; }
    public void setParentController(MainMenu parentController) { this.parentController = parentController; }

    @FXML private void initialize()  {
        toolbarGeneral_manageItemsButton.setOnAction(_ -> manageItemsPage());
        toolbarGeneral_newCategoryButton.setOnAction(_ -> newCategoryPage());
        toolbarGeneral_editCategoryButton.setOnAction(_ -> editCategoryPage());
    }

    private void manageItemsPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/manage-items.fxml"));
            String language = getLanguage(fxmlLoader, currentUser);
            Node manageItemsContent = fxmlLoader.load();

            ManageItems manageItems = fxmlLoader.getController();
            if (manageItems == null) {
                throw new IllegalStateException("Controller for manage-assets.fxml is null.");
            }
            manageItems.setUser(currentUser);
            manageItems.setLanguage(language);
            manageItems.initializeManageItems();

            Locale locale = "id".equals(language) ? new Locale("id") : new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle("values-"+language+".messages", locale);
            String tabTitle = bundle.getString("toolbar.manageItems");
            parentController.addNewTab(tabTitle, manageItemsContent);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void newCategoryPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/new-category.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            NewCategory newCategory = fxmlLoader.getController();
            newCategory.setCustomerCreationListener(success -> {
                if (success) {
                    parentController.getCustomerData();
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("New Category");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void editCategoryPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/edit-category.fxml"));
            String language = getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            // Set a listener to handle the result of client creation
            EditCategory editCategory = fxmlLoader.getController();
            editCategory.setCategoryEditListener(success -> {
                if (success) {
                    parentController.getCustomerData();
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(toolbarGeneral_editCategoryButton.getScene().getWindow());
            popupStage.setTitle("Edit Category");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
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
