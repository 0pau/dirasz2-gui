package com.opau.dirasz2.dirasz2gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

import java.awt.event.ActionEvent;
import java.io.IOException;
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

    @FXML
    void showAddProgrammeWindow() throws IOException {
        AddProgrammeWindow apw = new AddProgrammeWindow(app);
    }

    public void setApp(App app) {
        this.app = app;

        Label elapsedTimeLabel = (Label) app.scene.lookup("#elapsedTimeLabel");
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

        Button startRecButton = (Button) app.scene.lookup("#startRecButton");
        startRecButton.setOnMouseClicked((e)->{
            if (!app.recorder.isRecording) {
                if (!app.programmeController.isRunning) {
                    app.programmeController.start();
                }
                try {
                    app.recorder.startRecording();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                try {
                    app.recorder.stopRecording();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        HBox onAirSign = (HBox) app.scene.lookup("#onAirSign");
        Label recordElapsedTimeLabel = (Label) app.scene.lookup("#recordElapsedTimeLabel");

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
                    elapsedTimeLabel.setText("00:00:00");
                }
            });

        });

        HBox recordSign = (HBox) app.scene.lookup("#recordSign");
        Label byteCounter = (Label) app.scene.lookup("#byteCounter");
        Label titleLabel = (Label)app.scene.lookup("#currentProgrammeLabel");
        app.recorder.setListener((id)->{
            Platform.runLater(()->{
                if (id == 0) {
                    startRecButton.setText("Felvétel leállítása");
                    recordSign.setOpacity(1);
                } else {
                    startRecButton.setText("Felvétel indítása");
                    recordSign.setOpacity(0.4);
                    recordElapsedTimeLabel.setText("00:00:00");
                    byteCounter.setText(Utils.formatSize(0));
                }
            });

        });



        app.programmeController.setRemainingTimeChangedListener((rem)->{
            Platform.runLater(()->{
                Label l = (Label) app.scene.lookup("#remainingCounter");
                l.setText(Utils.formatTimeInt(rem, true));
                elapsedTimeLabel.setText(Utils.formatTimeInt(app.programmeController.elapsedSecs, true));
                if (!app.programmeController.currentTitle.equals(titleLabel.getText())) {
                    titleLabel.setText(app.programmeController.currentTitle);
                }
                if (app.recorder.isRecording) {
                    recordElapsedTimeLabel.setText(Utils.formatTimeInt(Utils.getCurrentTimeInt()-app.recorder.recordStartTimeStamp, true));
                    byteCounter.setText(Utils.formatSize(app.recorder.bytesWritten));
                }
            });
        });

        Slider localSlider = (Slider) app.scene.lookup("#localLineVolumeSlider");
        localSlider.valueProperty().addListener((d)->{
            app.programmeController.setVolume((float)localSlider.getValue());
        });

        Slider masterGainSlider = (Slider) app.scene.lookup("#masterGainSlider");
        masterGainSlider.valueProperty().addListener((d)->{
            app.programmeController.setMasterGain(masterGainSlider.getValue());
        });

        Label clockLabel = (Label) app.scene.lookup("#clock");
        ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor();
        clock.scheduleAtFixedRate(()->{
            Platform.runLater(()->{
                Calendar calendar = Calendar.getInstance();
                int current = Utils.timeToInt(calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND));
                clockLabel.setText(Utils.formatTimeInt(current, true));
            });
        }, 0, 1, TimeUnit.SECONDS);

    }



}