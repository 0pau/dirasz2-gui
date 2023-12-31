package com.opau.dirasz2.dirasz2gui;

import java.util.ArrayList;

public class PlaylistProgramme extends Programme {

    private final Type type = Type.PLAYLIST;
    private ArrayList<FileProgramme> items;
    private int playCount;
    private Order order;

    public enum Order {LINEAR,SHUFFLE}
}
