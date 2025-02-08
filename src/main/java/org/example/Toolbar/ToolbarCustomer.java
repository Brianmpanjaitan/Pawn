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
import org.example.Main.CreateCustomer;
import org.example.Main.MainMenu;
import org.example.Toolbar.Customer.EditCustomer;
import org.example.Toolbar.Customer.ViewCustomers;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class ToolbarCustomer {
    private static final Logger logger = Logger.getLogger(ToolbarCustomer.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";

    @FXML Button toolbarCustomer_newCustomerButton,toolbarCustomer_editCustomerButton,toolbarCustomer_viewCustomerButton;

    private User currentUser;
    private MainMenu parentController;

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void setParentController(MainMenu parentController) {
        this.parentController = parentController;
    }

    @FXML private void initialize() {
        toolbarCustomer_newCustomerButton.setOnAction(_ -> newCustomerPage());
        toolbarCustomer_editCustomerButton.setOnAction(_ -> editCustomerPage());
        toolbarCustomer_viewCustomerButton.setOnAction(_ -> viewCustomerPage());
    }

    private void newCustomerPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/create-customer.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            // Set a listener to handle the result of client creation
            CreateCustomer createCustomer = fxmlLoader.getController();
            createCustomer.setCustomerCreationListener(success -> {
                if (success) {
                    parentController.getCustomerData();
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(toolbarCustomer_newCustomerButton.getScene().getWindow());
            popupStage.setTitle("Create Customer");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editCustomerPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/edit-customer.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            // Set a listener to handle the result of client creation
            EditCustomer editCustomer = fxmlLoader.getController();
            editCustomer.initializePage();
            editCustomer.setCustomerEditListener(success -> {
                if (success) {
                    parentController.getCustomerData();
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(toolbarCustomer_editCustomerButton.getScene().getWindow());
            popupStage.setTitle("Edit Customer");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void viewCustomerPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view-customers.fxml"));
            String language = getLanguage(fxmlLoader, currentUser);
            Node manageItemsContent = fxmlLoader.load();

            ViewCustomers viewCustomers = fxmlLoader.getController();
            if (viewCustomers == null) {
                throw new IllegalStateException("Controller for view-customers.fxml is null.");
            }
            viewCustomers.setUser(currentUser);
            viewCustomers.setLanguage(language);
            viewCustomers.initializeViewCustomers();

            Locale locale = "id".equals(language) ? new Locale("id") : new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle("values-"+language+".messages", locale);
            String tabTitle = bundle.getString("toolbar.manageItems");
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
