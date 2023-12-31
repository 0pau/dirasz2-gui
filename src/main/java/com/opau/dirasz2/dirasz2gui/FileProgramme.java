package com.opau.dirasz2.dirasz2gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class FileProgramme extends Programme {
    private String path;
    private File f;
    AudioInputStream stream;
    byte[] buffer;

    public FileProgramme(String path) throws UnsupportedAudioFileException, IOException {
        super.type = Type.FILE;
        f = new File(path);
        setLabel(f.getName());
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
        AudioFormat format = audioInputStream.getFormat();
        long frames = audioInputStream.getFrameLength();
        double durationInSeconds = (frames+0.0) / format.getFrameRate();
        setLength((int)durationInSeconds);
    }

    @Override
    public void stop() throws IOException {
        stream.close();
        buffer = null;
        super.stop();
    }

    @Override
    public void start() throws IOException, UnsupportedAudioFileException {
        super.start();
        stream = AudioSystem.getAudioInputStream(f);
        if (dataAvailableListener != null) {
            int bytesRead = 0;
            buffer = new byte[1024];
            while (bytesRead != -1) {
                bytesRead = stream.read(buffer, 0, buffer.length);
                dataAvailableListener.onAvailable(buffer, bytesRead);
            }
            buffer = null;
            stream.close();
        }
    }
}
