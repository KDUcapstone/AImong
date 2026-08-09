package com.aimong.backend.global.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

public final class KstDateUtils {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private KstDateUtils() {
    }

    public static LocalDate today() {
        return LocalDate.now(KST);
    }

    public static LocalDate currentWeekStart() {
        return today().with(DayOfWeek.MONDAY);
    }
}
