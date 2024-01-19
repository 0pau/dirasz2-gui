package com.opau.dirasz2.dirasz2gui;

import javafx.scene.chart.PieChart;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class Recorder {
    App app;
    int bytesWritten = 0;
    boolean isRecording = false;
    RandomAccessFile raf;
    int recordStartTimeStamp = -1;
    RecorderStateListener listener;
    public Recorder(App a) {
        app = a;
    }
    public void startRecording() throws IOException {

        bytesWritten=0;
        LocalDateTime ldt = LocalDateTime.now();
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").format(ldt);

        Calendar c = Calendar.getInstance();
        recordStartTimeStamp = Utils.getCurrentTimeInt();

        File f = new File(System.getProperty("user.home") + "/DiRASz-REC-" + date + ".wav");
        raf = new RandomAccessFile(f, "rw");
        raf.writeBytes("RIFF");
        raf.write(intToByteArray(0, 4));
        raf.writeBytes("WAVE");
        raf.writeBytes("fmt ");
        raf.write(intToByteArray(16, 4));
        raf.write(intToByteArray(1, 2));
        raf.write(intToByteArray(2, 2));
        raf.write(intToByteArray(44100, 4));
        raf.write(intToByteArray((44100*16*2)/8, 4));
        raf.write(intToByteArray((16*2)/8, 2));
        raf.write(intToByteArray(16, 2));
        raf.writeBytes("data");
        raf.write(intToByteArray(0, 4));
        isRecording = true;

        if (listener != null) {
            listener.onEvent(0);
        }
    }

    public void stopRecording() throws IOException {
        if (listener != null) {
            listener.onEvent(1);
        }
        isRecording = false;
        raf.seek(4);
        raf.write(intToByteArray(44+bytesWritten, 4));
        raf.seek(40);
        raf.write(intToByteArray(bytesWritten, 4));
        raf.close();
    }

    public void write(byte[] buf) throws IOException {
        if (isRecording) {
            raf.write(buf);
            bytesWritten += buf.length;
        }
    }

    public byte[] intToByteArray(int integer, int size) {
        byte[] bytes = new byte[size];

        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (size == 2) {
            buffer.putShort((short)integer);
        } else if (size == 4) {
            buffer.putInt(integer);
        }
        bytes = buffer.array();

        return bytes;
    }

    public void setListener(RecorderStateListener listener) {
        this.listener = listener;
    }

    public interface RecorderStateListener {
        public void onEvent(int eventId);
    }

}
