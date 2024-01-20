package com.opau.dirasz2.dirasz2gui;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.annotation.Target;
import java.util.concurrent.BlockingQueue;

public class ManualProgramme extends Programme {
    App a;
    public ManualProgramme(App a) {
        super.type = Type.MANUAL;
        this.a = a;
    }

    @Override
    public void run() {
        super.run();
        try {
            PipedInputStream in = new PipedInputStream();
            a.lineMixer.out = new PipedOutputStream(in);
            a.lineMixer.open();
            while (getProgrammeState() == ProgrammeState.RUNNING) {
                byte[] b = in.readNBytes(32);
                out.write(b);
            }
            System.out.println("Manual got out of loop");
            a.lineMixer.close();
            in.close();
            /*
            TargetDataLine l = a.audioSourcesListManager.sources.getFirst().line;

            l.open();
            l.start();
            AudioInputStream io = new AudioInputStream(l);
            AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, true);
            AudioInputStream io2 = AudioSystem.getAudioInputStream(targetFormat, io);

            byte[] buf = new byte[l.getBufferSize()/4];
            int bytesRead = 0;
            while ((bytesRead = io2.read(buf, 0, buf.length)) != -1 && getProgrammeState() == ProgrammeState.RUNNING) {
                for (int i = 0; i < buf.length-1; i++) {
                    short sampleValue = (short) ((buf[i + 1] & 0xFF) << 8 | (buf[i] & 0xFF));
                    sampleValue = (short) (sampleValue * 0.0025);
                    buf[i] = (byte) (sampleValue & 0xFF);
                    buf[i + 1] = (byte) ((sampleValue >> 8) & 0xFF);
                }
                out.write(buf);
            }

            out.close();
            io2.close();
            io.close();
            l.drain();
            l.stop();
             */
        } catch (Exception e) {
            try {
                a.lineMixer.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            //System.out.println(e);
        }
    }
}
