package com.opau.dirasz2.dirasz2gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main_window.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 640);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        final String os = System.getProperty("os.name");
        stage.setTitle("DiRASz II");
        stage.setScene(scene);
        stage.show();
        if (os != null && os.startsWith("Mac"))
            ((MenuBar)scene.lookup("#menuBar")).useSystemMenuBarProperty().set(true);

        FileListViewController fileListViewController = new FileListViewController(scene);

    }

    public static void main(String[] args) {
        launch();
    }
}