package org.example.Toolbar.Savings;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.Classes.*;

import java.io.IOException;
import java.sql.*;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SavingsPage {
    private static final Logger logger = Logger.getLogger(SavingsPage.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String customerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";
    private static final String savingsQuery = "SELECT * FROM \"Pawn\".\"Savings\" WHERE \"customerID\" = ?";

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final ObservableList<Savings> savingsList = FXCollections.observableArrayList();

    @FXML Button savingsPage_addItemButton,savingsPage_deleteItemButton,savingsPage_editItemButton;
    @FXML ListView<Customer> savingsPage_customerListView;
    @FXML ListView<Savings> savingsPage_savingsListView;
    @FXML ChoiceBox<String> savingsPage_statusChoiceBox;
    @FXML TextField savingsPage_customerSearch,savingsPage_savingsIDSearch;

    private User currentUser;
    public void setUser(User user) {
        this.currentUser = user;
    }

    public void initializeSavingsPage() {
        savingsPage_statusChoiceBox.setDisable(true);
        savingsPage_addItemButton.setDisable(true);
        savingsPage_deleteItemButton.setDisable(true);
        savingsPage_editItemButton.setDisable(true);

        getCustomerData();
        setupListeners();
        setupButtonHandlers();
    }

    public void getCustomerData() {
        customerList.clear();
        savingsList.clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(customerQuery)) {

            savingsPage_customerListView.getItems().clear();

            while (rs.next()) {
                Customer customer = new Customer(
                        rs.getString("customerID"),
                        rs.getString("customerName"),
                        rs.getString("phoneNumber"),
                        rs.getString("address"),
                        rs.getString("gender"),
                        rs.getString("dob"),
                        rs.getString("status"),
                        rs.getString("entryDate"),
                        rs.getString("exitDate"),
                        rs.getString("notes"));
                customerList.add(customer);
            }

            // Sort the customerList by customerID (numerical order)
            FXCollections.sort(customerList, Comparator.comparingInt(customer -> Integer.parseInt(customer.getCustomerID())));

            // Set the items in the ListView with formatted strings
            savingsPage_customerListView.setItems(FXCollections.observableArrayList(customerList));

            // Define how to display customers in the ListView
            savingsPage_customerListView.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(Customer customer, boolean empty) {
                    super.updateItem(customer, empty);
                    if (empty || customer == null) {
                        setText(null);
                    } else {
                        setText(customer.getCustomerID() + " : " + customer.getCustomerName());
                    }
                }
            });

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading customer data", e);
        }
    }

    private void setupListeners() {
        savingsPage_customerListView.getSelectionModel().selectedItemProperty().addListener((observable, oldCustomer, newCustomer) -> {
            if (newCustomer != null) {
                // Fetch savings for selected customer and enable the ChoiceBox
                getSavingsData(newCustomer.getCustomerID());
                savingsPage_statusChoiceBox.setDisable(false);
                savingsPage_addItemButton.setDisable(false);
            } else {
                // Clear savings list and disable the ChoiceBox if no customer is selected
                savingsPage_savingsListView.getItems().clear();
                savingsPage_statusChoiceBox.setDisable(true);
            }
        });

        // Populate the ChoiceBox
        savingsPage_statusChoiceBox.getItems().addAll("Select All", "Active", "Redeemed");
        // Add listener to filter the savings list when status is selected
        savingsPage_statusChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            filterSavingsList(newValue);
        });

        savingsPage_savingsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Enable edit and delete buttons if an item is selected
            savingsPage_editItemButton.setDisable(newValue == null);
            savingsPage_deleteItemButton.setDisable(newValue == null);
        });

        // Customer Search
        savingsPage_customerSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            // Filter the customer list based on the text field input
            String filter = newValue.toLowerCase();

            ObservableList<Customer> filteredList = customerList.stream()
                    .filter(customer -> customer.getCustomerName().toLowerCase().contains(filter) ||
                            customer.getCustomerID().toLowerCase().contains(filter))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            // Update the customerListView with the filtered list
            savingsPage_customerListView.setItems(filteredList);
        });

        // Item Search
        savingsPage_savingsIDSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            // Filter the savings list based on the text field input
            String filter = newValue.toLowerCase();

            ObservableList<Savings> filteredList = savingsList.stream()
                    .filter(savings -> savings.getSavingsID().toLowerCase().contains(filter) ||
                            savings.getSavingsID().toLowerCase().contains(filter))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            // Update the customerListView with the filtered list
            savingsPage_savingsListView.setItems(filteredList);
        });
    }

    private void filterSavingsList(String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals("Select All")) {
            // Show all savings items if "Select All" is selected
            savingsPage_savingsListView.setItems(FXCollections.observableArrayList(savingsList));
        } else {
            // Filter the savings list based on the selected status
            ObservableList<Savings> filteredList = savingsList.filtered(savings -> savings.getStatus().equalsIgnoreCase(selectedStatus));
            savingsPage_savingsListView.setItems(filteredList);
        }
    }


    private void getSavingsData(String customerID) {
        savingsPage_customerSearch.clear();
        savingsPage_savingsIDSearch.clear();

        savingsList.clear();
        savingsPage_savingsListView.getItems().clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(savingsQuery)) {

            ps.setString(1, customerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Savings savings = new Savings(
                        rs.getString("savingsID"),
                        rs.getString("customerID"),
                        rs.getString("customerName"),
                        rs.getString("date"),
                        rs.getString("principal"),
                        rs.getString("mandatory"),
                        rs.getString("capital"),
                        rs.getString("voluntary"),
                        rs.getString("others"),
                        rs.getString("total"),
                        rs.getString("notes"),
                        rs.getString("status")
                );
                savingsList.add(savings);
            }

            Platform.runLater(() -> {
                // Sort the savingsList by savingsID (first by letter and year, then by unique number)
                savingsList.sort(Comparator
                        .comparing((Savings savings) -> savings.getSavingsID().substring(0, 3)) // Compare by "A25" (letter + year suffix)
                        .thenComparing(savings -> {
                            // Extract and parse the unique numeric part after "-S"
                            String savingsID = savings.getSavingsID();
                            int hyphenSIndex = savingsID.indexOf("-S");

                            if (hyphenSIndex != -1) {
                                String numericPart = savingsID.substring(hyphenSIndex + 2); // +2 to skip "-S"
                                try {
                                    return Integer.parseInt(numericPart);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace(); // Handle invalid numbers gracefully
                                }
                            }
                            return 0; // Default value if parsing fails
                        }));

                // Bind the sorted list to the ListView
                savingsPage_savingsListView.setItems(FXCollections.observableArrayList(savingsList));

                // Define how to display items in the ListView
                savingsPage_savingsListView.setCellFactory(listView -> new ListCell<>() {
                    @Override
                    protected void updateItem(Savings savings, boolean empty) {
                        super.updateItem(savings, empty);
                        if (empty || savings == null) {
                            setText(null);
                        } else {
                            setText(savings.getSavingsID() + ": " + savings.getCustomerName() + " - " + savings.getTotal() + " - " + savings.getStatus());
                        }
                    }
                });
            });

            // Double-click an item
            savingsPage_savingsListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                    Savings selectedSavings = savingsPage_savingsListView.getSelectionModel().getSelectedItem();
                    if (selectedSavings != null) {
                        showSavingsInformation(selectedSavings, savingsPage_customerListView.getSelectionModel().getSelectedItem(), true);
                    }
                }
            });

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item details", e);
        }
    }

    public void showSavingsInformation(Savings selectedSavings, Customer currentCustomer, Boolean mainMenu) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/savings-information.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            SavingsInformation savingsInformation = fxmlLoader.getController();
            savingsInformation.setUser(currentUser);
            savingsInformation.setCustomer(currentCustomer);
            savingsInformation.setSavings(selectedSavings);
            savingsInformation.initializePage();

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle(currentCustomer.getCustomerName() + ": " + selectedSavings.getSavingsID());
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

            // Get the customerID of the selected customer from the Customer List
            if (mainMenu) { getSavingsData(currentCustomer.getCustomerID()); }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupButtonHandlers() {
        String delete_message = "Are you sure you want to delete this savings?";

        savingsPage_addItemButton.setOnAction(event -> addNewSavings());
        savingsPage_deleteItemButton.setOnAction(event -> {
            if (confirmAction("Delete Item", delete_message, "This action cannot be undone.")) {
                deleteSavings();
            }
        });
        savingsPage_editItemButton.setOnAction(event -> editSelectedItem());
    }

    private void addNewSavings() {
        Customer selectedCustomer = savingsPage_customerListView.getSelectionModel().getSelectedItem();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/add-savings.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            AddSavings addSavings = fxmlLoader.getController();
            addSavings.setCustomer(selectedCustomer);
            addSavings.initializeAddSavings();

            addSavings.setAddSavingsListener(success -> {
                if (success) {
                    getSavingsData(selectedCustomer.getCustomerID());
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(savingsPage_addItemButton.getScene().getWindow());
            popupStage.setTitle("Add Savings");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteSavings() {
        Customer selectedCustomer = savingsPage_customerListView.getSelectionModel().getSelectedItem();
        Savings selectedSavings = savingsPage_savingsListView.getSelectionModel().getSelectedItem();

        String stored_procedure = "CALL \"Pawn\".delete_savings(?);";
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement preparedProcedure = connection.prepareStatement(stored_procedure)) {

            preparedProcedure.setString(1, selectedSavings.getSavingsID());
            preparedProcedure.execute();

            getSavingsData(selectedCustomer.getCustomerID());

        } catch (SQLException e) {
            showAlertBox("An error occurred while deleting the savings.");
            logger.log(Level.SEVERE, "Error deleting item: " + e.getMessage(), e);
        }
    }

    private void editSelectedItem() {
        Customer selectedCustomer = savingsPage_customerListView.getSelectionModel().getSelectedItem();
        Savings selectedSavings = savingsPage_savingsListView.getSelectionModel().getSelectedItem();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/edit-savings.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            EditSavings editSavings = fxmlLoader.getController();
            editSavings.setCustomer(selectedCustomer);
            editSavings.setSavings(selectedSavings);
            editSavings.initializeEditSavings();

            // Set a listener to handle the result of client creation
            editSavings.setEditSavingsListener(success -> {
                if (success) {
                    getSavingsData(selectedCustomer.getCustomerID());
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(savingsPage_editItemButton.getScene().getWindow());
            popupStage.setTitle("Edit Savings");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

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

    private boolean confirmAction(String title, String message, String contextText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, contextText, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlertBox(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
