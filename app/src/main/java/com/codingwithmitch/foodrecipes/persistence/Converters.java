package com.codingwithmitch.foodrecipes.persistence;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class Converters {
    // class for converting lists into gson obj

    @TypeConverter
    public static String[] fromString(String value) {
        //convert gson to string arr
        Type listType = new TypeToken<String[]>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromArrayList(String[] list) {
        //convert array to gson
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }

}
