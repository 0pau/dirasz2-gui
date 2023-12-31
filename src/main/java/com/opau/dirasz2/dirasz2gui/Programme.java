package com.opau.dirasz2.dirasz2gui;

public class Programme {

    private final Type type = Type.UNDEFINED;
    private String title;
    private int length;

    public String toString() {
        return "{}";
    }

    public Type getType() {
        return type;
    }

    public enum Type {FILE,PLAYLIST,MACRO,MANUAL,UNDEFINED}

    public static Programme buildFromString(String s) {

        return null;
    }
}
