package com.opau.dirasz2.dirasz2gui;

import javafx.application.Platform;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;

public class AudioServer extends Thread {

    App app;
    private ServerSocket socket;
    boolean isClientConnected = false;
    public OutputStream out;

    public AudioServer(App a) throws IOException {
        super();
        app = a;
        socket = new ServerSocket(5280);
    }

    void setUdpStateLabel(boolean connected) {
        isClientConnected = connected;
        Platform.runLater(()->{
            if (connected) {
                app.scene.lookup("#udpClientStateLabel").setOpacity(1);
            } else {
                app.scene.lookup("#udpClientStateLabel").setOpacity(0.25);
            }
        });
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                Socket c = socket.accept();
                if (!isClientConnected) {
                    out = c.getOutputStream();
                    setUdpStateLabel(true);
                } else {
                    c.close();
                }

            } catch (IOException e) {
                System.out.println(e);
            }
        }

    }

    public void writeStream(byte[] data, int size) {
        try {
            if (isClientConnected) {
                app.audioServer.out.write(data, 0, size);
            }
        } catch (Exception e) {
            setUdpStateLabel(false);
        }
    }

}
