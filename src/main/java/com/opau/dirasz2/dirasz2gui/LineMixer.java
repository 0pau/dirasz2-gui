package com.opau.dirasz2.dirasz2gui;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
    A LineMixer feladata a számítógépen található mikrofonokból
    jövő hangok összemixelése és a végeredmény kiírása egy streamre.
*/
public class LineMixer extends Thread {

    ArrayList<InputSource> sources = new ArrayList<>();
    LineMixerEventListener listener;
    PipedOutputStream out = new PipedOutputStream();
    boolean write = false;
    AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, true);

    public LineMixer() {
        super();
    }

    @Override
    public void run() {
        super.run();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(()->{
            try {
                updateInputSources();
            } catch (LineUnavailableException e) {
                System.out.println(e);
            }
        }, 0, 2, TimeUnit.SECONDS);


        byte[] buf = new byte[4096];
        while (!Thread.interrupted()) {
            if (write) {
                try {
                    if (sources.size() == 1) {
                        buf = readFromStreamIndex(0);
                    } else {
                        for (int i = 0; i < sources.size(); i++) {
                            byte[] x = readFromStreamIndex(i);
                            for (int j = 0; j < 4096; j++) {
                                buf[j] = (byte) ((buf[j] + x[j])>>1);
                            }
                        }
                    }
                    out.write(buf);
                } catch (Exception e) {
                    System.out.println("LINEMIXER says: " + e);
                }
            }
        }
    }

    byte[] readFromStreamIndex(int index) throws IOException {
        byte[] buf = new byte[4096];
        sources.get(index).stream.read(buf, 0, 4096);
        for (int i = 0; i < buf.length-1; i++) {
            short sampleValue = (short) ((buf[i + 1] & 0xFF) << 8 | (buf[i] & 0xFF));
            sampleValue = (short) (sampleValue * 0.0035);
            buf[i] = (byte) (sampleValue & 0xFF);
            buf[i + 1] = (byte) ((sampleValue >> 8) & 0xFF);
        }
        return buf;
    }

    public void open() throws LineUnavailableException {
        for (InputSource is : sources) {
            is.line.open();
            is.line.start();
            AudioInputStream s = new AudioInputStream(is.line);
            is.stream = AudioSystem.getAudioInputStream(targetFormat, s);
        }
        write = true;
    }

    public void close() throws IOException {
        write = false;
        out.close();
        for (InputSource is: sources) {
            is.line.close();
            is.line.drain();
        }
    }

    public void updateInputSources() throws LineUnavailableException {
        for (InputSource a: sources) {
            a.stillExists = false;
        }
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (Mixer.Info i : infos) {
            Mixer m = AudioSystem.getMixer(i);
            if (i.getName().contains("Default Audio Device")) {
                continue;
            }
            for (Line.Info lineInfo : m.getTargetLineInfo()) {
                if (lineInfo.getLineClass().getCanonicalName().contains("TargetDataLine")) {
                    String name = i.getName();
                    if (isSourceInList(name)) {
                        sources.get(getInputSourceIndexForName(name)).stillExists = true;
                    } else {
                        System.out.println("[SOURCE] Source '" + name + "' added.");
                        InputSource newInputSource = new InputSource(name, (TargetDataLine) m.getLine(lineInfo), true);
                        sources.add(newInputSource);
                        if (listener != null) {
                            listener.onEvent(0, newInputSource.uuid);
                        }
                    }
                }

            }/*
            if (!write) {
                m.close();
            }*/

        }

        ArrayList<InputSource> toRemove = new ArrayList<>();
        for (InputSource is: sources) {
            if (!is.stillExists) {
                System.out.println("[SOURCE] Source '" + is.name + "' removed.");
                if (listener != null) {
                    listener.onEvent(1, is.uuid);
                }
                toRemove.add(is);
            }
        }
        for (InputSource is: toRemove) {
            sources.remove(is);
        }
    }

    public boolean isSourceInList(String name) {
        for (InputSource is: sources) {
            if (is.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public int getInputSourceIndexForName(String name) {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public InputSource getInputSourceForUuid(String uuid) {
        for (InputSource is: sources) {
            if (is.uuid.equals(uuid)) {
                return is;
            }
        }
        return null;
    }

    public class InputSource {
        public String name;
        public TargetDataLine line;
        public boolean stillExists = false;
        public String uuid;
        public AudioInputStream stream;

        public InputSource(String n, TargetDataLine l, boolean e) throws LineUnavailableException {
            name = n;
            line = l;
            stillExists = e;
            uuid = UUID.randomUUID().toString();
        }
    }

    public interface LineMixerEventListener {
        public void onEvent(int eventID, String uuid);
    }

    public void setListener(LineMixerEventListener listener) {
        this.listener = listener;
    }

}
