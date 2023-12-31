package com.opau.dirasz2.dirasz2gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class App extends Application {
    public Scene scene;
    public ProgrammeController programmeController;
    @Override
    public void start(Stage stage) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main_window.fxml"));
        scene = new Scene(fxmlLoader.load(), 800, 640);
        Platform.setImplicitExit(false);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        final String os = System.getProperty("os.name");
        stage.setTitle("DiRASz II");
        stage.setScene(scene);
        stage.show();
        if (os != null && os.startsWith("Mac"))
            ((MenuBar)scene.lookup("#menuBar")).useSystemMenuBarProperty().set(true);
        FileListViewController fileListViewController = new FileListViewController(this);
        programmeController = new ProgrammeController(this);
        ((MainWindowController)fxmlLoader.getController()).setApp(this);
    }

    public static void main(String[] args) {
        launch();
    }
}