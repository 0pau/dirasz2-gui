package com.opau.dirasz2.dirasz2gui;

import java.nio.ByteBuffer;
import java.util.Calendar;

public class Utils {

    public static int getTime() {
        Calendar calendar = Calendar.getInstance();
         return Utils.timeToInt(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    public static int timeToInt(int h, int m, int s) {
        return 3600*h + 60*m + s;
    }

    public static String formatTimeInt(int date, boolean showSeconds) {
        int h = 0;
        int m = 0;
        int s = date;

        if (s > 3600) {
            h = s/3600;
            s -= h*3600;
        }
        if (s >= 60) {
            m = s/60;
            s -= m*60;
        }

        if (showSeconds) {
            return String.format("%02d:%02d:%02d", h,m,s);
        } else {
            return String.format("%02d:%02d", h,m);
        }

    }

    public static int[] getHMSFromTimeInt(int t) {
        int h = 0;
        int m = 0;
        int s = t;

        if (s > 3600) {
            h = s/3600;
            s -= h*3600;
        }
        if (s >= 60) {
            m = s/60;
            s -= m*60;
        }

        return new int[] {h,m,s};
    }

    public static String formatSize(int bytes) {

        double b2 = bytes;

        String suffix = "B";
        if (bytes >= 1024 && bytes < Math.pow(1024, 2)) {
            suffix = "KB";
            b2 = b2/1024;
        } else if (bytes < Math.pow(1024,3)) {
            suffix = "MB";
            b2 = b2/Math.pow(1024,2);
        } else if (bytes < Math.pow(1024,4)) {
            suffix = "GB";
            b2 = b2/Math.pow(1024,3);
        }

        return String.format("%.1f %s", b2, suffix);

    }

    public static int getCurrentTimeInt() {
        Calendar c = Calendar.getInstance();
        return timeToInt(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
    }

}
