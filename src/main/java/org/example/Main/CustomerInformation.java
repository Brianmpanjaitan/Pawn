package org.example.Main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.Classes.Customer;
import org.example.Classes.Item;
import org.example.Classes.ItemCategory;
import org.example.Classes.User;
import org.example.Toolbar.Customer.EditCustomer;

import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerInformation {
    private static final Logger logger = Logger.getLogger(CustomerInformation.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String itemCategoryQuery = "SELECT * FROM \"Pawn\".\"Item_Category\"";
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

    @FXML Label customerInformation_customerLabel,customerInformation_customerIDLabel,customerInformation_phoneNumberLabel,
            customerInformation_addressLabel,customerInformation_genderLabel,customerInformation_customerStatusLabel,
            customerInformation_notesLabel,customerInformation_dobLabel,customerInformation_entryLabel,customerInformation_exitLabel;
    @FXML Button customerInformation_editCustomerButton,customerInformation_cancelButton;
    @FXML ChoiceBox<ItemCategory> customerInformation_categoryChoiceBox;
    @FXML ToggleButton customerInformation_activeToggle,customerInformation_pawnedToggle,customerInformation_redeemedToggle,customerInformation_expiredToggle;
    @FXML Button customerInformation_clearButton;
    @FXML TableView<Item> customerInformation_itemTable;

    @FXML Label customerInformation_itemsFoundLabel;
    @FXML TextField customerInformation_itemIDSearch,customerInformation_itemNameSearch,customerInformation_customerIDSearch,
            customerInformation_categorySearch,customerInformation_statusSearch,customerInformation_pawnDateSearch,
            customerInformation_expireDateSearch,customerInformation_priceSearch,customerInformation_tariffSearch,
            customerInformation_adminSearch,customerInformation_provisionSearch,customerInformation_storageFeeSearch,
            customerInformation_damageFeeSearch,customerInformation_totalSearch,customerInformation_additionalNotesSearch;

    private final ObservableList<ItemCategory> itemCategoryList = FXCollections.observableArrayList();
    private final ObservableList<Item> itemList = FXCollections.observableArrayList();

    private User currentUser;
    private String setLanguage;
    private Customer currentCustomer;
    private MainMenu parentController;

    public void setUser(User user) { this.currentUser = user; }
    public void setLanguage(String language) { this.setLanguage = language; }

    public void setCustomer(Customer customer) {
        this.currentCustomer = customer;
        customerInformation_customerLabel.setText(currentCustomer.getCustomerName());
        customerInformation_customerIDLabel.setText(customerInformation_customerIDLabel.getText() + currentCustomer.getCustomerID());
        customerInformation_phoneNumberLabel.setText(customerInformation_phoneNumberLabel.getText() + (currentCustomer.getPhoneNumber() != null ? currentCustomer.getPhoneNumber() : ""));
        customerInformation_addressLabel.setText(customerInformation_addressLabel.getText() + (currentCustomer.getPhoneNumber() != null ? currentCustomer.getPhoneNumber() : ""));
        customerInformation_genderLabel.setText(customerInformation_genderLabel.getText() + (currentCustomer.getGender() != null ? currentCustomer.getGender() : ""));

        customerInformation_customerStatusLabel.setText(customerInformation_customerStatusLabel.getText() + (currentCustomer.getStatus() != null ? currentCustomer.getStatus() : ""));
        customerInformation_dobLabel.setText(customerInformation_dobLabel.getText() + (currentCustomer.getDOB() != null ? currentCustomer.getDOB() : ""));
        customerInformation_entryLabel.setText(customerInformation_entryLabel.getText() + (currentCustomer.getEntryDate() != null ? currentCustomer.getEntryDate() : ""));
        customerInformation_exitLabel.setText(customerInformation_exitLabel.getText() + (currentCustomer.getExitDate() != null ? currentCustomer.getExitDate() : ""));
        customerInformation_notesLabel.setText(customerInformation_notesLabel.getText() + (currentCustomer.getNotes() != null ? currentCustomer.getNotes() : ""));
    }
    public void setParentController(MainMenu parentController) { this.parentController = parentController; }

    public void initializePage() {
        Locale locale = "id".equals(setLanguage) ? new Locale("id") : new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-"+setLanguage+".messages", locale);

        setupChoiceBoxes(bundle);
        setupButtons();

        loadItemData(bundle);
        setupSearchFields();
        updateItemsFoundLabel();
    }

    private void setupChoiceBoxes(ResourceBundle bundle) {
        // Category Choice Box
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(itemCategoryQuery)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemCategory ic = new ItemCategory(
                        rs.getString("categoryID"),
                        rs.getString("categoryName")
                );
                itemCategoryList.add(ic);
            }
            customerInformation_categoryChoiceBox.setItems(itemCategoryList);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }

        defineChoiceBoxDisplay(bundle);
        customerInformation_categoryChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void defineChoiceBoxDisplay(ResourceBundle bundle) {
        ItemCategory selectAllCategory = new ItemCategory("ALL",bundle.getString("selectAll"));
        itemCategoryList.addFirst(selectAllCategory);

        // Display the categoryNames instead of the object representation
        customerInformation_categoryChoiceBox.setConverter(new StringConverter<ItemCategory>() {
            @Override
            public String toString(ItemCategory itemCategory) {
                return itemCategory != null ? itemCategory.getCategoryName() : "";
            }

            @Override
            public ItemCategory fromString(String string) {
                return null;
            }
        });
    }

    private void setupButtons() {
        customerInformation_editCustomerButton.setOnAction(_ -> editCustomerPage());
        customerInformation_cancelButton.setOnAction(_ -> closeWindow(customerInformation_cancelButton));

        customerInformation_clearButton.setOnAction(_ -> clearFilters());

        // Toggle Buttons
        ToggleGroup assetTypeToggle = new ToggleGroup();
        customerInformation_activeToggle.setToggleGroup(assetTypeToggle);
        customerInformation_pawnedToggle.setToggleGroup(assetTypeToggle);
        customerInformation_redeemedToggle.setToggleGroup(assetTypeToggle);
        customerInformation_expiredToggle.setToggleGroup(assetTypeToggle);

        customerInformation_activeToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_pawnedToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_redeemedToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_expiredToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void loadItemData(ResourceBundle bundle) {;
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(matchItemCategoryQuery)) {

            ps.setString(1, currentCustomer.getCustomerID());
            ResultSet rs = ps.executeQuery();

            customerInformation_itemTable.getColumns().clear();
            customerInformation_itemTable.getItems().clear();

            setupItemTableColumns(bundle);
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
            customerInformation_itemTable.setItems(itemList);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }
    }

    private void setupItemTableColumns(ResourceBundle bundle) {
        // Define column names and properties
        TableColumn<Item, String> itemIDColumn = createColumn(bundle.getString("items.itemID"), "itemIDProperty");
        TableColumn<Item, String> itemNameColumn = createColumn(bundle.getString("items.itemName"), "itemNameProperty");
        TableColumn<Item, String> customerIDColumn = createColumn(bundle.getString("items.customerName"), "customerIDProperty");
        TableColumn<Item, String> categoryNameColumn = createColumn(bundle.getString("items.categoryName"), "categoryNameProperty");
        TableColumn<Item, String> statusColumn = createColumn(bundle.getString("items.status"), "statusProperty");
        TableColumn<Item, String> pawnDateColumn = createColumn(bundle.getString("items.pawnDate"), "pawnDateProperty");
        TableColumn<Item, String> expireDateColumn = createColumn(bundle.getString("items.expireDate"), "expireDateProperty");
        TableColumn<Item, String> priceColumn = createColumn(bundle.getString("items.price"), "priceProperty");
        TableColumn<Item, String> tariffColumn = createColumn(bundle.getString("items.tariff"), "tariffProperty");
        TableColumn<Item, String> adminColumn = createColumn(bundle.getString("items.admin"), "adminProperty");
        TableColumn<Item, String> provisionColumn = createColumn(bundle.getString("items.provision"), "provisionProperty");
        TableColumn<Item, String> storageFeeColumn = createColumn(bundle.getString("items.storage"), "storageFeeProperty");
        TableColumn<Item, String> damageFeeColumn = createColumn(bundle.getString("items.damage"), "damageFeeProperty");
        TableColumn<Item, String> totalColumn = createColumn(bundle.getString("items.total"), "totalProperty");
        TableColumn<Item, String> additionNotesColumn = createColumn(bundle.getString("items.notes"), "additionalNotesProperty");

        // Add all columns to the table
        customerInformation_itemTable.getColumns().addAll(
                itemIDColumn,itemNameColumn,customerIDColumn,categoryNameColumn,
                statusColumn,pawnDateColumn,expireDateColumn,priceColumn,
                tariffColumn,adminColumn,provisionColumn,storageFeeColumn,
                damageFeeColumn,totalColumn,additionNotesColumn
        );

        // Batch setup for column width listeners
        setupTextColumnWidthListener(itemIDColumn, customerInformation_itemIDSearch);
        setupTextColumnWidthListener(itemNameColumn, customerInformation_itemNameSearch);
        setupTextColumnWidthListener(customerIDColumn, customerInformation_customerIDSearch);
        setupTextColumnWidthListener(categoryNameColumn, customerInformation_categorySearch);
        setupTextColumnWidthListener(statusColumn, customerInformation_statusSearch);
        setupTextColumnWidthListener(pawnDateColumn, customerInformation_pawnDateSearch);
        setupTextColumnWidthListener(expireDateColumn, customerInformation_expireDateSearch);
        setupTextColumnWidthListener(priceColumn, customerInformation_priceSearch);
        setupTextColumnWidthListener(tariffColumn, customerInformation_tariffSearch);
        setupTextColumnWidthListener(adminColumn, customerInformation_adminSearch);
        setupTextColumnWidthListener(provisionColumn, customerInformation_provisionSearch);
        setupTextColumnWidthListener(storageFeeColumn, customerInformation_storageFeeSearch);
        setupTextColumnWidthListener(damageFeeColumn, customerInformation_damageFeeSearch);
        setupTextColumnWidthListener(totalColumn, customerInformation_totalSearch);
        setupTextColumnWidthListener(additionNotesColumn, customerInformation_additionalNotesSearch);

    }

    private <T> TableColumn<Item, T> createColumn(String title, String property) {
        TableColumn<Item, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> {
            try {
                return (javafx.beans.value.ObservableValue<T>) cellData.getValue().getClass().getMethod(property).invoke(cellData.getValue());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
        return column;
    }

    private void setupTextColumnWidthListener(TableColumn<Item, ?> column, TextField searchField) {
        column.widthProperty().addListener((observable, oldValue, newValue) -> searchField.setPrefWidth(newValue.doubleValue()));
    }

    private void setupSearchFields() {
        customerInformation_itemIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_itemNameSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_customerIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_categorySearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_statusSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_pawnDateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_expireDateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_priceSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_tariffSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_adminSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_provisionSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_storageFeeSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_damageFeeSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_totalSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        customerInformation_additionalNotesSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void filterTable() {
        // Search Field
        String itemIDFilter = customerInformation_itemIDSearch.getText().toLowerCase();
        String itemNameFilter = customerInformation_itemNameSearch.getText().toLowerCase();
        String customerIDFilter = customerInformation_customerIDSearch.getText().toLowerCase();
        String categoryFilter = customerInformation_categorySearch.getText().toLowerCase();
        String statusFilter = customerInformation_statusSearch.getText().toLowerCase();
        String pawnDateFilter = customerInformation_pawnDateSearch.getText().toLowerCase();
        String expireDateFilter = customerInformation_expireDateSearch.getText().toLowerCase();
        String priceFilter = customerInformation_priceSearch.getText().toLowerCase();
        String tariffFilter = customerInformation_tariffSearch.getText().toLowerCase();
        String adminFilter = customerInformation_adminSearch.getText().toLowerCase();
        String provisionFilter = customerInformation_provisionSearch.getText().toLowerCase();
        String storageFeeFilter = customerInformation_storageFeeSearch.getText().toLowerCase();
        String damageFeeFilter = customerInformation_damageFeeSearch.getText().toLowerCase();
        String totalFilter = customerInformation_totalSearch.getText().toLowerCase();
        String additionalNotesFilter = customerInformation_additionalNotesSearch.getText().toLowerCase();

        // Toggle Buttons
        boolean activeSelected = customerInformation_activeToggle.isSelected();
        boolean pawnedSelected = customerInformation_pawnedToggle.isSelected();
        boolean redeemedSelected = customerInformation_redeemedToggle.isSelected();
        boolean expireSelected = customerInformation_expiredToggle.isSelected();

        // Get selected values from ChoiceBoxes
        Customer selectedCustomer = currentCustomer;
        ItemCategory selectedCategory = customerInformation_categoryChoiceBox.getValue();

        customerInformation_itemTable.setItems(itemList.filtered(item -> {
            // Filters
            boolean matchesItemID = isFilterMatch(item.getItemID(), itemIDFilter);
            boolean matchesItemName = isFilterMatch(item.getItemName(), itemNameFilter);
            boolean matchesCustomerID = isFilterMatch(item.getCustomerID(), customerIDFilter);
            boolean matchesCategoryName = isFilterMatch(item.getCategoryName(), categoryFilter);
            boolean matchesStatus = isFilterMatch(item.getStatus(), statusFilter);
            boolean matchesPawn = isFilterMatch(item.getPawnDate(), pawnDateFilter);
            boolean matchesExpire = isFilterMatch(item.getExpireDate(), expireDateFilter);
            boolean matchesPrice = isFilterMatch(item.getPrice(), priceFilter);
            boolean matchesTariff = isFilterMatch(item.getTariff(), tariffFilter);
            boolean matchesAdmin = isFilterMatch(item.getAdmin(), adminFilter);
            boolean matchesProvision = isFilterMatch(item.getProvision(), provisionFilter);
            boolean matchesStorageFee = isFilterMatch(item.getStorageFee(), storageFeeFilter);
            boolean matchesDamageFee = isFilterMatch(item.getDamageFee(), damageFeeFilter);
            boolean matchesTotal = isFilterMatch(item.getTotal(), totalFilter);
            boolean matchesNotes = isFilterMatch(item.getAdditionalNotes(), additionalNotesFilter);

            // Status Logic (Toggle Buttons)
            boolean matchesStatusToggle = true;
            if (activeSelected) {
                matchesStatusToggle = item.getStatus() != null
                        && item.getStatus().contains("Active");
            } else if (pawnedSelected) {
                matchesStatusToggle = item.getStatus() != null
                        && item.getStatus().contains("Pawned");
            } else if (redeemedSelected) {
                matchesStatusToggle = item.getStatus() != null
                        && item.getStatus().contains("Redeemed");
            } else if (expireSelected) {
                matchesStatusToggle = item.getStatus() != null
                        && item.getStatus().contains("Expired");
            }

            // Match selected customer
            boolean matchesCustomerSelection = selectedCustomer == null ||
                    selectedCustomer.getCustomerID().equals("ALL") ||
                    selectedCustomer.getCustomerID().equals(item.getCustomerID());

            // Match selected category
            boolean matchesCategorySelection = selectedCategory == null ||
                    selectedCategory.getCategoryID().equals("ALL") ||
                    selectedCategory.getCategoryID().equals(item.getCategoryID());

            // Combine All Conditions
            return matchesItemID && matchesItemName && matchesCustomerID && matchesCategoryName && matchesStatus &&
                    matchesPawn && matchesExpire && matchesPrice && matchesTariff && matchesAdmin && matchesProvision &&
                    matchesStorageFee && matchesDamageFee && matchesTotal && matchesNotes && matchesStatusToggle &&
                    matchesCustomerSelection && matchesCategorySelection;

        }));
        updateItemsFoundLabel();
    }

    private boolean isFilterMatch(String value, String filter) {
        if (filter.isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return value.toLowerCase().contains(filter);
    }

    private void clearFilters() {
        // Clear all text fields
        ObservableList<TextField> textFields = FXCollections.observableArrayList(
                customerInformation_itemIDSearch,customerInformation_itemNameSearch,customerInformation_customerIDSearch,
                customerInformation_categorySearch,customerInformation_statusSearch,customerInformation_pawnDateSearch,
                customerInformation_expireDateSearch,customerInformation_priceSearch,customerInformation_tariffSearch,
                customerInformation_adminSearch,customerInformation_provisionSearch,customerInformation_storageFeeSearch,
                customerInformation_damageFeeSearch,customerInformation_totalSearch,customerInformation_additionalNotesSearch
        );
        textFields.forEach(TextField::clear);

        // Clear Choice Box
        customerInformation_categoryChoiceBox.setValue(null);

        // Clear all toggle buttons
        ObservableList<ToggleButton> toggleButtons = FXCollections.observableArrayList(customerInformation_activeToggle,
                customerInformation_pawnedToggle,customerInformation_redeemedToggle,customerInformation_expiredToggle);
        toggleButtons.forEach(toggle -> toggle.setSelected(false));

        // Reset the table view to show all items
        customerInformation_itemTable.setItems(itemList);

        // Clear any selected items in the table
        customerInformation_itemTable.getSelectionModel().clearSelection();

        // Update the interventions found label
        updateItemsFoundLabel();
    }

    private void updateItemsFoundLabel() {
        int filteredSize = customerInformation_itemTable.getItems().size();
        customerInformation_itemsFoundLabel.setText(filteredSize + " Item(s) Found");
    }

    private void editCustomerPage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/edit-customer.fxml"));
            getLanguage(fxmlLoader, currentUser);
            Parent root = fxmlLoader.load();

            // Set a listener to handle the result of client creation
            EditCustomer editCustomer = fxmlLoader.getController();
            editCustomer.setCustomer(currentCustomer);
            editCustomer.initializePage();

            editCustomer.setCustomerEditListener(success -> {
                if (success) {
                    parentController.getCustomerData();
                }
            });

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(customerInformation_editCustomerButton.getScene().getWindow());
            popupStage.setTitle("Edit Customer");
            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false);
            popupStage.showAndWait();
            closeWindow(customerInformation_editCustomerButton);

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

    private void closeWindow(Button button) {
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }
}
