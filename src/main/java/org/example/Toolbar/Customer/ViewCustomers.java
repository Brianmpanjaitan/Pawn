package org.example.Toolbar.Customer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.Classes.Customer;
import org.example.Classes.ItemCategory;
import org.example.Classes.User;
import org.example.Logic;
import org.example.Main.MainMenu;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ViewCustomers {
    private static final Logger logger = Logger.getLogger(ViewCustomers.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String customerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private final ObservableList<Customer> customerListChoiceBox = FXCollections.observableArrayList();
    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();

    @FXML VBox viewCustomer_VBoxContent;
    @FXML Label viewCustomer_customersFoundLabel;
    @FXML Button viewCustomer_printButton,viewCustomer_clearButton;
    @FXML ChoiceBox<Customer> viewCustomer_customerChoiceBox;
    @FXML TableView<Customer> viewCustomer_customerTable;
    @FXML TextField viewCustomer_customerIDSearch,viewCustomer_customerNameSearch,viewCustomer_phoneNumberSearch,
            viewCustomer_addressSearch,viewCustomer_genderSearch,viewCustomer_dobSearch,viewCustomer_statusSearch,
            viewCustomer_entryDateSearch,viewCustomer_exitDateSearch,viewCustomer_notesSearch;

    Logic logic = new Logic();
    MainMenu mainMenu = new MainMenu();
    private static String foundLabel = "";

    private User currentUser;
    private String setLanguage;

    public void setUser(User user) {
        this.currentUser = user;
        foundLabel = viewCustomer_customersFoundLabel.getText();
    }
    public void setLanguage(String language) { this.setLanguage = language; }

    public void initializeViewCustomers() {
        Locale locale = "id".equals(setLanguage) ? new Locale("id") : new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-"+setLanguage+".messages", locale);

        setupButtons();
        getCustomerData(bundle);
        setupSearchFields();
        updateCustomersFoundLabel();
    }

    private void setupButtons() {
        viewCustomer_clearButton.setOnAction(_ -> clearFilters());
        viewCustomer_printButton.setOnAction(_ -> logic.printTableInformation(viewCustomer_VBoxContent, "ViewCustomers"));
    }

    private void getCustomerData(ResourceBundle bundle) {
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
                customerListChoiceBox.add(customer);
            }
            viewCustomer_customerTable.setItems(customerList);   // Set Table
            viewCustomer_customerChoiceBox.setItems(customerListChoiceBox);  // Set Choicebox
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error populating customer list", e);
        }
        setupCustomerTableColumns(bundle);
        defineChoiceBoxDisplay(bundle);
        viewCustomer_customerChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void defineChoiceBoxDisplay(ResourceBundle bundle) {
        Customer selectAllCustomers = new Customer("ALL", bundle.getString("selectAll"),
                "", "", "", "", "", "", "", "");
        customerListChoiceBox.addFirst(selectAllCustomers);

        // Display the customerNames instead of the object representation
        viewCustomer_customerChoiceBox.setConverter(new StringConverter<Customer>() {
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

    private void setupCustomerTableColumns(ResourceBundle bundle) {
        TableColumn<Customer, String> customerIDColumn = createColumn(bundle.getString("createCustomer.name"), "customerIDProperty");
        TableColumn<Customer, String> customerNameColumn = createColumn(bundle.getString("addItem.customerID"), "customerNameProperty");
        TableColumn<Customer, String> phoneNumberColumn = createColumn(bundle.getString("createCustomer.phone"), "phoneNumberProperty");
        TableColumn<Customer, String> addressColumn = createColumn(bundle.getString("createCustomer.address"), "addressProperty");
        TableColumn<Customer, String> genderColumn = createColumn(bundle.getString("createCustomer.gender"), "genderProperty");
        TableColumn<Customer, String> dobColumn = createColumn(bundle.getString("createCustomer.dob"), "dobProperty");
        TableColumn<Customer, String> statusColumn = createColumn(bundle.getString("createCustomer.status"), "statusProperty");
        TableColumn<Customer, String> entryDateColumn = createColumn(bundle.getString("createCustomer.entryDate"), "entryDateProperty");
        TableColumn<Customer, String> exitDateColumn = createColumn(bundle.getString("createCustomer.exitDate"), "exitDateProperty");
        TableColumn<Customer, String> notesColumn = createColumn(bundle.getString("createCustomer.notes"), "notesProperty");

        // Add all columns to the table
        viewCustomer_customerTable.getColumns().addAll(
                customerIDColumn,customerNameColumn,phoneNumberColumn,addressColumn,
                genderColumn,dobColumn,statusColumn,entryDateColumn,exitDateColumn,notesColumn
        );

        // Batch setup for column width listeners
        setupTextColumnWidthListener(customerIDColumn, viewCustomer_customerIDSearch);
        setupTextColumnWidthListener(customerNameColumn, viewCustomer_customerNameSearch);
        setupTextColumnWidthListener(phoneNumberColumn, viewCustomer_phoneNumberSearch);
        setupTextColumnWidthListener(addressColumn, viewCustomer_addressSearch);
        setupTextColumnWidthListener(genderColumn, viewCustomer_genderSearch);
        setupTextColumnWidthListener(dobColumn, viewCustomer_dobSearch);
        setupTextColumnWidthListener(statusColumn, viewCustomer_statusSearch);
        setupTextColumnWidthListener(entryDateColumn, viewCustomer_entryDateSearch);
        setupTextColumnWidthListener(exitDateColumn, viewCustomer_exitDateSearch);
        setupTextColumnWidthListener(notesColumn, viewCustomer_notesSearch);
    }

    private <T> TableColumn<Customer, T> createColumn(String title, String property) {
        TableColumn<Customer, T> column = new TableColumn<>(title);
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

    private void setupTextColumnWidthListener(TableColumn<Customer, ?> column, TextField searchField) {
        column.widthProperty().addListener((observable, oldValue, newValue) -> searchField.setPrefWidth(newValue.doubleValue()));
    }

    private void setupSearchFields() {
        viewCustomer_customerIDSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_customerNameSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_phoneNumberSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_addressSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_genderSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_dobSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_statusSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_entryDateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_exitDateSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        viewCustomer_notesSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    private void filterTable() {
        // Search Field
        String customerIDFilter = viewCustomer_customerIDSearch.getText().toLowerCase();
        String customerNameFilter = viewCustomer_customerNameSearch.getText().toLowerCase();
        String phoneNumberFilter = viewCustomer_phoneNumberSearch.getText().toLowerCase();
        String addressFilter = viewCustomer_addressSearch.getText().toLowerCase();
        String genderFilter = viewCustomer_genderSearch.getText().toLowerCase();
        String dobFilter = viewCustomer_dobSearch.getText().toLowerCase();
        String statusFilter = viewCustomer_statusSearch.getText().toLowerCase();
        String entryDateFilter = viewCustomer_entryDateSearch.getText().toLowerCase();
        String exitDateFilter = viewCustomer_exitDateSearch.getText().toLowerCase();
        String notesFilter = viewCustomer_notesSearch.getText().toLowerCase();

        // Get selected values from ChoiceBoxes
        Customer selectedCustomer = viewCustomer_customerChoiceBox.getValue();

        viewCustomer_customerTable.setItems(customerList.filtered(customer -> {
            // Filters
            boolean matchesCustomerID = isFilterMatch(customer.getCustomerID(), customerIDFilter);
            boolean matchesCustomerName = isFilterMatch(customer.getCustomerName(), customerNameFilter);
            boolean matchesPhoneNumber = isFilterMatch(customer.getPhoneNumber(), phoneNumberFilter);
            boolean matchesAddress = isFilterMatch(customer.getAddress(), addressFilter);
            boolean matchesGender = isFilterMatch(customer.getGender(), genderFilter);
            boolean matchesDOB = isFilterMatch(customer.getDOB(), dobFilter);
            boolean matchesStatus = isFilterMatch(customer.getStatus(), statusFilter);
            boolean matchesEntryDate = isFilterMatch(customer.getEntryDate(), entryDateFilter);
            boolean matchesExitDate = isFilterMatch(customer.getExitDate(), exitDateFilter);
            boolean matchesNotes = isFilterMatch(customer.getNotes(), notesFilter);

            // Match selected customer
            boolean matchesCustomerSelection = selectedCustomer == null ||
                    selectedCustomer.getCustomerID().equals("ALL") ||
                    selectedCustomer.getCustomerID().equals(customer.getCustomerID());

            // Combine All Conditions
            return matchesCustomerID && matchesCustomerName && matchesPhoneNumber && matchesAddress && matchesGender &&
                    matchesDOB && matchesStatus && matchesEntryDate && matchesExitDate && matchesNotes && matchesCustomerSelection;

        }));
        updateCustomersFoundLabel();
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
                viewCustomer_customerIDSearch,viewCustomer_customerNameSearch,viewCustomer_phoneNumberSearch,
                viewCustomer_addressSearch,viewCustomer_genderSearch,viewCustomer_dobSearch,viewCustomer_statusSearch,
                viewCustomer_entryDateSearch,viewCustomer_exitDateSearch,viewCustomer_notesSearch
        );
        textFields.forEach(TextField::clear);

        // Clear Choice Box
        viewCustomer_customerChoiceBox.setValue(null);

        // Reset the table view to show all items
        viewCustomer_customerTable.setItems(customerList);

        // Clear any selected items in the table
        viewCustomer_customerTable.getSelectionModel().clearSelection();

        // Update the interventions found label
        updateCustomersFoundLabel();
    }

    private void updateCustomersFoundLabel() {
        int filteredSize = viewCustomer_customerTable.getItems().size();
        viewCustomer_customersFoundLabel.setText(foundLabel + filteredSize);}
}
