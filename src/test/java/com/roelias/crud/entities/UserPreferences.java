package com.roelias.crud.entities;

import java.util.Map;

public class UserPreferences {
    private String theme;
    private boolean notifications;
    private Map<String, String> settings;

    public UserPreferences(String theme, boolean notifications, Map<String, String> settings) {
        this.theme = theme;
        this.notifications = notifications;
        this.settings = settings;
    }

    public UserPreferences() {
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}
