package net.aegui.enchantments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import net.aegui.Core;
import net.aegui.models.AdvancedEnchantment;

public class EnchantmentManager {

    private static List<AdvancedEnchantment> enchantments = new ArrayList<>();

    /**
     * 加载所有附魔
     */
    public static void loadEnchantments() {
        enchantments.clear();

        // 从插件的数据文件夹加载enchantments.yml
        File file = new File(Core.getInstance().getDataFolder(), "enchantments.yml");

        // 如果文件不存在，从资源文件夹复制
        if (!file.exists()) {
            Core.getInstance().saveResource("enchantments.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 遍历配置中的所有附魔
        for (String key : config.getKeys(false)) {
            ConfigurationSection enchantmentSection = config.getConfigurationSection(key);
            if (enchantmentSection == null) continue;

            AdvancedEnchantment enchantment = new AdvancedEnchantment(key);

            // 设置附魔属性
            enchantment.setDisplay(enchantmentSection.getString("display", key));
            enchantment.setDescription(enchantmentSection.getString("description", "No description available"));
            enchantment.setAppliesTo(enchantmentSection.getString("applies-to", "All items"));
            enchantment.setType(enchantmentSection.getString("type", "UNKNOWN"));
            enchantment.setGroup(enchantmentSection.getString("group", "DEFAULT"));

            // 加载等级配置
            ConfigurationSection levelsSection = enchantmentSection.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelStr : levelsSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelStr);
                        ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelStr);
                        if (levelSection == null) continue;

                        Map<String, Object> levelConfig = new HashMap<>();
                        levelConfig.put("chance", levelSection.get("chance"));
                        levelConfig.put("cooldown", levelSection.get("cooldown"));
                        levelConfig.put("effects", levelSection.getList("effects", new ArrayList<>()));
                        levelConfig.put("condition", levelSection.get("condition"));

                        enchantment.addLevel(level, levelConfig);
                    } catch (NumberFormatException e) {
                        Core.getInstance().getLogger().warning("Invalid level format for enchantment " + key + ": " + levelStr);
                    }
                }
            }

            enchantments.add(enchantment);
        }

        Core.getInstance().getLogger().info("Loaded " + enchantments.size() + " enchantments!");
    }

    /**
     * 获取所有附魔
     * @return 附魔列表
     */
    public static List<AdvancedEnchantment> getEnchantments() {
        return new ArrayList<>(enchantments);
    }

    /**
     * 根据名称获取附魔
     * @param name 附魔名称
     * @return 附魔对象，如果不存在则返回null
     */
    public static AdvancedEnchantment getEnchantment(String name) {
        return enchantments.stream()
                .filter(e -> e.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}