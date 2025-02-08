package org.example.Main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.Classes.Customer;
import org.example.Classes.Item;
import org.example.Classes.ItemCategory;
import org.example.Classes.User;
import org.example.Toolbar.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainMenu {
    private static final Logger logger = Logger.getLogger(MainMenu.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String customerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private static final String itemCategoryQuery = "SELECT * FROM \"Pawn\".\"Item_Category\"";
    private static final String expiredItemQuery =
        """
        SELECT i."itemID", i."itemName", i."customerID", c."customerName", i."expireDate"
        FROM "Pawn"."Item" i
        JOIN "Pawn"."Customer" c ON i."customerID" = c."customerID"
        WHERE i."status" = 'Active'
        AND CAST(i."expireDate" AS DATE) < CURRENT_DATE
        """;
    private static final String matchItemCategoryQuery =
        """
        SELECT i."itemID", i."itemName", i."customerID", i."categoryID", ic."categoryName", i."status",
               i."pawnDate", i."expireDate", i."price", i."tariff", i."admin", i."provision", i."storageFee", 
               i."damageFee", i."total", i."additionalNotes"
        FROM "Pawn"."Item" i
        JOIN "Pawn"."Item_Category" ic ON i."categoryID" = ic."categoryID"
        WHERE i."customerID" = ?
        """;
    private static final String languageQuery = "SELECT * FROM \"Pawn\".\"Language\" WHERE \"userID\" = ?";

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final ObservableList<Item> itemList = FXCollections.observableArrayList();
    private final ObservableList<Item> expiredItemList = FXCollections.observableArrayList();

    private final ObservableList<ItemCategory> originalItemCategoryList = FXCollections.observableArrayList();
    private final ObservableList<ItemCategory> itemCategoryList = FXCollections.observableArrayList();

    @FXML Button mainMenu_addItemButton,mainMenu_deleteItemButton,mainMenu_editItemButton,mainMenu_newCustomerButton;
    @FXML ListView<Customer> mainMenu_customerListView;
    @FXML ListView<Item> mainMenu_itemListView;
    @FXML ChoiceBox<ItemCategory> mainMenu_itemCategoryChoiceBox;
    @FXML ChoiceBox<String> mainMenu_statusChoiceBox;
    @FXML TextField mainMenu_customerSearch,mainMenu_itemIDSearch;
    @FXML TabPane mainMenu_homeTabPane;

    @FXML HBox mainMenu_toolbarButtons;
    @FXML Button mainMenu_toolbarGeneral,mainMenu_toolbarSavings,mainMenu_toolbarExpired,mainMenu_toolbarCustomer,
            mainMenu_toolbarSettings,mainMenu_toolbarHelp;

    private User currentUser;
    private String setLanguage;

    public void setUser(User user) { this.currentUser = user; }
    public void setLanguage(String language) { this.setLanguage = language; }

    public void initializeMainMenu() {
        Locale locale = "id".equals(setLanguage) ? new Locale("id") : new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-"+setLanguage+".messages", locale);

        mainMenu_itemCategoryChoiceBox.setDisable(true);
        mainMenu_addItemButton.setDisable(true);
        mainMenu_deleteItemButton.setDisable(true);
        mainMenu_editItemButton.setDisable(true);

        initializeItemCategoryList();

        getCustomerData();
        setupListeners(bundle);
        setupFilterField();
        setupButtonHandlers();

        setupToolbar();
        mainMenu_toolbarGeneral.fire(); // Default

        Platform.runLater(this::checkExpiredItems); // Check once the page is fully loaded
    }

    private void initializeItemCategoryList() {
        // Populate the itemCategoryList with all categories
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(itemCategoryQuery)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemCategory ic = new ItemCategory(
                        rs.getString("categoryID"),
                        rs.getString("categoryName")
                );
                originalItemCategoryList.add(ic);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }
    }

    public void getCustomerData() {
        customerList.clear();
        itemList.clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(customerQuery)) {

            mainMenu_customerListView.getItems().clear();

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
            mainMenu_customerListView.setItems(FXCollections.observableArrayList(customerList));

            // Define how to display customers in the ListView
            mainMenu_customerListView.setCellFactory(listView -> new ListCell<>() {
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

            // Double-click a customer
            mainMenu_customerListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                    Customer selectedCustomer = mainMenu_customerListView.getSelectionModel().getSelectedItem();
                    if (selectedCustomer != null) {
                        showCustomerInformation(selectedCustomer, currentUser);
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading customer data", e);
        }
    }

    public void showCustomerInformation(Customer selectedCustomer, User selectedUser) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/customer-information.fxml"));
            String language = getLanguage(fxmlLoader, selectedUser);
            Parent root = fxmlLoader.load();

            CustomerInformation customerInformation = fxmlLoader.getController();
            customerInformation.setUser(selectedUser);
            customerInformation.setLanguage(language);
            customerInformation.setCustomer(selectedCustomer);
            customerInformation.setParentController(this);
            customerInformation.initializePage();

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(mainMenu_itemListView.getScene().getWindow());
            popupStage.setTitle(selectedCustomer.getCustomerName());
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

            // Get the customerID of the selected customer from the Customer List
            getCustomerData();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkExpiredItems() {
        expiredItemList.clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(expiredItemQuery)) {

            StringBuilder items = new StringBuilder("Expired Items:\n");

            while (rs.next()) {
                String itemID = rs.getString("itemID");
                String itemName = rs.getString("itemName");
                String customerID = rs.getString("customerID");
                String customerName = rs.getString("customerName");
                String expireDate = rs.getString("expireDate");

                items.append(String.format("%s: %s - %s: %s - %s%n",
                        customerID, customerName, itemID, itemName, expireDate));
            }

            if (items.length() > "Expired Items:\n".length()) { // Check if there are expired items
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Expired Items");
                alert.setHeaderText("The following items have expired:");
                alert.setContentText(items.toString() + "\n\nDo you want to update the status of all expired items?");

                alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // User confirmed - update the status of expired items
                    try (PreparedStatement updateStmt = connection.prepareStatement(
                            "UPDATE \"Pawn\".\"Item\" " +
                                    "SET \"status\" = 'Expired' " +
                                    "WHERE \"status\" = 'Active' " +
                                    "AND CAST(\"expireDate\" AS DATE) < CURRENT_DATE")) {

                        int updatedRows = updateStmt.executeUpdate();
                        Alert confirmationAlert = new Alert(Alert.AlertType.INFORMATION);
                        confirmationAlert.setTitle("Update Successful");
                        confirmationAlert.setHeaderText(null);
                        confirmationAlert.setContentText(updatedRows + " items have been marked as 'Expired'.");
                        confirmationAlert.showAndWait();
                    }
                }
            } else {
                // No expired items
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Expired Items");
                alert.setHeaderText(null);
                alert.setContentText("No expired items found.");
                alert.showAndWait();
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading customer data", e);
        }
    }

    private void setupListeners(ResourceBundle bundle) {
        // Customer ChoiceBox Listener
        mainMenu_customerListView.getSelectionModel().selectedItemProperty().addListener((observable, oldCustomer, newCustomer) -> {
            if (newCustomer != null) {
                // Fetch items for selected customer and enable ChoiceBox
                getItemData(newCustomer.getCustomerID());
                buildItemCategoryChoiceBox(bundle);
                mainMenu_itemCategoryChoiceBox.setDisable(false);
                mainMenu_addItemButton.setDisable(false);
            } else {
                // Clear items and disable the itemCategoryChoiceBox if no customer is selected
                mainMenu_itemListView.getItems().clear();
                itemCategoryList.clear();
                mainMenu_itemCategoryChoiceBox.setDisable(true);
                mainMenu_addItemButton.setDisable(true);
            }
        });

        // Item Category ChoiceBox Listener
        mainMenu_itemCategoryChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldCategory, newCategory) -> {
            if (newCategory != null) {
                // Filter items based on the selected category and status (if any)
                filterItemListView();
            } else {
                // If no category is selected, show all items based on status (if any)
                filterItemListView();
            }
        });

        // Status Choice Box Listener
        mainMenu_statusChoiceBox.getItems().addAll(bundle.getString("selectAll"), "Active", "Redeemed", "Expired");
        mainMenu_statusChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldStatus, newStatus) -> {
            filterItemListView();
        });

        // Delete/Edit Button Listener
        mainMenu_itemListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // First, check if newValue is null
            if (newValue == null) {
                mainMenu_editItemButton.setDisable(true);
                mainMenu_deleteItemButton.setDisable(true);
                return;
            }

            // Enable edit button only if the status is "Redeemed" or "Expired"
            mainMenu_editItemButton.setDisable(!"Active".equals(newValue.getStatus()));

            mainMenu_deleteItemButton.setDisable(false);
        });
    }

    private void filterItemListView() {
        // Get selected category
        ItemCategory selectedCategory = mainMenu_itemCategoryChoiceBox.getSelectionModel().getSelectedItem();
        String selectedStatus = mainMenu_statusChoiceBox.getSelectionModel().getSelectedItem();

        // Apply filters based on the selected category and status
        ObservableList<Item> filteredItems = itemList.stream()
                .filter(item -> (selectedCategory == null || "ALL".equals(selectedCategory.getCategoryID()) || item.getCategoryID().equals(selectedCategory.getCategoryID())) &&
                        (selectedStatus == null || "Select All".equals(selectedStatus) || item.getStatus().equals(selectedStatus)))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        // Update the ListView with the filtered items
        mainMenu_itemListView.setItems(filteredItems);

        // Optionally, update the ListView cell factory to display the filtered items
        mainMenu_itemListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getItemID() + ": " + item.getItemName() + " - " + item.getCategoryName() + " - " + item.getStatus());
                }
            }
        });
    }

    private void getItemData(String customerID) {
        mainMenu_customerSearch.clear();
        mainMenu_itemIDSearch.clear();

        itemList.clear();
        mainMenu_itemListView.getItems().clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(matchItemCategoryQuery)) {

            ps.setString(1, customerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Item item = new Item(
                        rs.getString("itemID"),
                        rs.getString("itemName"),
                        rs.getString("customerID"),
                        rs.getString("categoryID"),
                        rs.getString("categoryName"),
                        rs.getString("status"),
                        rs.getString("pawnDate"),
                        rs.getString("expireDate"),
                        rs.getString("price"),
                        rs.getString("tariff"),
                        rs.getString("admin"),
                        rs.getString("provision"),
                        rs.getString("storageFee"),
                        rs.getString("damageFee"),
                        rs.getString("total"),
                        rs.getString("additionalNotes")
                );
                itemList.add(item);
            }

            Platform.runLater(() -> {
                // Sort the itemList by itemID (first by letter and year, then by unique number)
                itemList.sort(Comparator
                        .comparing((Item item) -> item.getItemID().substring(0, 3)) // Compare by "A25" (letter + year suffix)
                        .thenComparing(item -> {
                            // Extract and parse the unique numeric part after the hyphen
                            String numericPart = item.getItemID().substring(item.getItemID().indexOf('-') + 1);
                            return Integer.parseInt(numericPart);
                        }));

                // Bind the sorted list to the ListView
                mainMenu_itemListView.setItems(FXCollections.observableArrayList(itemList));

                // Define how to display items in the ListView
                mainMenu_itemListView.setCellFactory(listView -> new ListCell<>() {
                    @Override
                    protected void updateItem(Item item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getItemID() + ": " + item.getItemName() + " - " + item.getCategoryName() + " - " + item.getStatus());
                        }
                    }
                });
            });

            // Double-click an item
            mainMenu_itemListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                    Item selectedItem = mainMenu_itemListView.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        showItemInformation(selectedItem, mainMenu_customerListView.getSelectionModel().getSelectedItem(), true, currentUser);
                    }
                }
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item details", e);
        }
    }

    public void showItemInformation(Item selectedItem, Customer currentCustomer, Boolean mainMenu, User user) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/item-information.fxml"));
            getLanguage(fxmlLoader, user);
            Parent root = fxmlLoader.load();

            ItemInformation itemInformation = fxmlLoader.getController();
            itemInformation.setUser(user);
            itemInformation.setCustomer(currentCustomer);
            itemInformation.setItem(selectedItem);
            itemInformation.initializePage();

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle(currentCustomer.getCustomerName() + ": " + selectedItem.getItemName());
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

            // Get the customerID of the selected customer from the Customer List
            if (mainMenu) { getItemData(currentCustomer.getCustomerID()); }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildItemCategoryChoiceBox(ResourceBundle bundle) {
        // Clear the current itemCategoryList and ChoiceBox
        itemCategoryList.clear();
        mainMenu_itemCategoryChoiceBox.getItems().clear();

        // Create and add the "Select All" option as a special ItemCategory
        ItemCategory selectAllCategory = new ItemCategory("ALL", bundle.getString("selectAll"));
        itemCategoryList.add(selectAllCategory);

        // Get the set of categoryIDs from the itemList
        Set<String> itemCategoryIDs = itemList.stream()
                .map(Item::getCategoryID)
                .collect(Collectors.toSet());

        // Filter the originalItemCategoryList to include only categories present in itemCategoryIDs
        List<ItemCategory> filteredCategories = originalItemCategoryList.stream()
                .filter(category -> itemCategoryIDs.contains(category.getCategoryID()))
                .toList();

        // Add the filtered categories to itemCategoryList
        itemCategoryList.addAll(filteredCategories);

        // Populate the ChoiceBox
        mainMenu_itemCategoryChoiceBox.setItems(itemCategoryList);

        // Display the categoryNames instead of the object representation
        mainMenu_itemCategoryChoiceBox.setConverter(new StringConverter<ItemCategory>() {
            @Override
            public String toString(ItemCategory itemCategory) {
                return itemCategory != null ? itemCategory.getCategoryName() : "";
            }

            @Override
            public ItemCategory fromString(String string) {
                return null;
            }
        });
        mainMenu_itemCategoryChoiceBox.getSelectionModel().select(selectAllCategory);
    }

    private void setupFilterField() {
        // Customer Search
        mainMenu_customerSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            // Filter the customer list based on the text field input
            String filter = newValue.toLowerCase();

            ObservableList<Customer> filteredList = customerList.stream()
                    .filter(customer -> customer.getCustomerName().toLowerCase().contains(filter) ||
                            customer.getCustomerID().toLowerCase().contains(filter))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            // Update the customerListView with the filtered list
            mainMenu_customerListView.setItems(filteredList);
        });

        // Item Search
        mainMenu_itemIDSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            // Filter the item list based on the text field input
            String filter = newValue.toLowerCase();

            ObservableList<Item> filteredList = itemList.stream()
                    .filter(item -> item.getItemID().toLowerCase().contains(filter) ||
                            item.getItemID().toLowerCase().contains(filter))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            // Update the customerListView with the filtered list
            mainMenu_itemListView.setItems(filteredList);
        });
    }

    private void setupButtonHandlers() {
        String delete_message = "Are you sure you want to delete this item?";

        mainMenu_newCustomerButton.setOnAction(event -> openCustomerCreationScreen());
        mainMenu_addItemButton.setOnAction(event -> addNewItem());
        mainMenu_deleteItemButton.setOnAction(event -> {
            if (confirmAction("Delete Item", delete_message, "This action cannot be undone.")) {
                deleteItem();
            }
        });
        mainMenu_editItemButton.setOnAction(event -> editSelectedItem());

    }

    private void openCustomerCreationScreen() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/create-customer.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            // Set a listener to handle the result of client creation
            CreateCustomer createCustomer = fxmlLoader.getController();
            createCustomer.setCustomerCreationListener(success -> {
                if (success) {
                    getCustomerData();
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(mainMenu_newCustomerButton.getScene().getWindow());
            popupStage.setTitle("Create Customer");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addNewItem() {
        Customer selectedCustomer = mainMenu_customerListView.getSelectionModel().getSelectedItem();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/add-item.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            AddItem addItem = fxmlLoader.getController();
            addItem.setCustomer(selectedCustomer);
            addItem.initializeAddItem();

            addItem.setAddItemListener(success -> {
                if (success) {
                    getItemData(selectedCustomer.getCustomerID());
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(mainMenu_addItemButton.getScene().getWindow());
            popupStage.setTitle("Add Item");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteItem() {
        Customer selectedCustomer = mainMenu_customerListView.getSelectionModel().getSelectedItem();
        Item selectedItem = mainMenu_itemListView.getSelectionModel().getSelectedItem();

        String stored_procedure = "CALL \"Pawn\".delete_item(?);";
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement preparedProcedure = connection.prepareStatement(stored_procedure)) {

            preparedProcedure.setString(1, selectedItem.getItemID());
            preparedProcedure.execute();

            getItemData(selectedCustomer.getCustomerID());

        } catch (SQLException e) {
            showAlertBox("An error occurred while deleting the item.");
            logger.log(Level.SEVERE, "Error deleting item: " + e.getMessage(), e);
        }
    }

    private void editSelectedItem() {
        Customer selectedCustomer = mainMenu_customerListView.getSelectionModel().getSelectedItem();
        Item selectedItem = mainMenu_itemListView.getSelectionModel().getSelectedItem();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/edit-item.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            EditItem editItem = fxmlLoader.getController();
            editItem.setItem(selectedItem);
            editItem.initializeEditItem();

            // Set a listener to handle the result of client creation
            editItem.setEditItemListener(success -> {
                if (success) {
                    getItemData(selectedCustomer.getCustomerID());
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(mainMenu_editItemButton.getScene().getWindow());
            popupStage.setTitle("Edit Item");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupToolbar() {
        mainMenu_toolbarGeneral.setOnAction(event -> loadToolbarContent("/toolbar-general.fxml", 1));
        mainMenu_toolbarSavings.setOnAction(event -> loadToolbarContent("/toolbar-savings.fxml", 2));
        mainMenu_toolbarExpired.setOnAction(event -> loadToolbarContent("/toolbar-expired.fxml", 3));
        mainMenu_toolbarCustomer.setOnAction(event -> loadToolbarContent("/toolbar-customer.fxml", 4));
        mainMenu_toolbarSettings.setOnAction(event -> loadToolbarContent("/toolbar-settings.fxml", 5));
        mainMenu_toolbarHelp.setOnAction(event -> loadToolbarContent("/toolbar-help.fxml", 6));
    }

    private void loadToolbarContent(String fxmlFile, int index) {
        try {
            mainMenu_toolbarButtons.getChildren().clear(); // Clear the HBox

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
            getLanguage(fxmlLoader, currentUser);
            Node toolbarContent = fxmlLoader.load();

            switch (index) {
                case 1 -> {
                    ToolbarGeneral tg = fxmlLoader.getController();
                    tg.setUser(currentUser);
                    tg.setParentController(this);
                }
                case 2 -> {
                    ToolbarSavings ts = fxmlLoader.getController();
                    ts.setUser(currentUser);
                    ts.setParentController(this);
                }
                case 3 -> {
                    ToolbarExpired te = fxmlLoader.getController();
                    te.setUser(currentUser);
                    te.setParentController(this);
                }
                case 4 -> {
                    ToolbarCustomer tc = fxmlLoader.getController();
                    tc.setUser(currentUser);
                    tc.setParentController(this);
                }
                case 5 -> {
                    ToolbarSettings ts = fxmlLoader.getController();
                    ts.setUser(currentUser);
                    ts.setParentController(this);
                }
                case 6 -> {
                    ToolbarHelp th = fxmlLoader.getController();
                    th.setUser(currentUser);
                    th.setParentController(this);
                }
            }
            mainMenu_toolbarButtons.getChildren().add(toolbarContent);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load FXML file: " + fxmlFile);

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

    public void addNewTab(String tabTitle, Node content) {
        Tab newTab = new Tab(tabTitle);
        newTab.setContent(content);
        newTab.setClosable(true);
        mainMenu_homeTabPane.getTabs().add(newTab);
    }

    public boolean checkExistingTab(String tabTitle) {
        Tab existingTab = mainMenu_homeTabPane.getTabs().stream()
                .filter(tab -> tab.getText().equals(tabTitle))
                .findFirst()
                .orElse(null);

        if (existingTab != null) {
            mainMenu_homeTabPane.getSelectionModel().select(existingTab);
            return true;
        }
        return false;
    }
}
