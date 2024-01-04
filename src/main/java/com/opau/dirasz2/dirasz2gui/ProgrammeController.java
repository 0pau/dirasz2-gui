package com.opau.dirasz2.dirasz2gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProgrammeController {
    int position = -1;
    App app;
    TableView<Programme> table;
    boolean isRunning = false;
    public int nextEventAt = 0;
    ScheduledExecutorService loopService;
    Thread playService;
    RemainingTimeChangedListener remainingTimeChangedListener;
    RunStateChangeListener runStateChangeListener;
    ElapsedTimeListener elapsedTimeListener;
    SourceDataLine audioDataLine;
    Thread playThread;
    int elapsedSecs = 0;

    public ProgrammeController(App app) throws LineUnavailableException {
        this.app = app;
        table = (TableView<Programme>) app.scene.lookup("#programmeTable");
        table.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("statusString"));
        table.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("startTimeString"));
        table.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("length"));
        table.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("label"));
        table.getColumns().get(4).setCellValueFactory(new PropertyValueFactory<>("typeString"));
        setupContextMenu();

        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false));
        try {
            audioDataLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            audioDataLine.open();
        } catch (LineUnavailableException e) {
            System.out.println(e);
        }
    }

    void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem move = new MenuItem("Kezdési időpont módosítása");
        MenuItem rename = new MenuItem("Átnevezés");
        MenuItem remove = new MenuItem("Törlés");
        menu.getItems().addAll(move, rename, remove);
        table.setContextMenu(menu);
        menu.setOnShown((a)->{
            boolean r = (isRunning && table.getSelectionModel().getSelectedIndex() == position);
            move.setDisable(r);
            rename.setDisable(r);
            remove.setDisable(r);
        });
    }

    public boolean start() {
        if (table.getItems().isEmpty()) {
            return false;
        }
        setRunning(true);
        elapsedSecs = 0;

        System.out.println("HOSSZ " + table.getItems().get(0).getStartTimeInt());

        if (table.getItems().size() >0 && table.getItems().get(0).getStartTimeInt() == 0) {
            table.getItems().get(0).setStartTime(Utils.getTime());
            for (int i = 1; i < table.getItems().size(); i++) {
                table.getItems().get(i).setStartTime(
                        table.getItems().get(i-1).getStartTimeInt()+table.getItems().get(i-1).getLengthInt()
                );
            }
        }
        service = Executors.newSingleThreadScheduledExecutor();
        position = -1;
        playThread = new PlaybackThread(this);
        String threadName = "play" + Utils.getTime();
        playThread.setName(threadName);
        System.out.println(threadName);
        playThread.start();



        return true;
    }

    ScheduledExecutorService service;
    public void stop() throws InterruptedException {
        setRunning(false);
        loopService.close();
        audioDataLine.drain();
        //audioDataLine.stop();
        playThread.interrupt();
        playThread.join();
    }
    public class PlaybackThread extends Thread {
        ProgrammeController c;
        public PlaybackThread(ProgrammeController controller) {
            c = controller;
        }
        @Override
        public void run() {
            super.run();
            loopService = Executors.newSingleThreadScheduledExecutor();
            loopService.scheduleAtFixedRate(()->{
                c.refreshRemainingCounter();
                elapsedSecs++;
            },1,1,TimeUnit.SECONDS);
            //audioDataLineSetup
            audioDataLine.start();
            runNextProgramme();
            System.out.println("Thread exit.");
        }
    }

    private void runNextProgramme() {
        position++;
        if (position < table.getItems().size()) {
            Programme next = table.getItems().get(position);
            Calendar calendar = Calendar.getInstance();
            int current = Utils.timeToInt(calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND));
            int remaining = next.getStartTimeInt()-current;
            nextEventAt = next.getStartTimeInt();
            System.out.println("[PROGRAMME] Time till next (" + next.getLabel() + "): " + remaining + "s");
            if (remaining >= 0) {
                next.setState(Programme.ProgrammeState.QUEUED);
                service.schedule(()->{
                    if (!Thread.currentThread().isAlive()) {
                        return;
                    }
                    System.out.println(Thread.currentThread().getName());
                    nextEventAt = next.getStartTimeInt()+next.getLengthInt();
                    next.setDataAvailableListener((data, x)->{
                        if (isRunning) {
                            try {
                                audioDataLine.write(data, 0, x);
                                app.audioServer.writeStream(data, x);
                            } catch (Exception e) {
                                System.out.println(e);
                            }
                        } else {
                            next.stop();
                        }
                    });
                    next.setStateListener((state)->{
                        if (state == Programme.ProgrammeState.FINISHED) {
                            runNextProgramme();
                        }
                    });
                    try {
                        next.start();
                    } catch (Exception e) {
                        System.out.println("[PROGRAMME] " + e);
                        throw new RuntimeException(e);
                    }
                }, remaining, TimeUnit.SECONDS);
            } else {
                //Ha már volt, akkor megkeressük a következőt, aminek futnia kell
                next.setState(Programme.ProgrammeState.ERRORED);
                runNextProgramme();
            }
        } else {
            setRunning(false);
            loopService.close();
        }
    }

    public void refreshRemainingCounter() {
        if (remainingTimeChangedListener != null) {
            Calendar calendar = Calendar.getInstance();
            int current = Utils.timeToInt(calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND));
            int remaining = app.programmeController.nextEventAt - current;
            remainingTimeChangedListener.onEvent(remaining);
        }
    }

    public void enqueue(Programme p) {
        table.getItems().add(p);
    }

    public void enqueueAfterLast(Programme p) {
        if (table.getItems().size() != 0) {
            Programme last = table.getItems().getLast();
            p.setStartTime(last.getStartTimeInt()+last.getLengthInt());
            System.out.println("Added at " + p.getStartTimeInt());
        }
        enqueue(p);
    }

    public void setVolume(float vol) {
        final FloatControl volumeControl = (FloatControl) audioDataLine.getControl( FloatControl.Type.MASTER_GAIN );
        volumeControl.setValue( 20.0f * (float) Math.log10( vol / 100.0 ) );
    }

    void setRunning(boolean running) {
        if (!running) {
            service.shutdownNow();
        }
        isRunning = running;
        if (runStateChangeListener != null) {
            runStateChangeListener.onEvent(isRunning);
        }
    }

    public class ProgrammeControllerException extends Exception {
        public ProgrammeControllerException(PCEType type) {
            super(type.name());
        }

        public enum PCEType {
            EMPTY_LIST,PROGRAMME_COLLISION,PROGRAMME_FAILED
        }
    }

    public interface RemainingTimeChangedListener {
        void onEvent(int remainingTime);
    }

    public void setRemainingTimeChangedListener(RemainingTimeChangedListener remainingTimeChangedListener) {
        this.remainingTimeChangedListener = remainingTimeChangedListener;
    }

    public interface RunStateChangeListener {
        void onEvent(boolean running);
    }

    public void setRunStateChangeListener(RunStateChangeListener runStateChangeListener) {
        this.runStateChangeListener = runStateChangeListener;
    }

    public interface ElapsedTimeListener {
        void onEvent(int elapsed);
    }

    public void setElapsedTimeListener(ElapsedTimeListener elapsedTimeListener) {
        this.elapsedTimeListener = elapsedTimeListener;
    }
}
