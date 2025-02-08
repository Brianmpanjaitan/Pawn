package org.example.Toolbar.Expired;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.Classes.Customer;
import org.example.Classes.Expired;
import org.example.Classes.Item;
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

public class ManageExpired {
    private static final Logger logger = Logger.getLogger(ManageExpired.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String customerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private static final String expiredQuery = "SELECT * FROM \"Pawn\".\"Expired\"";
    private static final String itemQuery =
        """
        SELECT i."itemID", i."itemName", i."customerID", i."categoryID", ic."categoryName", i."status",
               i."pawnDate", i."expireDate", i."price", i."tariff", i."admin", i."provision", i."storageFee", 
               i."damageFee", i."total", i."additionalNotes"
        FROM "Pawn"."Item" i
        JOIN "Pawn"."Item_Category" ic ON i."categoryID" = ic."categoryID"
        """;

    @FXML VBox manageExpired_VBoxContent;
    @FXML Button manageExpired_clearButton, manageExpired_printButton;
    @FXML Label manageExpired_expiredFoundLabel,manageExpired_profitLabel;
    @FXML ChoiceBox<Customer> manageExpired_customerChoiceBox;
    @FXML ChoiceBox<String> manageExpired_dateFilterChoiceBox;
    @FXML TextField manageExpired_expiredIDSearch,manageExpired_customerIDSearch,manageExpired_customerNameSearch,
            manageExpired_itemIDSearch,manageExpired_itemNameSearch,manageExpired_dateSearch,manageExpired_collateralSearch,
            manageExpired_saleSearch,manageExpired_profitSearch,manageExpired_notesSearch;
    @FXML TableView<Expired> manageExpired_expiredTable;

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final ObservableList<Expired> expiredList = FXCollections.observableArrayList();
    private final ObservableList<Item> itemList = FXCollections.observableArrayList();

    Logic logic = new Logic();
    MainMenu mainMenu = new MainMenu();
    private static String foundLabel = "";
    private static String profitLabel = "";

    private User currentUser;
    private String setLanguage;

    public void setUser(User user) {
        this.currentUser = user;
        foundLabel = manageExpired_expiredFoundLabel.getText();
        profitLabel = manageExpired_profitLabel.getText();
    }
    public void setLanguage(String language) { this.setLanguage = language; }

    public void initializeManageExpired() {
        Locale locale = "id".equals(setLanguage) ? new Locale("id") : new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-"+setLanguage+".messages", locale);

        loadItemData();

        setupButtons();
        setupChoiceBoxes(bundle);
        loadExpiredData(bundle);
        setupSearchFields();
        updateItemsFoundLabel();
    }

    private void setupButtons() {
        manageExpired_clearButton.setOnAction(_ -> clearFilters());
        manageExpired_printButton.setOnAction(_ -> logic.printTableInformation(manageExpired_VBoxContent, "ManageExpired"));
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
            manageExpired_customerChoiceBox.setItems(customerList);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error populating customer list", e);
        }

        manageExpired_dateFilterChoiceBox.setItems(FXCollections.observableArrayList(
                bundle.getString("selectAll"),
                bundle.getString("dateFilter.today"),
                bundle.getString("dateFilter.thirty"),
                bundle.getString("dateFilter.year")
        ));

        defineChoiceBoxDisplay(bundle);
        manageExpired_customerChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_dateFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void defineChoiceBoxDisplay(ResourceBundle bundle) {
        Customer selectAllCustomers = new Customer("ALL",bundle.getString("selectAll"),
                "","","","","","","","");
        customerList.addFirst(selectAllCustomers);

        manageExpired_customerChoiceBox.setConverter(new StringConverter<Customer>() {
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
    }

    private void loadExpiredData(ResourceBundle bundle) {
        manageExpired_expiredTable.getColumns().clear();
        manageExpired_expiredTable.getItems().clear();


        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(expiredQuery)) {
            ResultSet rs = ps.executeQuery();

            setupExpiredTableColumns(bundle);
            while (rs.next()) {
                Expired expired = new Expired(
                        rs.getString("expiredID"),
                        rs.getString("date"),
                        rs.getString("collateral"),
                        rs.getString("sale"),
                        rs.getString("profit"),
                        rs.getString("itemID"),
                        rs.getString("itemName"),
                        rs.getString("customerID"),
                        rs.getString("customerName"),
                        rs.getString("notes")
                );
                expiredList.add(expired);
            }
            manageExpired_expiredTable.setItems(expiredList);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving expired items", e);
        }

        // Double-click an item
        manageExpired_expiredTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                Expired selectedItem = manageExpired_expiredTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    Item item = getItemByID(selectedItem.getItemID());
                    Customer customer = getCustomerByID(selectedItem.getCustomerID());
                    mainMenu.showItemInformation(item, customer, false, currentUser);
                }
            }
        });
    }

    private void setupExpiredTableColumns(ResourceBundle bundle) {
        // Define column names and properties
        TableColumn<Expired, String> expiredIDColumn = createColumn(bundle.getString("expired.expiredID"), "expiredIDProperty");
        TableColumn<Expired, String> customerIDColumn = createColumn(bundle.getString("expired.customerID"), "customerIDProperty");
        TableColumn<Expired, String> customerNameColumn = createColumn(bundle.getString("expired.customerName"), "customerNameProperty");
        TableColumn<Expired, String> itemIDColumn = createColumn(bundle.getString("expired.itemID"), "itemIDProperty");
        TableColumn<Expired, String> itemNameColumn = createColumn(bundle.getString("expired.itemName"), "itemNameProperty");
        TableColumn<Expired, String> dateColumn = createColumn(bundle.getString("expired.date"), "dateProperty");
        TableColumn<Expired, String> collateralColumn = createColumn(bundle.getString("expired.collateral"), "collateralProperty");
        TableColumn<Expired, String> saleColumn = createColumn(bundle.getString("expired.sale"), "saleProperty");
        TableColumn<Expired, String> profitColumn = createColumn(bundle.getString("expired.profit"), "profitProperty");
        TableColumn<Expired, String> notesColumn = createColumn(bundle.getString("expired.notes"), "notesProperty");


        // Add all columns to the table
        manageExpired_expiredTable.getColumns().addAll(
                expiredIDColumn,customerIDColumn,customerNameColumn,itemIDColumn,itemNameColumn,dateColumn,collateralColumn,saleColumn,profitColumn,notesColumn
        );

        // Batch setup for column width listeners
        setupTextColumnWidthListener(expiredIDColumn,manageExpired_expiredIDSearch);
        setupTextColumnWidthListener(customerIDColumn,manageExpired_customerIDSearch);
        setupTextColumnWidthListener(customerNameColumn,manageExpired_customerNameSearch);
        setupTextColumnWidthListener(itemIDColumn,manageExpired_itemIDSearch);
        setupTextColumnWidthListener(itemNameColumn,manageExpired_itemNameSearch);
        setupTextColumnWidthListener(dateColumn,manageExpired_dateSearch);
        setupTextColumnWidthListener(collateralColumn,manageExpired_collateralSearch);
        setupTextColumnWidthListener(saleColumn,manageExpired_saleSearch);
        setupTextColumnWidthListener(profitColumn,manageExpired_profitSearch);
        setupTextColumnWidthListener(notesColumn,manageExpired_notesSearch);

    }

    private <T> TableColumn<Expired, T> createColumn(String title, String property) {
        TableColumn<Expired, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> {
            try {
                return (ObservableValue<T>) cellData.getValue().getClass().getMethod(property).invoke(cellData.getValue());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
        return column;
    }

    private void setupTextColumnWidthListener(TableColumn<Expired, ?> column, javafx.scene.control.TextField searchField) {
        column.widthProperty().addListener((observable, oldValue, newValue) -> searchField.setPrefWidth(newValue.doubleValue()));
    }

    private void setupSearchFields() {
        manageExpired_expiredIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_customerIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_customerNameSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_itemIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_itemNameSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_dateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_collateralSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_saleSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_profitSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageExpired_notesSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void filterTable() {
        // Search Field
        String expiredIDFilter = manageExpired_expiredIDSearch.getText().toLowerCase();
        String customerIDFilter = manageExpired_customerIDSearch.getText().toLowerCase();
        String customerNameFilter = manageExpired_customerNameSearch.getText().toLowerCase();
        String itemIDFilter = manageExpired_itemIDSearch.getText().toLowerCase();
        String itemNameFilter = manageExpired_itemNameSearch.getText().toLowerCase();
        String dateFilter = manageExpired_dateSearch.getText().toLowerCase();
        String collateralFilter = manageExpired_collateralSearch.getText().toLowerCase();
        String saleFilter = manageExpired_saleSearch.getText().toLowerCase();
        String profitFilter = manageExpired_profitSearch.getText().toLowerCase();
        String notesFilter = manageExpired_notesSearch.getText().toLowerCase();

        // Get selected values from ChoiceBoxes
        Customer selectedCustomer = manageExpired_customerChoiceBox.getValue();
        String selectedDateFilter = manageExpired_dateFilterChoiceBox.getValue();
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        startDate = switch (selectedDateFilter) {
            case "Last 30 Days" -> today.minusDays(30);
            case "Last Year" -> today.minusYears(1);
            case "Today" -> today; // Only show items from today
            case null, default -> null; // No filter applied
        };

        manageExpired_expiredTable.setItems(expiredList.filtered(expired -> {
            // Filters
            boolean matchesExpiredID = isFilterMatch(expired.getExpiredID(), expiredIDFilter);
            boolean matchesCustomerID = isFilterMatch(expired.getCustomerID(), customerIDFilter);
            boolean matchesCustomerName = isFilterMatch(expired.getCustomerName(), customerNameFilter);
            boolean matchesItemID = isFilterMatch(expired.getItemID(), itemIDFilter);
            boolean matchesItemName = isFilterMatch(expired.getItemName(), itemNameFilter);
            boolean matchesDate = isFilterMatch(expired.getDate(), dateFilter);
            boolean matchesCollateral = isFilterMatch(expired.getCollateral(), collateralFilter);
            boolean matchesSale = isFilterMatch(expired.getSale(), saleFilter);
            boolean matchesProfit = isFilterMatch(expired.getProfit(), profitFilter);
            boolean matchesNotes = isFilterMatch(expired.getNotes(), notesFilter);

            // Match selected customer
            boolean matchesCustomerSelection = selectedCustomer == null ||
                    selectedCustomer.getCustomerID().equals("ALL") ||
                    selectedCustomer.getCustomerID().equals(expired.getCustomerID());

            // Match selected date
            boolean matchesDateSelection = true;
            if (startDate != null) {
                try {
                    LocalDate pawnDate = LocalDate.parse(expired.getDate(), formatter);
                    matchesDateSelection = !pawnDate.isBefore(startDate);
                } catch (Exception e) {
                    matchesDateSelection = false;
                }
            }

            // Combine All Conditions
            return matchesExpiredID && matchesCustomerID && matchesCustomerName && matchesItemID && matchesItemName &&
                    matchesDate && matchesCollateral && matchesSale && matchesProfit && matchesNotes && matchesCustomerSelection &&
                    matchesDateSelection;
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
        ObservableList<TextField> textFields = FXCollections.observableArrayList(
                manageExpired_expiredIDSearch,manageExpired_customerIDSearch,manageExpired_customerNameSearch,
                manageExpired_itemIDSearch,manageExpired_itemNameSearch,manageExpired_dateSearch,manageExpired_collateralSearch,
                manageExpired_saleSearch,manageExpired_profitSearch,manageExpired_notesSearch
        );
        textFields.forEach(TextField::clear);

        // Clear Choice Box
        manageExpired_customerChoiceBox.setValue(null);
        manageExpired_dateFilterChoiceBox.setValue(null);

        // Reset the table view to show all items
        manageExpired_expiredTable.setItems(expiredList);

        // Clear any selected items in the table
        manageExpired_expiredTable.getSelectionModel().clearSelection();

        // Update the interventions found label
        updateItemsFoundLabel();
    }

    private void loadItemData() {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(itemQuery)) {
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
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }
    }

    private Item getItemByID(String itemID) {
        for (Item item : itemList) {
            if (item.getItemID().equals(itemID)) {
                return item;
            }
        }
        return null;
    }

    private Customer getCustomerByID(String customerID) {
        for (Customer customer : customerList) {
            if (customer.getCustomerID().equals(customerID)) {
                return customer;
            }
        }
        return null;
    }

    private void updateItemsFoundLabel() {
        int filteredSize = manageExpired_expiredTable.getItems().size();
        manageExpired_expiredFoundLabel.setText(filteredSize + " " + foundLabel);

        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
        double profitSum = manageExpired_expiredTable.getItems().stream()
                .mapToDouble(expired -> {
                    try {
                        return Double.parseDouble(expired.getProfit().replace(",", ""));
                    } catch (NullPointerException e) {
                        return 0;
                    }
                })
                .sum();

        manageExpired_profitLabel.setText(profitLabel + decimalFormat.format(profitSum));
    }
}
