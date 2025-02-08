package org.example.Toolbar.Savings;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.Classes.*;
import org.example.Logic;

import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManageSavings {
    private static final Logger logger = Logger.getLogger(ManageSavings.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String customerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private static final String savingsQuery = "SELECT * FROM \"Pawn\".\"Savings\"";

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final ObservableList<Savings> savingsList = FXCollections.observableArrayList();

    @FXML VBox manageSavings_VBoxContent;
    @FXML Label manageSavings_savingsFoundLabel,manageSavings_searchLabel,manageSavings_totalLabel;
    @FXML ChoiceBox<Customer> manageSavings_customerChoiceBox;
    @FXML ChoiceBox<String> manageSavings_dateFilterChoiceBox;
    @FXML ToggleButton manageSavings_activeToggle,manageSavings_redeemedToggle;
    @FXML Button manageSavings_clearButton,manageSavings_printButton;
    @FXML TextField manageSavings_savingsIDSearch, manageSavings_customerIDSearch, manageSavings_customerNameSearch,
            manageSavings_statusSearch, manageSavings_dateSearch, manageSavings_principalSearch,
            manageSavings_mandatorySearch, manageSavings_capitalSearch, manageSavings_voluntarySearch,
            manageSavings_othersSearch, manageSavings_totalSearch, manageSavings_notesSearch;
    @FXML TableView<Savings> manageSavings_savingsTable;

    Logic logic = new Logic();
    SavingsPage savingsPage = new SavingsPage();
    private static String foundLabel = "";
    private static String totalLabel = "";

    private User currentUser;
    private String setLanguage;

    public void setUser(User user) {
        this.currentUser = user;
        foundLabel = manageSavings_savingsFoundLabel.getText();
        totalLabel = manageSavings_totalLabel.getText();
    }
    public void setLanguage(String language) { this.setLanguage = language; }

    public void initializeManageSavings() {
        Locale locale = "id".equals(setLanguage) ? new Locale("id") : new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-"+setLanguage+".messages", locale);

        setupChoiceBoxes(bundle);
        setupButtons();

        loadSavingsData(bundle);
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
            manageSavings_customerChoiceBox.setItems(customerList);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error populating customer list", e);
        }

        manageSavings_dateFilterChoiceBox.setItems(FXCollections.observableArrayList(
                bundle.getString("selectAll"),
                bundle.getString("dateFilter.today"),
                bundle.getString("dateFilter.thirty"),
                bundle.getString("dateFilter.year")
        ));

        defineChoiceBoxDisplay(bundle);
        manageSavings_customerChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_dateFilterChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void defineChoiceBoxDisplay(ResourceBundle bundle) {
        Customer selectAllCustomers = new Customer("ALL",bundle.getString("selectAll"),
                "","","","","","","","");
        customerList.addFirst(selectAllCustomers);

        manageSavings_customerChoiceBox.setConverter(new StringConverter<Customer>() {
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

    private void setupButtons() {
        manageSavings_clearButton.setOnAction(_ -> clearFilters());
        manageSavings_printButton.setOnAction(_ -> logic.printTableInformation(manageSavings_VBoxContent, "ManageSavings"));

        // Toggle Buttons
        ToggleGroup assetTypeToggle = new ToggleGroup();
        manageSavings_activeToggle.setToggleGroup(assetTypeToggle);
        manageSavings_redeemedToggle.setToggleGroup(assetTypeToggle);

        manageSavings_activeToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_redeemedToggle.selectedProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void loadSavingsData(ResourceBundle bundle) {
        manageSavings_savingsTable.getColumns().clear();
        manageSavings_savingsTable.getItems().clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(savingsQuery)) {
            ResultSet rs = ps.executeQuery();

            setupSavingsTableColumns(bundle);
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
            manageSavings_savingsTable.setItems(savingsList);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item categories", e);
        }
    }

    private void setupSavingsTableColumns(ResourceBundle bundle) {
        // Define column names and properties
        TableColumn<Savings, String> savingsIDColumn = createColumn(bundle.getString("savings.savingsID"), "savingsIDProperty");
        TableColumn<Savings, String> customerIDColumn = createColumn(bundle.getString("savings.customerID"), "customerIDProperty");
        TableColumn<Savings, String> customerNameColumn = createColumn(bundle.getString("savings.customerName"), "customerNameProperty");
        TableColumn<Savings, String> statusColumn = createColumn(bundle.getString("savings.status"), "statusProperty");
        TableColumn<Savings, String> dateColumn = createColumn(bundle.getString("savings.date"), "dateProperty");
        TableColumn<Savings, String> principalColumn = createColumn(bundle.getString("savings.principal"), "principalProperty");
        TableColumn<Savings, String> mandatoryColumn = createColumn(bundle.getString("savings.mandatory"), "mandatoryProperty");
        TableColumn<Savings, String> capitalColumn = createColumn(bundle.getString("savings.capital"), "capitalProperty");
        TableColumn<Savings, String> voluntaryColumn = createColumn(bundle.getString("savings.voluntary"), "voluntaryProperty");
        TableColumn<Savings, String> otherColumn = createColumn(bundle.getString("savings.other"), "othersProperty");
        TableColumn<Savings, String> totalColumn = createColumn(bundle.getString("savings.total"), "totalProperty");
        TableColumn<Savings, String> notesColumn = createColumn(bundle.getString("savings.notes"), "notesProperty");

        // Add all columns to the table
        manageSavings_savingsTable.getColumns().addAll(
                savingsIDColumn,customerIDColumn,customerNameColumn,statusColumn,dateColumn,principalColumn,
                mandatoryColumn,capitalColumn,voluntaryColumn,otherColumn,totalColumn,notesColumn
        );

        // Batch setup for column width listeners
        setupTextColumnWidthListener(savingsIDColumn,manageSavings_savingsIDSearch);
        setupTextColumnWidthListener(customerIDColumn, manageSavings_customerIDSearch);
        setupTextColumnWidthListener(customerNameColumn, manageSavings_customerNameSearch);
        setupTextColumnWidthListener(statusColumn, manageSavings_statusSearch);
        setupTextColumnWidthListener(dateColumn, manageSavings_dateSearch);
        setupTextColumnWidthListener(principalColumn, manageSavings_principalSearch);
        setupTextColumnWidthListener(mandatoryColumn, manageSavings_mandatorySearch);
        setupTextColumnWidthListener(capitalColumn, manageSavings_capitalSearch);
        setupTextColumnWidthListener(voluntaryColumn, manageSavings_voluntarySearch);
        setupTextColumnWidthListener(otherColumn, manageSavings_othersSearch);
        setupTextColumnWidthListener(totalColumn, manageSavings_totalSearch);
        setupTextColumnWidthListener(notesColumn, manageSavings_notesSearch);
    }

    private <T> TableColumn<Savings, T> createColumn(String title, String property) {
        TableColumn<Savings, T> column = new TableColumn<>(title);
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

    private void setupTextColumnWidthListener(TableColumn<Savings, ?> column, TextField searchField) {
        column.widthProperty().addListener((observable, oldValue, newValue) -> searchField.setPrefWidth(newValue.doubleValue()));
    }

    private void setupSearchFields() {
        manageSavings_savingsIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_customerIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_customerNameSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_statusSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_dateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_principalSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_mandatorySearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_capitalSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_voluntarySearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_othersSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_totalSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        manageSavings_notesSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void filterTable() {
        // Search Field
        String savingsIDFilter = manageSavings_savingsIDSearch.getText().toLowerCase();
        String customerIDFilter = manageSavings_customerIDSearch.getText().toLowerCase();
        String customerNameFilter = manageSavings_customerNameSearch.getText().toLowerCase();
        String statusFilter = manageSavings_statusSearch.getText().toLowerCase();
        String dateFilter = manageSavings_dateSearch.getText().toLowerCase();
        String principalFilter = manageSavings_principalSearch.getText().toLowerCase();
        String mandatoryFilter = manageSavings_mandatorySearch.getText().toLowerCase();
        String capitalFilter = manageSavings_capitalSearch.getText().toLowerCase();
        String voluntaryFilter = manageSavings_voluntarySearch.getText().toLowerCase();
        String othersFilter = manageSavings_othersSearch.getText().toLowerCase();
        String totalFilter = manageSavings_totalSearch.getText().toLowerCase();
        String notesFilter = manageSavings_notesSearch.getText().toLowerCase();

        // Toggle Buttons
        boolean activeSelected = manageSavings_activeToggle.isSelected();
        boolean redeemedSelected = manageSavings_redeemedToggle.isSelected();

        // Get selected values from ChoiceBoxes
        Customer selectedCustomer = manageSavings_customerChoiceBox.getValue();
        String selectedDateFilter = manageSavings_dateFilterChoiceBox.getValue();
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        startDate = switch (selectedDateFilter) {
            case "Last 30 Days" -> today.minusDays(30);
            case "Last Year" -> today.minusYears(1);
            case "Today" -> today; // Only show items from today
            case null, default -> null; // No filter applied
        };

        manageSavings_savingsTable.setItems(savingsList.filtered(savings -> {
            // Filters
            boolean matchesSavingsID = isFilterMatch(savings.getSavingsID(), savingsIDFilter);
            boolean matchesCustomerID = isFilterMatch(savings.getCustomerID(), customerIDFilter);
            boolean matchesCustomerName = isFilterMatch(savings.getCustomerName(), customerNameFilter);
            boolean matchesStatus = isFilterMatch(savings.getStatus(), statusFilter);
            boolean matchesDate = isFilterMatch(savings.getDate(), dateFilter);
            boolean matchesPrincipal = isFilterMatch(savings.getPrincipal(), principalFilter);
            boolean matchesMandatory = isFilterMatch(savings.getMandatory(), mandatoryFilter);
            boolean matchesCapital = isFilterMatch(savings.getCapital(), capitalFilter);
            boolean matchesVoluntary = isFilterMatch(savings.getVoluntary(), voluntaryFilter);
            boolean matchesOthers = isFilterMatch(savings.getOthers(), othersFilter);
            boolean matchesTotal = isFilterMatch(savings.getTotal(), totalFilter);
            boolean matchesNotes = isFilterMatch(savings.getNotes(), notesFilter);

            // Status Logic (Toggle Buttons)
            boolean matchesStatusToggle = true;
            if (activeSelected) {
                matchesStatusToggle = savings.getStatus() != null
                        && savings.getStatus().contains("Active");
            } else if (redeemedSelected) {
                matchesStatusToggle = savings.getStatus() != null
                        && savings.getStatus().contains("Redeemed");
            }

            // Match selected customer
            boolean matchesCustomerSelection = selectedCustomer == null ||
                    selectedCustomer.getCustomerID().equals("ALL") ||
                    selectedCustomer.getCustomerID().equals(savings.getCustomerID());

            // Match selected date
            boolean matchesDateSelection = true;
            if (startDate != null) {
                try {
                    LocalDate date = LocalDate.parse(savings.getDate(), formatter);
                    matchesDateSelection = !date.isBefore(startDate);
                } catch (Exception e) {
                    matchesDateSelection = false;
                }
            }

            // Combine All Conditions
            return matchesSavingsID && matchesCustomerID && matchesCustomerName && matchesStatus && matchesDate &&
                    matchesPrincipal && matchesMandatory && matchesCapital && matchesVoluntary && matchesOthers &&
                    matchesTotal && matchesNotes && matchesStatusToggle && matchesCustomerSelection && matchesDateSelection;

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
                manageSavings_savingsIDSearch,manageSavings_customerIDSearch,manageSavings_customerNameSearch,manageSavings_statusSearch,
                manageSavings_dateSearch,manageSavings_principalSearch,manageSavings_mandatorySearch,manageSavings_capitalSearch,
                manageSavings_voluntarySearch,manageSavings_othersSearch,manageSavings_totalSearch,manageSavings_notesSearch
        );
        textFields.forEach(TextField::clear);

        // Clear Choice Box
        manageSavings_customerChoiceBox.setValue(null);
        manageSavings_dateFilterChoiceBox.setValue(null);

        // Clear all toggle buttons
        ObservableList<ToggleButton> toggleButtons = FXCollections.observableArrayList(manageSavings_activeToggle,manageSavings_redeemedToggle);
        toggleButtons.forEach(toggle -> toggle.setSelected(false));

        // Reset the table view to show all items
        manageSavings_savingsTable.setItems(savingsList);

        // Clear any selected items in the table
        manageSavings_savingsTable.getSelectionModel().clearSelection();

        // Update the interventions found label
        updateItemsFoundLabel();
    }

    private void updateItemsFoundLabel() {
        int filteredSize = manageSavings_savingsTable.getItems().size();
        manageSavings_savingsFoundLabel.setText(filteredSize + " " + foundLabel);

        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
        double totalSum = manageSavings_savingsTable.getItems().stream()
                .mapToDouble(savings -> {
                    try {
                        return Double.parseDouble(savings.getTotal().replace(",", ""));
                    } catch (NullPointerException e) {
                        return 0;
                    }
                })
                .sum();

        manageSavings_totalLabel.setText(totalLabel + decimalFormat.format(totalSum));
    }
}
