package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class Start extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Load the FXML file with the resource bundle
        FXMLLoader fxmlLoader = new FXMLLoader(Start.class.getResource("/user-login.fxml"));

        Locale defaultLocale = new Locale("en");
        ResourceBundle bundle = ResourceBundle.getBundle("values-en.messages", defaultLocale);
        fxmlLoader.setResources(bundle);

        // Create the scene
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setWidth(Screen.getPrimary().getBounds().getWidth());
        stage.setHeight(Screen.getPrimary().getBounds().getHeight());
        stage.setTitle("Pawn Database");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
