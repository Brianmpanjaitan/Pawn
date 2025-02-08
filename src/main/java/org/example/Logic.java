package org.example;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.example.Classes.Item;
import org.example.Main.ItemInformation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logic {
    private static final Logger logger = Logger.getLogger(Logic.class.getName());

    public void setupNumericField(TextField textField) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0"); // Format for integers with thousands separator

        // Listener for text input
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Remove non-digit characters
            String unformatted = newValue.replaceAll("[^\\d]", "");

            if (unformatted.isEmpty()) {
                textField.setText("");
                return;
            }

            // Parse and reformat the number with thousands separator
            try {
                long parsedNumber = Long.parseLong(unformatted);
                String formattedText = decimalFormat.format(parsedNumber);
                // Temporarily remove the listener to avoid infinite loop
                textField.textProperty().removeListener((obs, o, n) -> {});
                textField.setText(formattedText);
                textField.positionCaret(formattedText.length()); // Keep the caret at the end
                textField.textProperty().addListener((obs, o, n) -> {}); // Re-add the listener
            } catch (NumberFormatException e) {
                // If parsing fails (unlikely for valid integers), revert to old value
                textField.setText(oldValue);
            }
        });
    }

    public void setupPercentage(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Allow only valid percentages (0-100)
            if (!newValue.matches("\\d{0,2}(\\.\\d{0,2})?") || (newValue.matches("\\d+\\.?\\d*") && Double.parseDouble(newValue) > 100)) {
                textField.setText(oldValue);
            }
        });
    }

    public void calculateTotal(TextField priceField,TextField tariffField,TextField adminField,TextField prufisiField,
                               TextField storageField,TextField damageField,TextField totalField) {
        ChangeListener<String> listener = (observable, oldValue, newValue) -> {
            try {
                // Parse values from the text fields, remove commas to avoid errors
                double price = priceField.getText().isEmpty() ? 0 : Double.parseDouble(priceField.getText().replace(",", ""));
                double tariff = tariffField.getText().isEmpty() ? 0 : Double.parseDouble(tariffField.getText().replace(",", ""));
                double prufisi = prufisiField.getText().isEmpty() ? 0 : Double.parseDouble(prufisiField.getText().replace(",", ""));
                double storage = storageField.getText().isEmpty() ? 0 : Double.parseDouble(storageField.getText().replace(",", ""));
                double damage = damageField.getText().isEmpty() ? 0 : Double.parseDouble(damageField.getText().replace(",", ""));
                double admin = adminField.getText().isEmpty() ? 0 : Double.parseDouble(adminField.getText().replace(",", ""));

                // Calculate the total
                double total = price + (price * (admin / 100)) + (price * (tariff / 100)) +
                        (price * (prufisi / 100)) + (price * (storage / 100)) + (price * (damage / 100));

                // Format and set the total field with thousands separators
                DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
                totalField.setText(decimalFormat.format(total));
            } catch (NumberFormatException e) {
                // Handle invalid input gracefully
                totalField.setText("");
            }
        };

        // Add the listener to the relevant fields
        priceField.textProperty().addListener(listener);
        tariffField.textProperty().addListener(listener);
        adminField.textProperty().addListener(listener);
        prufisiField.textProperty().addListener(listener);
        storageField.textProperty().addListener(listener);
        damageField.textProperty().addListener(listener);
    }

    public void calculateSavings(TextField principalField,TextField mandatoryField,TextField capitalField,TextField voluntaryField,TextField otherField,TextField totalField) {
        ChangeListener<String> listener = (observable, oldValue, newValue) -> {
            try {
                // Parse values from the text fields, remove commas to avoid errors
                double principal = principalField.getText().isEmpty() ? 0 : Double.parseDouble(principalField.getText().replace(",", ""));
                double mandatory = mandatoryField.getText().isEmpty() ? 0 : Double.parseDouble(mandatoryField.getText().replace(",", ""));
                double capital = capitalField.getText().isEmpty() ? 0 : Double.parseDouble(capitalField.getText().replace(",", ""));
                double voluntary = voluntaryField.getText().isEmpty() ? 0 : Double.parseDouble(voluntaryField.getText().replace(",", ""));
                double other = otherField.getText().isEmpty() ? 0 : Double.parseDouble(otherField.getText().replace(",", ""));

                // Calculate the total
                double total = principal + mandatory + capital + voluntary + other;

                // Format and set the total field with thousands separators
                DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
                totalField.setText(decimalFormat.format(total));

            } catch (NumberFormatException e) {
                // Handle invalid input gracefully
                totalField.setText("");
            }
        };

        principalField.textProperty().addListener(listener);
        mandatoryField.textProperty().addListener(listener);
        capitalField.textProperty().addListener(listener);
        voluntaryField.textProperty().addListener(listener);
        otherField.textProperty().addListener(listener);
    }

    public void calculateProfit(TextField collateralField,TextField saleField,TextField profitField) {
        ChangeListener<String> listener = (observable, oldValue, newValue) -> {
            try {
                // Parse values from the text fields, remove commas to avoid errors
                double collateral = collateralField.getText().isEmpty() ? 0 : Double.parseDouble(collateralField.getText().replace(",", ""));
                double sale = saleField.getText().isEmpty() ? 0 : Double.parseDouble(saleField.getText().replace(",", ""));

                // Calculate the total
                double profit = sale - collateral;

                // Format and set the total field with thousands separators
                DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
                profitField.setText(decimalFormat.format(profit));

            } catch (NumberFormatException e) {
                // Handle invalid input gracefully
                profitField.setText("");
            }
        };

        collateralField.textProperty().addListener(listener);
        saleField.textProperty().addListener(listener);
    }

    public void printTableInformation(VBox contentVBox, String managePage) {
        WritableImage snapshot = contentVBox.snapshot(null, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

        if (bufferedImage == null) {
            logger.severe("Failed to capture contentVBox image.");
            return;
        }

        try {
            // Create a new PDF document
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(); // Default A4 page, but we will resize it
            document.addPage(page);

            // Save image as a temporary file
            File tempImageFile = new File("table_temp.png");
            ImageIO.write(bufferedImage, "png", tempImageFile);

            // Load the image into PDFBox
            PDImageXObject pdImage = PDImageXObject.createFromFile(tempImageFile.getAbsolutePath(), document);

            // Scale image to fit receipt size
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            // Scale image to fit within A4
            float imageWidth = pdImage.getWidth();
            float imageHeight = pdImage.getHeight();
            float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
            float newWidth = imageWidth * scale;
            float newHeight = imageHeight * scale;

            // Center the image on the A4 page
            float xPosition = (pageWidth - newWidth) / 2;
            float yPosition = (pageHeight - newHeight) / 2;

            // Create PDF content stream to add image
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, xPosition, yPosition, newWidth, newHeight);
            }

            // Save the PDF
            String desktopPath = System.getProperty("user.home") + "/Desktop/Pawn Prints/" + managePage;
            File receiptFolder = new File(desktopPath);
            if (!receiptFolder.exists()) {
                receiptFolder.mkdirs();
            }

            String pdfPath = getUniqueFilePath(desktopPath, managePage + "_Table");
            document.save(pdfPath);
            document.close();
            showAlertBox("Table has been printed.");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating PDF", e);
        }
    }

    public void printItemInformation(VBox contentVBox, String itemID) {
        WritableImage snapshot = contentVBox.snapshot(null, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

        if (bufferedImage == null) {
            logger.severe("Failed to capture contentVBox image.");
            return;
        }

        try {
            // Create a new PDF document
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);

            // Save image as a temporary file
            File tempImageFile = new File("receipt_temp.png");
            ImageIO.write(bufferedImage, "png", tempImageFile);

            // Load the image into PDFBox
            PDImageXObject pdImage = PDImageXObject.createFromFile(tempImageFile.getAbsolutePath(), document);

            // Scale image to fit receipt size
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            // Scale image to fit within A4
            float imageWidth = pdImage.getWidth();
            float imageHeight = pdImage.getHeight();
            float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight); // Keep aspect ratio
            float newWidth = imageWidth * scale;
            float newHeight = imageHeight * scale;

            // Center the image on the A4 page
            float xPosition = (pageWidth - newWidth) / 2;
            float yPosition = (pageHeight - newHeight) / 2;

            // Create PDF content stream to add image
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, xPosition, yPosition, newWidth, newHeight);
            }

            // Save the PDF
            String desktopPath = System.getProperty("user.home") + "/Desktop/Pawn Prints/Item Information";
            File receiptFolder = new File(desktopPath);
            if (!receiptFolder.exists()) {
                receiptFolder.mkdirs();
            }

            String pdfPath = getUniqueFilePath(desktopPath, itemID + "_receipt");
            document.save(pdfPath);
            document.close();
            showAlertBox("Item has been printed.");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating PDF", e);
        }
    }

    public void printReceipt(VBox contentVBox, String itemID, String addPage) {
        WritableImage snapshot = contentVBox.snapshot(null, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

        if (bufferedImage == null) {
            logger.severe("Failed to capture contentVBox image.");
            return;
        }

        try {
            // Create a new PDF document
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(); // Default A4 page, but we will resize it
            document.addPage(page);

            // Save image as a temporary file
            File tempImageFile = new File("receipt_temp.png");
            ImageIO.write(bufferedImage, "png", tempImageFile);

            // Load the image into PDFBox
            PDImageXObject pdImage = PDImageXObject.createFromFile(tempImageFile.getAbsolutePath(), document);

            // Scale image to fit receipt size
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            // Scale image to fit within A4
            float imageWidth = pdImage.getWidth();
            float imageHeight = pdImage.getHeight();
            float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
            float newWidth = imageWidth * scale;
            float newHeight = imageHeight * scale;

            // Center the image on the A4 page
            float xPosition = (pageWidth - newWidth) / 2;
            float yPosition = (pageHeight - newHeight) / 2;

            // Create PDF content stream to add image
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, xPosition, yPosition, newWidth, newHeight);
            }

            // Save the PDF
            String desktopPath = System.getProperty("user.home") + "/Desktop/Pawn Prints/" + addPage;
            File receiptFolder = new File(desktopPath);
            if (!receiptFolder.exists()) {
                receiptFolder.mkdirs();
            }

            String pdfPath = getUniqueFilePath(desktopPath, itemID + "_receipt");
            document.save(pdfPath);
            document.close();
            showAlertBox("Item has been printed.");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating PDF", e);
        }
    }

    private String getUniqueFilePath(String directory, String baseName) {
        File file = new File(directory, baseName + ".pdf");
        int count = 1;

        while (file.exists()) {
            file = new File(directory, baseName + " (" + count + ").pdf");
            count++;
        }

        return file.getAbsolutePath();
    }

    private void showAlertBox(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle("Complete");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
