package com.opau.dirasz2.dirasz2gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class FileProgramme extends Programme {
    private String path;
    private File f;
    AudioInputStream stream;
    AudioInputStream resampledStream;
    byte[] buffer;
    boolean needPipeClose = true;
    boolean outClosed = false;

    public FileProgramme(String path) throws UnsupportedAudioFileException, IOException {
        super();
        super.type = Type.FILE;
        f = new File(path);
        setLabel(f.getName().substring(0, f.getName().lastIndexOf(".")));
        stream = AudioSystem.getAudioInputStream(f);
        AudioFormat source = stream.getFormat();
        long frames = stream.getFrameLength();
        double durationInSeconds = (frames+0.0) / source.getFrameRate();
        setLength((int)durationInSeconds);
    }

    public void stopProgramme() {
        super.stopProgramme();
        /*
        try {
            if (needPipeClose) {
                stream.close();
            }
            buffer = null;
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void run() {
        super.run();
        try {
            AudioFormat soruceFmt = stream.getFormat();
            AudioFormat targetFormat = new AudioFormat(soruceFmt.getEncoding(), 44100, 16, 2, soruceFmt.getFrameSize(), 44100, soruceFmt.isBigEndian());
            resampledStream = AudioSystem.getAudioInputStream(targetFormat, stream);
            int bytesRead = 0;
            buffer = new byte[1024];
            while (getProgrammeState() == ProgrammeState.RUNNING) {
                bytesRead = resampledStream.read(buffer, 0, buffer.length);
                if (bytesRead == -1) {
                    if (needPipeClose) {
                        out.close();
                    }
                    break;
                }
                //byte[] bout = new byte[bytesRead];
                //System.arraycopy(buffer, 0, bout, 0, bytesRead);
                out.write(buffer);
            }
            setState(ProgrammeState.FINISHED);
            out = null;
            buffer = null;
            resampledStream.close();
            stream.close();
        } catch (Exception e) {
            System.out.println("Pipe close!");
        }
    }
}
