package com.opau.dirasz2.dirasz2gui;

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

}
