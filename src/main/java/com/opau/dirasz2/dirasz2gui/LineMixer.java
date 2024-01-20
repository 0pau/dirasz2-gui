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
    AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, false);

    int enabledDevices = 0;

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


        byte[] buf;
        while (!Thread.interrupted()) {
            if (write) {
                try {
                    buf = new byte[1024];
                    for (int i = 0; i < sources.size(); i++) {
                        byte[] x = readFromStreamIndex(i);
                        Utils.gain(x, sources.get(i).volume);
                        if (isDeviceEnabled(i)) {
                            if (enabledDevices == 1) {
                                buf = x;
                            } else {
                                buf = mixBuffers(buf, x);
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

    private boolean isDeviceEnabled(int index) {
        return sources.get(index).enabled;
    }

    private int getFirstEnabled() {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).enabled) {
                return i;
            }
        }
        return -1;
    }

    private byte[] mixBuffers(byte[] bufferA, byte[] bufferB) {
        byte[] array = new byte[bufferA.length];

        for (int i=0; i<bufferA.length; i++) {
            //array[i] = (byte)(((bufferA[i]+bufferB[i])>>1));
            array[i] = (byte)(((bufferA[i]+bufferB[i])));
        }

        return array;
    }


    byte[] readFromStreamIndex(int index) throws IOException {
        byte[] buf = new byte[1024];
        sources.get(index).stream.read(buf, 0, 1024);
        return buf;
    }

    public void open() throws LineUnavailableException {
        for (InputSource is : sources) {
            AudioFormat preferred = is.line.getFormat();
            System.out.println("Preferred for " + is.name + ": " + preferred.toString());

            is.line.open(new AudioFormat(44100, 16, 1, true, true));
            is.line.start();
            AudioInputStream s = new AudioInputStream(is.line);
            //is.stream = new AudioInputStream(s, targetFormat, 16);
            is.stream = AudioSystem.getAudioInputStream(targetFormat, s);
        }
        write = true;
    }

    public void close() throws IOException {
        System.out.println("Closing LineMixer...");
        write = false;
        out.close();
        out = null;
        for (InputSource is: sources) {
            is.stream.close();
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
                        enabledDevices++;
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
            if (is.enabled) {
                enabledDevices--;
            }
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
        public boolean enabled = true;
        public double volume = 100;
        public boolean monitoringEnabled = true;

        public InputSource(String n, TargetDataLine l, boolean e) throws LineUnavailableException {
            name = n;
            line = l;
            stillExists = e;
            uuid = UUID.randomUUID().toString();
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (enabled) {
                enabledDevices++;
            } else {
                enabledDevices--;
            }
        }
    }

    public interface LineMixerEventListener {
        public void onEvent(int eventID, String uuid);
    }

    public void setListener(LineMixerEventListener listener) {
        this.listener = listener;
    }

}
