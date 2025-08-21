package net.aegui.models;

import java.util.HashMap;
import java.util.Map;

public class AdvancedEnchantment {

    private String name;
    private String display;
    private String description;
    private String appliesTo;
    private String type;
    private String group;
    private Map<Integer, Map<String, Object>> levels;

    public AdvancedEnchantment(String name) {
        this.name = name;
        this.levels = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Map<Integer, Map<String, Object>> getLevels() {
        return levels;
    }

    public void setLevels(Map<Integer, Map<String, Object>> levels) {
        this.levels = levels;
    }

    public void addLevel(int level, Map<String, Object> config) {
        this.levels.put(level, config);
    }

    @Override
    public String toString() {
        return "AdvancedEnchantment{" +
                "name='" + name + '\'' +
                ", display='" + display + '\'' +
                ", appliesTo='" + appliesTo + '\'' +
                ", type='" + type + '\'' +
                ", group='" + group + '\'' +
                ", levels=" + levels +
                '}';
    }
}