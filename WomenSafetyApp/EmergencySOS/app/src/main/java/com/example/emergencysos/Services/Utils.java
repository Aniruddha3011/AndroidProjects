package com.example.emergencysos.Services;

import android.content.SharedPreferences;

import com.example.emergencysos.Models.EmergencyContact;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<EmergencyContact> getSavedContacts(SharedPreferences prefs) {
        String json = prefs.getString("contact_list", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
        return new Gson().fromJson(json, type);
    }
}
