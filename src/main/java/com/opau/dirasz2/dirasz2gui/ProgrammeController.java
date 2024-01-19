package com.opau.dirasz2.dirasz2gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class ProgrammeController {
    int position = -1;
    String currentUUID = "";
    App app;
    private TableView<Programme> table;
    boolean isRunning = false;
    public int nextEventAt = 0;
    private ScheduledExecutorService loopService;
    private RemainingTimeChangedListener remainingTimeChangedListener;
    private RunStateChangeListener runStateChangeListener;
    private SourceDataLine audioDataLine;
    private Thread playThread;
    int elapsedSecs = 0;
    PipedInputStream audioStream = new PipedInputStream();
    PipedOutputStream audioStreamOut;
    private double masterGain = 100;
    private double currentOutVolume = 0;
    private boolean manual = false;
    public String currentTitle = "Ismeretlen";

    public ProgrammeController(App app) {

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
            audioStreamOut = new PipedOutputStream();
            audioDataLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            audioDataLine.open();
        } catch (Exception e) {
            System.out.println(e);
        }
        fillVolumeCanvas(0);
    }

    void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem move = new MenuItem("Kezdési időpont módosítása");
        MenuItem rename = new MenuItem("Átnevezés");
        MenuItem remove = new MenuItem("Törlés");
        menu.setOnAction((e)->{
            if (((MenuItem)e.getTarget()).equals(remove)) {
                remove(table.getSelectionModel().getSelectedItem().uuid);
            } else if (((MenuItem)e.getTarget()).equals(move)) {
                TimePicker tp = new TimePicker(app.scene, false, table.getSelectionModel().getSelectedItem().getStartTimeInt());
                int t = tp.show(null);
                if (t >= 0) {
                    move(table.getSelectionModel().getSelectedItem().uuid, t);
                }
            }
        });
        menu.getItems().addAll(move, rename, remove);
        table.setContextMenu(menu);
        menu.setOnShown((a)->{
            boolean r = ((isRunning && table.getSelectionModel().getSelectedIndex() == position) || table.getSelectionModel().getSelectedItem() == null);
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

        if (!table.getItems().isEmpty() && table.getItems().getFirst().getStartTimeInt() == 0) {
            table.getItems().getFirst().setStartTime(Utils.getTime());
            for (int i = 1; i < table.getItems().size(); i++) {
                table.getItems().get(i).setStartTime(
                        table.getItems().get(i-1).getStartTimeInt()+table.getItems().get(i-1).getLengthInt()
                );
            }
        }
        position = -1;
        playThread = new PlaybackThread(this);
        String threadName = "play" + Utils.getTime();
        playThread.setName(threadName);
        playThread.start();
        return true;
    }

    public void stop() throws InterruptedException {
        try {
            if (app.recorder.isRecording) {
                app.recorder.stopRecording();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentTitle = "Jelenleg nincs adás.";
        remainingTimeChangedListener.onEvent(0);
        setRunning(false);
        loopService.close();
        audioDataLine.drain();
        //playThread.interrupt();
        //playThread.join();
    }
    public class PlaybackThread extends Thread {
        ProgrammeController c;
        public PlaybackThread(ProgrammeController controller) {
            c = controller;
        }
        @Override
        public void run() {
            super.run();

            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(8), new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    double f = currentOutVolume/15000;
                    fillVolumeCanvas(f);
                }
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            loopService = Executors.newSingleThreadScheduledExecutor();
            loopService.scheduleAtFixedRate(()->{
                c.refreshRemainingCounter();
                elapsedSecs++;
            },1,1,TimeUnit.SECONDS);
            audioDataLine.start();
            try {
                runNextProgramme();
                timeline.stop();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Thread exit.");
        }
    }

    private void fillVolumeCanvas(double val) {
        Canvas visualizer = (Canvas)app.scene.lookup("#visCanvas");
        GraphicsContext context = visualizer.getGraphicsContext2D();
        context.setFill(Color.rgb(30,30,30));
        context.fillRect(0,0,10,20);
        context.setFill(Color.rgb(0,255, 0));
        double height = 20*val;
        context.fillRect(0, 20-height, 10, height);
    }

    private void runNextProgramme() throws InterruptedException, IOException {

        if (manual) {
            try {
                audioStream = new PipedInputStream();
                app.lineMixer.out = new PipedOutputStream(audioStream);
                app.lineMixer.open();
                while (isRunning) {
                    byte[] d = audioStream.readNBytes(1024);
                    writeToStreams(d);
                }
            } catch (Exception e) {
                System.out.println(e);
            }
            return;
        }

        position++;
        if (position < table.getItems().size()) {
            Programme next = table.getItems().get(position);
            int current = Utils.getCurrentTimeInt();
            int remaining = next.getStartTimeInt()-current;
            nextEventAt = next.getStartTimeInt();
            System.out.println("[PROGRAMME] Time till next (" + next.getLabel() + "): " + remaining + "s");

            if (remaining >= -1) {
                next.setState(Programme.ProgrammeState.QUEUED);
            } else {
                next.setState(Programme.ProgrammeState.ERRORED);
                runNextProgramme();
                return;
            }

            while (remaining >= 0 && isRunning) {
                byte[] b = new byte[16];
                writeToStreams(b);
                remaining = next.getStartTimeInt()-Utils.getCurrentTimeInt();
            }
            nextEventAt = next.getStartTimeInt()+next.getLengthInt();
            try {
                audioStreamOut = new PipedOutputStream();
                audioStream = new PipedInputStream();
                audioStreamOut.connect(audioStream);
                next.out = audioStreamOut;
                next.setState(Programme.ProgrammeState.RUNNING);
                currentTitle = next.getLabel();
                remainingTimeChangedListener.onEvent(remaining);
                next.start();
                currentUUID = next.uuid;
                while (next.getProgrammeState() == Programme.ProgrammeState.RUNNING && isRunning) {
                    byte[] d = audioStream.readNBytes(512);
                    writeToStreams(d);
                }
                while (next.getProgrammeState() != Programme.ProgrammeState.FINISHED && isRunning);
                currentUUID = "";
                if (!isRunning) {
                    fillVolumeCanvas(0);
                    next.stopProgramme();
                } else {
                    System.out.println("NEXT");
                    runNextProgramme();
                }
            } catch (Exception e) {
                System.out.println("[PROGRAMME] " + e);
            }

            /* region Legacy code
            if (remaining >= -1) {
                if (remaining > 0 && app.recorder.isRecording) {
                    byte[] b = new byte[44100*2*2*remaining];
                    app.recorder.write(b);
                }
                next.setState(Programme.ProgrammeState.QUEUED);
                service.schedule(()->{
                    if (!Thread.currentThread().isAlive()) {
                        return;
                    }
                    nextEventAt = next.getStartTimeInt()+next.getLengthInt();
                    try {
                        audioStreamOut = new PipedOutputStream();
                        audioStream = new PipedInputStream();
                        audioStreamOut.connect(audioStream);
                        next.out = audioStreamOut;
                        next.setState(Programme.ProgrammeState.RUNNING);
                        currentTitle = next.getLabel();
                        remainingTimeChangedListener.onEvent(remaining);
                        next.start();
                        while (next.getProgrammeState() == Programme.ProgrammeState.RUNNING && isRunning) {
                            byte[] d = audioStream.readNBytes(1024);
                            writeToStreams(d);
                        }
                        while (next.getProgrammeState() != Programme.ProgrammeState.FINISHED && isRunning);
                        if (!isRunning) {
                            next.stopProgramme();
                        } else {
                            System.out.println("NEXT");
                            runNextProgramme();
                        }
                    } catch (Exception e) {
                        System.out.println("[PROGRAMME] " + e);
                    }
                }, remaining, TimeUnit.SECONDS);
            } else {
                //Ha már volt, akkor megkeressük a következőt, aminek futnia kell
                next.setState(Programme.ProgrammeState.ERRORED);
                runNextProgramme();
            }
            endregion
            */
        } else {
            stop();
        }
    }

    void writeToStreams(byte[] d) throws IOException {
        for (int i = 0; i < d.length-1; i+=2) {
            if (masterGain != 101) {
                short sampleValue = (short) ((d[i + 1] & 0xFF) << 8 | (d[i] & 0xFF));
                sampleValue = (short) (sampleValue * (masterGain / 100));
                d[i] = (byte) (sampleValue & 0xFF);
                d[i + 1] = (byte) ((sampleValue >> 8) & 0xFF);
            }
        }

        long sum = 0;
        for (int i = 0; i < d.length; i+=2) {
            short b = (short) ((d[i + 1] << 8) | (d[i] & 0xFF));
            sum += b * b;
        }
        currentOutVolume = Math.sqrt((double) sum / d.length);

        audioDataLine.write(d, 0, d.length);

        app.audioServer.writeStream(d, d.length);
        if (app.recorder.isRecording) {
            app.recorder.write(d);
        }
    }

    public void setMasterGain(double gain) {
        masterGain = gain;
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
        sortTable();
    }

    public void enqueueAfterLast(Programme p) {
        if (table.getItems().size() != 0) {
            Programme last = table.getItems().getLast();
            p.setStartTime(last.getStartTimeInt()+last.getLengthInt());
            System.out.println("Added at " + p.getStartTimeInt());
        }
        enqueue(p);
    }

    void remove(String uuid) {

        if (isRunning && uuid.equals(currentUUID)) {
            return;
        }

        for (Programme p : table.getItems()) {
            if (p.uuid.equals(uuid)) {
                table.getItems().remove(p);
                break;
            }
        }
        if (isRunning) {
            refreshCurrentPosition();
        }
    }

    void move(String uuid, int t) {

        for (Programme p : table.getItems()) {
            if (p.uuid.equals(uuid)) {
                p.setStartTime(t);
            }
        }

        sortTable();

    }

    void refreshCurrentPosition() {
        for (int i = 0; i < table.getItems().size(); i++) {
            if (table.getItems().get(i).uuid.equals(currentUUID)) {
                position = i;
                break;
            }
        }
    }

    void sortTable() {
        table.getItems().sort((o1,o2)->{
            if (o1.getStartTimeInt() > o2.getStartTimeInt()) {
                return 1;
            }
            return -1;
        });
        if (isRunning) {
            refreshCurrentPosition();
        }
    }

    public void setVolume(float vol) {
        final FloatControl volumeControl = (FloatControl) audioDataLine.getControl( FloatControl.Type.MASTER_GAIN );
        volumeControl.setValue( 20.0f * (float) Math.log10( vol / 100.0 ) );
    }

    void setRunning(boolean running) {
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
}
