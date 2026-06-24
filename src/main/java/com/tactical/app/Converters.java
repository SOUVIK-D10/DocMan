package com.tactical.app;

import androidx.room.TypeConverter;
import java.time.LocalDateTime;

public class Converters {

    @TypeConverter
    public static LocalDateTime fromString(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }

    @TypeConverter
    public static String dateToString(LocalDateTime date) {
        return date == null ? null : date.toString();
    }
}