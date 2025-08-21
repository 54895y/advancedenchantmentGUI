package net.aegui;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.aegui.commands.AEGuiCommand;
import net.aegui.enchantments.EnchantmentManager;
import net.aegui.commands.ReloadCommand;
import net.aegui.listeners.ConfigWatcher;
import org.bukkit.scheduler.BukkitTask;

public class Core extends JavaPlugin {

    private static Core instance;
    private static FileConfiguration config;
    private static FileConfiguration messagesConfig;
    private static FileConfiguration groupsConfig;
    private static BukkitTask configWatcherTask;

    @Override
    public void onEnable() {
        instance = this;
        
        // 显示字符画
        showAsciiArt();
        
        // 保存默认配置
        saveDefaultConfig();
        config = getConfig();
        
        // 自动生成groups.yml文件
        saveGroupsFile();
        
        // 加载groups配置
        loadGroupsConfig();
        
        // 加载消息配置
        loadMessagesConfig();
        
        // 加载附魔数据
        EnchantmentManager.loadEnchantments();

        // 注册命令
        getCommand("aegui").setExecutor(new AEGuiCommand());
        getCommand("aegui").setTabCompleter(new AEGuiCommand());
        getCommand("aegui-reload").setExecutor(new ReloadCommand());

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new AEGuiCommand(), this);

        // 启动配置文件自动重载服务
        try {
            // 如果之前有任务在运行，先取消它
            if (configWatcherTask != null && !configWatcherTask.isCancelled()) {
                configWatcherTask.cancel();
            }
            configWatcherTask = Bukkit.getScheduler().runTaskAsynchronously(this, new ConfigWatcher(this));
        } catch (Exception e) {
            getLogger().warning("启动配置文件自动重载服务失败: " + e.getMessage());
        }

        getLogger().info("AdvancedEnchantmentsGUI 已成功启用！");
    }

    @Override
    public void onDisable() {
        // 取消配置监控任务
        if (configWatcherTask != null && !configWatcherTask.isCancelled()) {
            configWatcherTask.cancel();
        }
        
        // 清理静态变量，防止热加载时的内存泄漏
        AEGuiCommand.cleanup();
        
        getLogger().info("AdvancedEnchantmentsGUI 已禁用！");
    }
    
    @Override
    public void onLoad() {
        // 初始化静态变量
        instance = this;
        // 注意：不要在onLoad中加载配置文件，因为getDataFolder可能尚未准备好
    }

    public static Core getInstance() {
        return instance;
    }
    
    public static FileConfiguration getPluginConfig() {
        return config;
    }
    
    public static FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    /**
     * 获取groups配置
     */
    public static FileConfiguration getGroupsConfig() {
        return groupsConfig;
    }
    
    /**
     * 重新加载主配置文件并更新静态配置变量
     */
    public void reloadPluginConfig() {
        try {
            reloadConfig();
            config = getConfig();
            getLogger().info("主配置文件重新加载成功！");
        } catch (Exception e) {
            getLogger().severe("加载主配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载消息配置文件
     */
    public void loadMessagesConfig() {
        try {
            File messagesFile = new File(getDataFolder(), "message.yml");
            if (!messagesFile.exists()) {
                saveResource("message.yml", false);
                getLogger().info("成功生成message.yml配置文件！");
            }
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } catch (Exception e) {
            getLogger().severe("加载message.yml配置文件失败: " + e.getMessage());
            e.printStackTrace();
            // 创建一个空的配置对象作为后备
            messagesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "message.yml"));
        }
    }
    
    /**
     * 加载groups配置文件
     */
    public void loadGroupsConfig() {
        try {
            File groupsFile = new File(getDataFolder(), "groups.yml");
            if (!groupsFile.exists()) {
                saveResource("groups.yml", false);
                getLogger().info("成功生成groups.yml配置文件！");
            }
            groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
        } catch (Exception e) {
            getLogger().severe("加载groups.yml配置文件失败: " + e.getMessage());
            e.printStackTrace();
            // 创建一个空的配置对象作为后备
            groupsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "groups.yml"));
        }
    }
    
    /**
     * 获取配置的消息，如有后备则返回后备消息
     * @param path 消息路径
     * @param fallback 后备消息
     * @return 配置的消息或后备消息
     */
    public static String getMessage(String path, String fallback) {
        if (messagesConfig != null && messagesConfig.contains(path)) {
            return messagesConfig.getString(path);
        }
        return fallback;
    }
    
    /**
     * 显示ASCII字符画
     */
    private void showAsciiArt() {
        getLogger().info("==========[高级附魔GUI]==========");
        getLogger().info("   版本: " + getDescription().getVersion());
        getLogger().info("   作者: 54895");
        getLogger().info("===============================");
    }
    
    /**
     * 自动生成groups.yml文件
     */
    private void saveGroupsFile() {
        try {
            // 检查groups.yml文件是否存在
            File groupsFile = new File(getDataFolder(), "groups.yml");
            if (!groupsFile.exists()) {
                // 如果不存在，从资源文件夹保存默认的groups.yml
                saveResource("groups.yml", false);
                getLogger().info("成功生成groups.yml配置文件！");
            }
        } catch (Exception e) {
            getLogger().severe("生成groups.yml配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}