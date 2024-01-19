package com.opau.dirasz2.dirasz2gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Programme extends Thread {
    Type type = Type.UNDEFINED;
    private String label;
    private int length = 0;
    private int startTime = 0;
    private ProgrammeStateListener stateListener;
    private ProgrammeState state = ProgrammeState.WAITING;
    DataAvailableListener dataAvailableListener;
    PipedOutputStream out = new PipedOutputStream();
    public void setLength(int length) {
        this.length = length;
    }

    public final String uuid = UUID.randomUUID().toString();

    public void setStartTime(int startTime) {
        this.startTime = startTime;
        startTimeStringProperty().set(Utils.formatTimeInt(startTime, false));
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    private StringProperty statusString;
    public StringProperty statusStringProperty() {
        if (statusString == null) {
            statusString = new SimpleStringProperty(this, "statusString");
        }
        return statusString;
    }

    public StringProperty startTimeString;
    public StringProperty startTimeStringProperty() {
        if (startTimeString == null) {
            startTimeString = new SimpleStringProperty(this, "startTimeString");
        }
        return startTimeString;
    }

    public String getStartTimeString() {return startTimeStringProperty().get();}

    public String getStatusString() {
        return statusStringProperty().get();
    }

    public String getLength() {
        return Utils.formatTimeInt(length, true);
    }
    public int getLengthInt() {
        return length;
    }

    public int getStartTimeInt() {
        return startTime;
    }

    public String getTypeString() {
        return type.toString();
    }

    public Programme() {}
    public Programme(String label, int l, int st) {
        this.label = label;
        length = l;
        startTime = st;
    }

    @Override
    public void run() {
        super.run();
        System.out.println("[P]\t"+getLabel()+" (" + uuid + ") running");
        setState(ProgrammeState.RUNNING);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.schedule(()->{
            onFinish();
        }, length, TimeUnit.SECONDS);
    }

    public void connectStream(PipedInputStream is) {
        try {
            out.connect(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopProgramme() {
        setState(ProgrammeState.FINISHED);
    }

    public void setStateListener(ProgrammeStateListener stateListener) {
        this.stateListener = stateListener;
    }

    public String toString() {
        return "{}";
    }

    public Type getType() {
        return type;
    }

    void setState(ProgrammeState s) {
        state = s;
        statusStringProperty().set(state.toShortStr());
        if (stateListener != null) {
            stateListener.onChange(state);
        }
    }

    public enum Type {FILE,PLAYLIST,MACRO,MANUAL,UNDEFINED;

        @Override
        public String toString() {
            switch (this) {
                case FILE:
                    return "Fájl";
                case PLAYLIST:
                    return "Lejátszási lista";
                case MACRO:
                    return "Makró";
                case MANUAL:
                    return "Kézi";
                default:
                    return "Ismeretlen";
            }
        }
    }

    void setType(Type t) {
        type = t;
    }
    public static Programme buildFromString(String s) {
        return null;
    }

    public interface ProgrammeStateListener {
        void onChange(ProgrammeState state);
    }

    public enum ProgrammeState {WAITING,RUNNING,FINISHED,ERRORED,QUEUED;
        public String toShortStr() {
            switch (this) {
                case WAITING:
                    return "";
                case RUNNING:
                    return "▶";
                case FINISHED:
                    return "✓";
                case QUEUED:
                    return "⧖";
                default:
                    return "X";
            }
        }
    }

    void onFinish() {
        System.out.println("Finished. - " + Thread.currentThread().getName());
        setState(ProgrammeState.FINISHED);
    }

    public interface DataAvailableListener {
        void onAvailable(byte[] b, int dataLength) throws IOException;
    }

    public void setDataAvailableListener(DataAvailableListener dataAvailableListener) {
        this.dataAvailableListener = dataAvailableListener;
    }

    public ProgrammeState getProgrammeState() {
        return state;
    }
}
