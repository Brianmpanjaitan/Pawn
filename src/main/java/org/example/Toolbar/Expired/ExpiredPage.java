package org.example.Toolbar.Expired;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import org.example.Classes.Customer;
import org.example.Classes.Item;
import org.example.Classes.User;
import org.example.Logic;
import org.example.Main.MainMenu;

import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ExpiredPage {
    private static final Logger logger = Logger.getLogger(ExpiredPage.class.getName());
    private static final String url = "jdbc:postgresql://localhost:5432/PDB";
    private static final String sqluser = "postgres";
    private static final String sqlpassword = "hello123";

    private static final String getCustomerQuery = "SELECT * FROM \"Pawn\".\"Customer\"";
    private static final String getExpiredQuery =
            "SELECT \"expiredID\",\"date\",\"collateral\",\"sale\",\"profit\",\"itemID\",\"itemName\",\"customerID\",\"customerName\",\"notes\" " +
            "FROM \"Pawn\".\"Expired\" " +
            "WHERE \"itemID\" = ?";
    private static final String expiredItemQuery =
        """
        SELECT i."itemID", i."itemName", i."customerID", i."categoryID", ic."categoryName", i."status",
               i."pawnDate", i."expireDate", i."price", i."tariff", i."admin", i."provision", i."storageFee",
               i."damageFee", i."total", i."additionalNotes"
        FROM "Pawn"."Item" i
        JOIN "Pawn"."Item_Category" ic ON i."categoryID" = ic."categoryID"
        WHERE i."status" = 'Expired'
        """;
    private static final String insertExpiredEntryQuery = "INSERT INTO \"Pawn\".\"Expired\"(" +
            "\"expiredID\",\"date\",\"collateral\",\"sale\",\"profit\",\"itemID\",\"itemName\",\"customerID\",\"customerName\",\"notes\") " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String updateExpiredEntryQuery = "UPDATE \"Pawn\".\"Expired\" " +
            "SET \"date\"=?,\"collateral\"=?,\"sale\"=?,\"profit\"=?,\"notes\"=? " +
            "WHERE \"expiredID\"=?";
    private static final String getExpiredID = "SELECT \"expiredID\" FROM \"Pawn\".\"Expired\" WHERE \"expiredID\" LIKE ?";
    private static final String getCustomerName = "SELECT \"customerName\" FROM \"Pawn\".\"Customer\" WHERE \"customerID\" = ?";

    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final ObservableList<Item> expiredItemList = FXCollections.observableArrayList();

    private User currentUser;
    private String currentExpiredID;
    private String setLanguage;

    Logic logic = new Logic();
    MainMenu mainMenu = new MainMenu();

    @FXML private TextField expiredPage_itemSearch,expiredPage_customerIDField,expiredPage_customerNameField,
            expiredPage_itemIDField,expiredPage_itemNameField,expiredPage_notesField,expiredPage_collateralField,
            expiredPage_saleField,expiredPage_profitField;
    @FXML private ListView<Item> expiredPage_itemListView;
    @FXML private Label expiredPage_expiredLabel;
    @FXML private Button expiredPage_applyButton, expiredPage_deleteButton;
    @FXML private DatePicker expiredPage_datePicker;

    public void setUser(User user) {this.currentUser = user;}
    public void setLanguage(String language) {this.setLanguage = language;}

    public void initializeExpiredPage() {
        Locale locale = "id".equals(setLanguage) ? new Locale("id") : new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-"+setLanguage+".messages", locale);

        getCustomers();
        getExpiredItems();
        setupTextFields();
        setupListeners(bundle);
        setupFieldListeners();
        setupButtons();
        disableFields();
    }

    public void getCustomers() {
        customerList.clear();
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(getCustomerQuery)) {

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
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading customer data", e);
        }
    }

    private void getExpiredItems() {
        expiredItemList.clear();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(expiredItemQuery)) {

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
                expiredItemList.add(item);
            }

            Platform.runLater(() -> {
                // Sort the itemList by itemID (first by letter and year, then by unique number)
                expiredItemList.sort(Comparator
                        .comparing((Item item) -> item.getItemID().substring(0, 3)) // Compare by "A25" (letter + year suffix)
                        .thenComparing(item -> {
                            // Extract and parse the unique numeric part after the hyphen
                            String numericPart = item.getItemID().substring(item.getItemID().indexOf('-') + 1);
                            return Integer.parseInt(numericPart);
                        }));

                // Bind the sorted list to the ListView
                expiredPage_itemListView.setItems(FXCollections.observableArrayList(expiredItemList));

                // Define how to display items in the ListView
                expiredPage_itemListView.setCellFactory(listView -> new ListCell<>() {
                    @Override
                    protected void updateItem(Item item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getItemID() + ": " + item.getItemName() + " - " + item.getCategoryName());
                        }
                    }
                });
            });

            // Double-click an item
            expiredPage_itemListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                    Item selectedItem = expiredPage_itemListView.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        // Retrieve the customer based on customerID from the selected item
                        Customer customer = getCustomerByID(selectedItem.getCustomerID());

                        if (customer != null) {
                            mainMenu.showItemInformation(selectedItem, customer, false, currentUser);
                        } else {
                            showAlertBox("Error", "Customer not found.");
                        }
                    }
                }
            });

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading customer data", e);
        }
    }

    private void setupTextFields() {
        expiredPage_profitField.setDisable(true);
        logic.setupNumericField(expiredPage_collateralField);
        logic.setupNumericField(expiredPage_saleField);

        expiredPage_collateralField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateProfit(expiredPage_collateralField,expiredPage_saleField,expiredPage_profitField));
        expiredPage_saleField.textProperty().addListener((observable, oldValue, newValue) ->
                logic.calculateProfit(expiredPage_collateralField,expiredPage_saleField,expiredPage_profitField));
    }

    private void setupListeners(ResourceBundle bundle) {
        expiredPage_itemListView.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, newItem) -> {
            if (newItem != null) {
                enableFields();
                populateExpiredEntry(newItem, bundle);
            } else {
                disableFields();
                setCurrentExpiredID(null);
                expiredPage_itemListView.getItems().clear();
            }
        });

        expiredPage_itemSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            // Filter the customer list based on the text field input
            String filter = newValue.toLowerCase();

            ObservableList<Item> filteredList = expiredItemList.stream()
                    .filter(item -> item.getItemName().toLowerCase().contains(filter) ||
                            item.getItemID().toLowerCase().contains(filter))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            expiredPage_itemListView.setItems(filteredList);
        });
    }

    private void setCurrentExpiredID(String expiredID) {
        currentExpiredID = expiredID;

        expiredPage_deleteButton.setDisable(currentExpiredID == null || currentExpiredID.isEmpty());
    }


    private void setupFieldListeners() {
        ChangeListener<Object> fieldListener = (ObservableValue<?> observable, Object oldValue, Object newValue) -> checkAllFieldsFilled();

        expiredPage_datePicker.valueProperty().addListener(fieldListener);
        expiredPage_collateralField.textProperty().addListener(fieldListener);
        expiredPage_saleField.textProperty().addListener(fieldListener);
        expiredPage_profitField.textProperty().addListener(fieldListener);
    }

    private void checkAllFieldsFilled() {
        boolean allFieldsFilled =
                expiredPage_datePicker.getValue() != null &&
                        isValidFormattedNumber(expiredPage_collateralField.getText()) &&
                        isValidFormattedNumber(expiredPage_saleField.getText()) &&
                        isValidFormattedNumber(expiredPage_profitField.getText());

        expiredPage_applyButton.setDisable(!allFieldsFilled);
    }

    private boolean isValidFormattedNumber(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            // Remove commas (thousands separators) and attempt to parse the number
            String cleanedText = text.replace(",", "");
            Float.parseFloat(cleanedText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void populateExpiredEntry(Item expiredItem, ResourceBundle bundle) {
        clearFields();

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(getExpiredQuery)) {

            ps.setString(1, expiredItem.getItemID());
            ResultSet rs = ps.executeQuery();

            // Expired entry does exist
            if (rs.next()) {
                setCurrentExpiredID(rs.getString("expiredID"));
                expiredPage_customerIDField.setText(rs.getString("customerID"));
                expiredPage_customerNameField.setText(rs.getString("customerName"));
                expiredPage_itemIDField.setText(rs.getString("itemID"));
                expiredPage_itemNameField.setText(rs.getString("itemName"));
                expiredPage_notesField.setText(rs.getString("notes"));

                String dateStr = rs.getString("date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    LocalDate date = LocalDate.parse(dateStr);
                    expiredPage_datePicker.setValue(date);
                } else {
                    expiredPage_datePicker.setValue(null);
                }

                expiredPage_collateralField.setText(rs.getString("collateral"));
                expiredPage_saleField.setText(rs.getString("sale"));
                expiredPage_profitField.setText(rs.getString("profit"));
                expiredPage_expiredLabel.setText(bundle.getString("expiredPage.expiredUpdate"));
            } else {
                // Expired entry does exist
                try (Connection connection_customerName = DriverManager.getConnection(url, sqluser, sqlpassword);
                     PreparedStatement ps_customerName = connection_customerName.prepareStatement(getCustomerName)) {

                    ps_customerName.setString(1, expiredItem.getCustomerID());
                    ResultSet rs_customerName = ps_customerName.executeQuery();

                    if (rs_customerName.next()) {
                        expiredPage_customerNameField.setText(rs_customerName.getString("customerName"));
                    }

                    setCurrentExpiredID(null);  // If expired entry doesn't exist, reset expiredID to null
                    expiredPage_customerIDField.setText(expiredItem.getCustomerID());
                    expiredPage_itemIDField.setText(expiredItem.getItemID());
                    expiredPage_itemNameField.setText(expiredItem.getItemName());
                    expiredPage_collateralField.setText(expiredItem.getPrice());
                    expiredPage_expiredLabel.setText(bundle.getString("expiredPage.expiredNew"));

                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error retrieving item details", e);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving item details", e);
        }
    }

    private void clearFields() {
        expiredPage_customerIDField.clear();
        expiredPage_itemIDField.clear();
        expiredPage_itemNameField.clear();
        expiredPage_notesField.clear();
        expiredPage_datePicker.setValue(null);
        expiredPage_collateralField.clear();
        expiredPage_saleField.clear();
        expiredPage_profitField.clear();
    }

    private void setupButtons() {
        String delete_message = "Are you sure you want to delete this expired information?";

        expiredPage_applyButton.setOnAction(event -> handleApplyButton());
        expiredPage_deleteButton.setOnAction(event -> {
            if (confirmAction("Delete Expired", delete_message, "This action cannot be undone.")) {
                deleteExpired();
            }
        });
    }

    private void handleApplyButton() {
        Item selectedItem = expiredPage_itemListView.getSelectionModel().getSelectedItem();
        boolean exists = checkExpiredExists();
        if (exists) {
            updateExpiredEntry();
        } else {
            insertExpiredEntry(selectedItem);
        }
    }

    private boolean checkExpiredExists() {
        Item selectedItem = expiredPage_itemListView.getSelectionModel().getSelectedItem();

        String EXPIRED_EXISTS_QUERY = "SELECT COUNT(*) FROM \"Pawn\".\"Expired\" WHERE \"itemID\" = ?";
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement statement = connection.prepareStatement(EXPIRED_EXISTS_QUERY)) {
            statement.setString(1,selectedItem.getItemID());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving expired item", e);
        }
        return false;
    }

    private void updateExpiredEntry() {
        String date = expiredPage_datePicker.getValue() != null ? expiredPage_datePicker.getValue().toString() : null;

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(updateExpiredEntryQuery)) {

            ps.setString(1, date);
            ps.setString(2, expiredPage_collateralField.getText());
            ps.setString(3, expiredPage_saleField.getText());
            ps.setString(4, expiredPage_profitField.getText());
            ps.setString(5, expiredPage_notesField.getText());
            ps.setString(6, currentExpiredID);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        showAlertBox("Expired Form", "Updated");
    }

    private void insertExpiredEntry(Item expiredItem) {
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(insertExpiredEntryQuery)) {

            ps.setString(1, getNextExpiredID(expiredPage_datePicker.getValue()));
            ps.setString(2, expiredPage_datePicker.getValue().toString());
            ps.setString(3, expiredPage_collateralField.getText());
            ps.setString(4, expiredPage_saleField.getText());
            ps.setString(5, expiredPage_profitField.getText());
            ps.setString(6, expiredItem.getItemID());
            ps.setString(7, expiredItem.getItemName());
            ps.setString(8, expiredItem.getCustomerID());
            ps.setString(9, expiredPage_customerNameField.getText());
            ps.setString(10, expiredPage_notesField.getText());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        showAlertBox("Expired Form", "Complete");
    }

    private String getNextExpiredID(LocalDate date) {
        String nextExpiredID = null;
        char monthLetter = (char) ('A' + date.getMonthValue() - 1);
        String yearSuffix = String.valueOf(date.getYear()).substring(2);

        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement ps = connection.prepareStatement(getExpiredID)) {

            ps.setString(1, monthLetter + yearSuffix + "-E%");

            ResultSet rs = ps.executeQuery();
            int maxNumber = 0;

            // Iterate through the results and find the max number
            while (rs.next()) {
                String expiredID = rs.getString("expiredID");
                if (expiredID != null && expiredID.contains("-E")) {
                    String[] parts = expiredID.split("-E");
                    if (parts.length == 2) {
                        try {
                            int number = Integer.parseInt(parts[1]);
                            maxNumber = Math.max(maxNumber, number);
                        } catch (NumberFormatException e) {
                            // Ignore invalid numbers
                            e.printStackTrace();
                        }
                    }
                }
            }
            nextExpiredID = monthLetter + yearSuffix + "-E" + (maxNumber + 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nextExpiredID;
    }

    private void deleteExpired() {
        String stored_procedure = "CALL \"Pawn\".delete_expired(?);";
        try (Connection connection = DriverManager.getConnection(url, sqluser, sqlpassword);
             PreparedStatement preparedProcedure = connection.prepareStatement(stored_procedure)) {

            preparedProcedure.setString(1, currentExpiredID);
            preparedProcedure.execute();

        } catch (SQLException e) {
            showAlertBox("Error","An error occurred while deleting the expired form.");
            logger.log(Level.SEVERE, "Error deleting expired: " + e.getMessage(), e);
        }
        showAlertBox("Expired Form", "Delete Successful");
    }

    private void disableFields() {
        expiredPage_customerIDField.setDisable(true);
        expiredPage_customerNameField.setDisable(true);
        expiredPage_itemIDField.setDisable(true);
        expiredPage_itemNameField.setDisable(true);
        expiredPage_notesField.setDisable(true);
        expiredPage_datePicker.setDisable(true);
        expiredPage_collateralField.setDisable(true);
        expiredPage_saleField.setDisable(true);
        expiredPage_profitField.setDisable(true);
        expiredPage_applyButton.setDisable(true);
        expiredPage_deleteButton.setDisable(true);
    }

    private void enableFields() {
        expiredPage_notesField.setDisable(false);
        expiredPage_datePicker.setDisable(false);
        expiredPage_collateralField.setDisable(false);
        expiredPage_saleField.setDisable(false);
    }

    private Customer getCustomerByID(String customerID) {
        for (Customer customer : customerList) {
            if (customer.getCustomerID().equals(customerID)) {
                return customer;
            }
        }
        return null;
    }

    private boolean confirmAction(String title, String message, String contextText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, contextText, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlertBox(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
