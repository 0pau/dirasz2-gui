package com.opau.dirasz2.dirasz2gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class App extends Application {
    public Scene scene;
    public ProgrammeController programmeController;
    public AudioServer audioServer;
    public AudioSourcesListManager audioSourcesListManager;
    public LineMixer lineMixer;
    public Recorder recorder;
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main_window.fxml"));
        scene = new Scene(fxmlLoader.load(), 800, 640);
        stage.setOnCloseRequest((event)->{

            if (programmeController.isRunning) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.getButtonTypes().remove(ButtonType.OK);
                alert.getButtonTypes().add(ButtonType.NO);
                alert.getButtonTypes().add(ButtonType.YES);
                alert.setTitle("Kliépés megakadályozva");
                alert.setContentText(String.format("A műsor éppen fut!\nBiztosan ki szeretne lépni a programból?"));
                alert.initOwner(stage.getOwner());
                Optional<ButtonType> res = alert.showAndWait();

                if (res.isPresent() && res.get().equals(ButtonType.NO)) {
                    event.consume();
                    return;
                }
                try {
                    programmeController.stop();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            System.exit(0);
        });
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
        lineMixer = new LineMixer();
        lineMixer.start();
        audioSourcesListManager = new AudioSourcesListManager(this);
        programmeController = new ProgrammeController(this);
        audioServer = new AudioServer(this);
        audioServer.start();
        recorder = new Recorder(this);
        //programmeController.enqueue(new PlaylistProgramme("/Users/opau/Music/teszt", 2));
        ((MainWindowController)fxmlLoader.getController()).setApp(this);
    }

    public static void main(String[] args) {
        launch();
    }
}