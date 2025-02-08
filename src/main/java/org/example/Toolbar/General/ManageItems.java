package org.example.Toolbar.General;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.Classes.Customer;
import org.example.Classes.Item;
import org.example.Classes.ItemCategory;
import org.example.Classes.User;
import org.example.Logic;
import org.example.Main.MainMenu;

import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManageItems {
    private static final Logger logger = Logger.getLogger(ManageItems.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String customerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private static final String itemCategoryQuery = "SELECT * FROM \"Pawn\".\"Item_Category\"";
    private static final String matchItemCategoryQuery =
            """
            SELECT i."itemID", i."itemName", i."customerID", i."categoryID", ic."categoryName", i."status",
                   i."pawnDate", i."expireDate", i."price", i."tariff", i."admin", i."provision", i."storageFee", 
                   i."damageFee", i."total", i."additionalNotes"
            FROM "Pawn"."Item" i
            JOIN "Pawn"."Item_Category" ic ON i."categoryID" = ic."categoryID"
            """;

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final ObservableList<ItemCategory> itemCategoryList = FXCollections.observableArrayList();
    private final ObservableList<Item> itemList = FXCollections.observableArrayList();

    @FXML VBox manageItems_VBoxContent;
    @FXML ChoiceBox<Customer> manageItems_customerChoiceBox;
    @FXML ChoiceBox<ItemCategory> manageItems_categoryChoiceBox;
    @FXML ChoiceBox<String> manageItems_dateFilterChoiceBox;
    @FXML ToggleButton manageItems_activeToggle, manageItems_pawnedToggle, manageItems_redeemedToggle, manageItems_expiredToggle;
    @FXML Button manageItems_clearButton,manageItems_printButton;
    @FXML TableView<Item> manageItems_itemTable;

    @FXML Label manageItems_itemsFoundLabel,manageItems_totalLabel;
    @FXML TextField manageItems_itemIDSearch,manageItems_itemNameSearch,manageItems_customerIDSearch,manageItems_categorySearch,
            manageItems_statusSearch,manageItems_pawnDateSearch,manageItems_expireDateSearch,manageItems_priceSearch,
            manageItems_tariffSearch,manageItems_adminSearch,manageItems_provisionSearch,manageItems_storageFeeSearch,
            manageItems_damageFeeSearch,manageItems_totalSearch,manageItems_additionalNotesSearch;

    Logic logic = new Logic();
    MainMenu mainMenu = new MainMenu();
    private static String foundLabel = "";
    private static String totalLabel = "";

    private User currentUser;
    private String setLanguage;

    public void setUser(User user) {
        this.currentUser = user;
        foundLabel = manageItems_itemsFoundLabel.getText();
        totalLabel = manageItems_totalLabel.getText();
    }
    public void setLanguage(String language) { this.setLanguage = language; }

    public void initializeManageItems() {
        Locale locale = "id".equals(setLanguage) ? new Locale("id") : new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-"+setLanguage+".messages", locale);

        setupChoiceBoxes(bundle);
        setupButtons();

        loadItemData(bundle);
        setupSearchFields();
        updateItemsFoundLabel();
    }

    private void setupChoiceBoxes(ResourceBundle bundle) {
        // Customer Choice Box
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(customerQuery)) {

            ResultSet rs = ps.executeQuery();
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
                        rs.getString("notes")
                );
                customerList.add(customer);
            }
            manageItems_customerChoiceBox.setItems(customerList);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error populating customer list", e);
        }

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
            manageItems_categoryChoiceBox.setItems(itemCategoryList);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }

        // Date Filter Choice Box
        manageItems_dateFilterChoiceBox.setItems(FXCollections.observableArrayList(
                bundle.getString("selectAll"),
                bundle.getString("dateFilter.today"),
                bundle.getString("dateFilter.thirty"),
                bundle.getString("dateFilter.year")
        ));

        defineChoiceBoxDisplay(bundle);

        manageItems_customerChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_categoryChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_dateFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void defineChoiceBoxDisplay(ResourceBundle bundle) {
        Customer selectAllCustomers = new Customer("ALL",bundle.getString("selectAll"),
                "","","","","","","","");
        ItemCategory selectAllCategory = new ItemCategory("ALL",bundle.getString("selectAll"));
        customerList.addFirst(selectAllCustomers);
        itemCategoryList.addFirst(selectAllCategory);

        // Display the customerNames instead of the object representation
        manageItems_customerChoiceBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                if (customer != null) {
                    if ("ALL".equals(customer.getCustomerID())) {
                        return bundle.getString("selectAll");
                    }
                    return customer.getCustomerID() + ": " + customer.getCustomerName();
                }
                return "";
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        });

        // Display the categoryNames instead of the object representation
        manageItems_categoryChoiceBox.setConverter(new StringConverter<ItemCategory>() {
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
        manageItems_clearButton.setOnAction(_ -> clearFilters());
        manageItems_printButton.setOnAction(_ -> logic.printTableInformation(manageItems_VBoxContent, "ManageItems"));

        // Toggle Buttons
        ToggleGroup assetTypeToggle = new ToggleGroup();
        manageItems_activeToggle.setToggleGroup(assetTypeToggle);
        manageItems_pawnedToggle.setToggleGroup(assetTypeToggle);
        manageItems_redeemedToggle.setToggleGroup(assetTypeToggle);
        manageItems_expiredToggle.setToggleGroup(assetTypeToggle);

        manageItems_activeToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_pawnedToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_redeemedToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_expiredToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void loadItemData(ResourceBundle bundle) {
        manageItems_itemTable.getColumns().clear();
        manageItems_itemTable.getItems().clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(matchItemCategoryQuery)) {
            ResultSet rs = ps.executeQuery();

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
            manageItems_itemTable.setItems(itemList);

            // Double-click an item
            manageItems_itemTable.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                    Item selectedItem = manageItems_itemTable.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        mainMenu.showItemInformation(selectedItem, getCustomerFromItem(selectedItem), false, currentUser);
                    }
                }
            });

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }
    }

    private Customer getCustomerFromItem(Item selectedItem) {
        String customerID = selectedItem.getCustomerID();
        return customerList.stream()
                .filter(customer -> customer.getCustomerID().equals(customerID))
                .findFirst()
                .orElse(null);
    }

    private void setupItemTableColumns(ResourceBundle bundle) {
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
        manageItems_itemTable.getColumns().addAll(
                itemIDColumn, itemNameColumn, customerIDColumn, categoryNameColumn,
                statusColumn, pawnDateColumn, expireDateColumn, priceColumn,
                tariffColumn, adminColumn, provisionColumn, storageFeeColumn,
                damageFeeColumn, totalColumn, additionNotesColumn
        );

        // Batch setup for column width listeners
        setupTextColumnWidthListener(itemIDColumn, manageItems_itemIDSearch);
        setupTextColumnWidthListener(itemNameColumn, manageItems_itemNameSearch);
        setupTextColumnWidthListener(customerIDColumn, manageItems_customerIDSearch);
        setupTextColumnWidthListener(categoryNameColumn, manageItems_categorySearch);
        setupTextColumnWidthListener(statusColumn, manageItems_statusSearch);
        setupTextColumnWidthListener(pawnDateColumn, manageItems_pawnDateSearch);
        setupTextColumnWidthListener(expireDateColumn, manageItems_expireDateSearch);
        setupTextColumnWidthListener(priceColumn, manageItems_priceSearch);
        setupTextColumnWidthListener(tariffColumn, manageItems_tariffSearch);
        setupTextColumnWidthListener(adminColumn, manageItems_adminSearch);
        setupTextColumnWidthListener(provisionColumn, manageItems_provisionSearch);
        setupTextColumnWidthListener(storageFeeColumn, manageItems_storageFeeSearch);
        setupTextColumnWidthListener(damageFeeColumn, manageItems_damageFeeSearch);
        setupTextColumnWidthListener(totalColumn, manageItems_totalSearch);
        setupTextColumnWidthListener(additionNotesColumn, manageItems_additionalNotesSearch);
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
        manageItems_itemIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_itemNameSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_customerIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_categorySearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_statusSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_pawnDateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_expireDateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_priceSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_tariffSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_adminSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_provisionSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_storageFeeSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_damageFeeSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_totalSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageItems_additionalNotesSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void filterTable() {
        // Search Field
        String itemIDFilter = manageItems_itemIDSearch.getText().toLowerCase();
        String itemNameFilter = manageItems_itemNameSearch.getText().toLowerCase();
        String customerIDFilter = manageItems_customerIDSearch.getText().toLowerCase();
        String categoryFilter = manageItems_categorySearch.getText().toLowerCase();
        String statusFilter = manageItems_statusSearch.getText().toLowerCase();
        String pawnDateFilter = manageItems_pawnDateSearch.getText().toLowerCase();
        String expireDateFilter = manageItems_expireDateSearch.getText().toLowerCase();
        String priceFilter = manageItems_priceSearch.getText().toLowerCase();
        String tariffFilter = manageItems_tariffSearch.getText().toLowerCase();
        String adminFilter = manageItems_adminSearch.getText().toLowerCase();
        String provisionFilter = manageItems_provisionSearch.getText().toLowerCase();
        String storageFeeFilter = manageItems_storageFeeSearch.getText().toLowerCase();
        String damageFeeFilter = manageItems_damageFeeSearch.getText().toLowerCase();
        String totalFilter = manageItems_totalSearch.getText().toLowerCase();
        String additionalNotesFilter = manageItems_additionalNotesSearch.getText().toLowerCase();

        // Toggle Buttons
        boolean activeSelected = manageItems_activeToggle.isSelected();
        boolean pawnedSelected = manageItems_pawnedToggle.isSelected();
        boolean redeemedSelected = manageItems_redeemedToggle.isSelected();
        boolean expireSelected = manageItems_expiredToggle.isSelected();

        // Get selected values from ChoiceBoxes
        Customer selectedCustomer = manageItems_customerChoiceBox.getValue();
        ItemCategory selectedCategory = manageItems_categoryChoiceBox.getValue();

        // Date Filter
        String selectedDateFilter = manageItems_dateFilterChoiceBox.getValue();
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        startDate = switch (selectedDateFilter) {
            case "Last 30 Days" -> today.minusDays(30);
            case "Last Year" -> today.minusYears(1);
            case "Today" -> today; // Only show items from today
            case null, default -> null; // No filter applied
        };

        manageItems_itemTable.setItems(itemList.filtered(item -> {
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

            // Match selected date
            boolean matchesDateSelection = true;
            if (startDate != null) {
                try {
                    LocalDate pawnDate = LocalDate.parse(item.getPawnDate(), formatter);
                    matchesDateSelection = !pawnDate.isBefore(startDate);
                } catch (Exception e) {
                    matchesDateSelection = false;
                }
            }

            // Combine All Conditions
            return matchesItemID && matchesItemName && matchesCustomerID && matchesCategoryName && matchesStatus &&
                    matchesPawn && matchesExpire && matchesPrice && matchesTariff && matchesAdmin && matchesProvision &&
                    matchesStorageFee && matchesDamageFee && matchesTotal && matchesNotes && matchesStatusToggle &&
                    matchesCustomerSelection && matchesCategorySelection && matchesDateSelection;

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
                manageItems_itemIDSearch,manageItems_itemNameSearch,manageItems_customerIDSearch,manageItems_categorySearch,
                manageItems_statusSearch,manageItems_pawnDateSearch,manageItems_expireDateSearch,manageItems_priceSearch,
                manageItems_tariffSearch,manageItems_adminSearch,manageItems_provisionSearch,manageItems_storageFeeSearch,
                manageItems_damageFeeSearch,manageItems_totalSearch,manageItems_additionalNotesSearch
        );
        textFields.forEach(TextField::clear);

        // Clear Choice Box
        manageItems_customerChoiceBox.setValue(null);
        manageItems_categoryChoiceBox.setValue(null);
        manageItems_dateFilterChoiceBox.setValue(null);

        // Clear all toggle buttons
        ObservableList<ToggleButton> toggleButtons = FXCollections.observableArrayList(manageItems_activeToggle,manageItems_pawnedToggle,manageItems_redeemedToggle,manageItems_expiredToggle);
        toggleButtons.forEach(toggle -> toggle.setSelected(false));

        // Reset the table view to show all items
        manageItems_itemTable.setItems(itemList);

        // Clear any selected items in the table
        manageItems_itemTable.getSelectionModel().clearSelection();

        // Update the interventions found label
        updateItemsFoundLabel();
    }

    private void updateItemsFoundLabel() {
        int filteredSize = manageItems_itemTable.getItems().size();
        manageItems_itemsFoundLabel.setText(filteredSize + " " + foundLabel);

        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
        double totalSum = manageItems_itemTable.getItems().stream()
                .mapToDouble(item -> {
                    try {
                        return Double.parseDouble(item.getTotal().replace(",", ""));
                    } catch (NullPointerException e) {
                        return 0;
                    }
                })
                .sum();

        manageItems_totalLabel.setText(totalLabel + decimalFormat.format(totalSum));
    }
}

