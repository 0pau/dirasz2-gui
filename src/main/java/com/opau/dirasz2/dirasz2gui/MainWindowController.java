package com.opau.dirasz2.dirasz2gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainWindowController {
    App app;
    boolean initialized = false;
    @FXML
    void initialize() {
        initialized = true;
    }

    public void setApp(App app) {
        this.app = app;

        Button startStop = (Button) app.scene.lookup("#startStop");
        startStop.setOnMouseClicked((e)->{
            if (!app.programmeController.isRunning) {
                app.programmeController.start();
            } else {

                try {
                    app.programmeController.stop();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        HBox onAirSign = (HBox) app.scene.lookup("#onAirSign");

        app.programmeController.setRunStateChangeListener((run)->{
            Platform.runLater(()->{
                if (run) {
                    startStop.setText("Műsor leállítása");
                    onAirSign.setOpacity(1);
                } else {
                    startStop.setText("Műsor indítása");
                    onAirSign.setOpacity(0.4);
                    Label l = (Label) app.scene.lookup("#remainingCounter");
                    l.setText("--:--:--");
                }
            });

        });

        app.programmeController.setRemainingTimeChangedListener((rem)->{
            Platform.runLater(()->{
                Label l = (Label) app.scene.lookup("#remainingCounter");
                l.setText(Utils.formatTimeInt(rem, true));
            });
        });

    }



}