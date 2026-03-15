package com.user404_.infinitehomes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class HomeData implements ConfigurationSerializable {
    private Location location;
    private Material icon;

    public HomeData(Location location, Material icon) {
        this.location = location;
        this.icon = icon;
    }

    public HomeData(Map<String, Object> map) {
        this.location = (Location) map.get("location");
        this.icon = Material.valueOf((String) map.get("icon"));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("location", location);
        map.put("icon", icon.name());
        return map;
    }

    public Location getLocation() { return location; }
    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }
}