package com.opau.dirasz2.dirasz2gui;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class PlaylistProgramme extends Programme {

    private ArrayList<FileProgramme> items = new ArrayList<>();
    private int playCount = 1;
    private Order order = Order.LINEAR;
    public enum Order {LINEAR,SHUFFLE}

    public PlaylistProgramme() {
        super.type = Type.PLAYLIST;
    }

    public PlaylistProgramme(String path, int playC, Order o) throws Exception {
        super.setType(Type.PLAYLIST);
        order = o;
        playCount = playC;
        File folder = new File(path);
        if (!folder.isDirectory()) {
            throw new Exception("Path must be a directory.");
        }
        for (File f : folder.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".wav")) {
                items.add(new FileProgramme(f.getPath()));
            }
        }

        if (order == Order.SHUFFLE) {
            items.sort((o1,o2)-> ThreadLocalRandom.current().nextInt(-1, 2));
        }

        int l = 0;
        String n = "";
        for (int i = 0; i < playCount && i < items.size(); i++) {
            l += items.get(i).getLengthInt();
            n += items.get(i).getLabel() + ", ";
        }
        setLabel(n);
        setLength(l);
    }

    @Override
    void onFinish() {
        //do nothing.
    }

    @Override
    public void run() {
        super.run();
        setState(ProgrammeState.RUNNING);
        for (int i = 0; i < playCount && i < items.size(); i++) {
            try {
                PipedInputStream is = new PipedInputStream();
                PipedOutputStream os = new PipedOutputStream(is);
                items.get(i).out = os;
                items.get(i).setState(ProgrammeState.RUNNING);
                items.get(i).start();
                while (items.get(i).getProgrammeState() == ProgrammeState.RUNNING && getProgrammeState()==ProgrammeState.RUNNING) {
                    out.write(is.readNBytes(1024));
                }
                if (getProgrammeState()!=ProgrammeState.RUNNING) {
                    items.get(i).stopProgramme();
                    break;
                }
                os.close();
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setState(ProgrammeState.FINISHED);
    }
}
