package com.queuectl.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final Map<String, String> config = new HashMap<>();

    private ConfigManager() {
        loadDefaults();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    private void loadDefaults() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (in == null) return;
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            Gson g = new Gson();
            JsonObject obj = g.fromJson(reader, JsonObject.class);
            for (String k : obj.keySet()) {
                config.put(k, obj.get(k).getAsString());
            }
        } catch (Exception e) {
            System.err.println("Failed to load default config: " + e.getMessage());
        }
    }

    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String v = config.get(key);
        if (v == null) return defaultValue;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultValue; }
    }

    public void set(String key, String value) { config.put(key, value); }
}
