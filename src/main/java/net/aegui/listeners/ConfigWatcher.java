package net.aegui.listeners;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import net.aegui.Core;
import net.aegui.commands.AEGuiCommand;
import net.aegui.enchantments.EnchantmentManager;

/**
 * 配置文件自动重载监听器
 * 该类使用Java的WatchService API监听配置文件变化并自动重载
 */
public class ConfigWatcher implements Runnable {
    
    private final Plugin plugin;
    private final WatchService watchService;
    private final Path configDir;
    private final Map<String, Long> lastModifiedTimes = new HashMap<>();
    private final long debounceTime = 1000; // 防抖动时间（毫秒）
    
    public ConfigWatcher(Plugin plugin) {
        this.plugin = plugin;
        
        try {
            // 初始化WatchService
            this.watchService = FileSystems.getDefault().newWatchService();
            
            // 获取配置文件所在目录
            this.configDir = Paths.get(plugin.getDataFolder().getAbsolutePath());
            
            // 注册WatchService以监听目录变化
            this.configDir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            );
            
            plugin.getLogger().info("配置文件监控已启动，将自动重载修改的配置文件...");
        } catch (Exception e) {
            throw new RuntimeException("初始化配置文件监控失败", e);
        }
    }
    
    @Override
    public void run() {
        while (plugin.isEnabled()) {
            try {
                // 等待文件变化事件，超时时间设为1秒
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                
                if (key == null) {
                    continue;
                }
                
                // 处理所有事件
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // 处理溢出事件
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    // 获取变化的文件名
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    String fullPath = configDir.resolve(fileName).toString();
                    
                    // 检查是否为我们关心的配置文件
                    if (isConfigFile(fileName.toString())) {
                        // 防抖动处理，避免频繁重载
                        long currentTime = System.currentTimeMillis();
                        Long lastTime = lastModifiedTimes.get(fullPath);
                        
                        if (lastTime == null || currentTime - lastTime > debounceTime) {
                            // 更新最后修改时间
                            lastModifiedTimes.put(fullPath, currentTime);
                            
                            // 在主线程中重新加载配置
                            Bukkit.getScheduler().runTask(plugin, () -> reloadConfig(fileName.toString()));
                        }
                    }
                }
                
                // 重置key，以便继续接收事件
                boolean valid = key.reset();
                if (!valid) {
                    break; // 监听的目录不再有效
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("配置文件监控发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 检查文件是否为我们关心的配置文件
     */
    private boolean isConfigFile(String fileName) {
        return fileName.equals("config.yml") || 
               fileName.equals("message.yml") || 
               fileName.equals("groups.yml");
    }
    
    /**
     * 根据文件名重新加载对应的配置
     */
    private void reloadConfig(String fileName) {
        try {
            Core core = Core.getInstance();
            
            if (fileName.equals("config.yml")) {
                core.reloadPluginConfig();
                AEGuiCommand.updateGuiTitle(); // 更新GUI标题
                plugin.getLogger().info("自动重载配置文件: " + fileName);
                String message = Core.getMessage("watchdog.config_reloaded", "配置文件 %file% 已自动重载");
                message = message.replace("%file%", fileName);
                sendReloadNotification(message);
                
                // 配置重载后，重新打开玩家的GUI
                reopenPlayerGUIs();
            } else if (fileName.equals("message.yml")) {
                core.loadMessagesConfig();
                plugin.getLogger().info("自动重载配置文件: " + fileName);
                String message = Core.getMessage("watchdog.message_reloaded", "消息配置文件 %file% 已自动重载");
                message = message.replace("%file%", fileName);
                sendReloadNotification(message);
            } else if (fileName.equals("groups.yml")) {
                core.loadGroupsConfig();
                EnchantmentManager.loadEnchantments(); // 重新加载附魔数据
                plugin.getLogger().info("自动重载配置文件: " + fileName);
                String message = Core.getMessage("watchdog.groups_reloaded", "分组配置文件 %file% 已自动重载，附魔数据已更新");
                message = message.replace("%file%", fileName);
                sendReloadNotification(message);
                
                // 分组数据重载后，重新打开玩家的GUI
                reopenPlayerGUIs();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("自动重载配置文件" + fileName + "失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 重新打开所有玩家的GUI
     * 当配置或分组数据重载后，需要关闭并重新打开玩家的GUI以应用新的配置
     */
    private void reopenPlayerGUIs() {
        try {
            // 检查AEGuiCommand类是否有跟踪玩家GUI的方法或字段
            // 通过反射获取搜索查询和当前页码信息
            java.lang.reflect.Field searchQueriesField = null;
            java.lang.reflect.Field currentPagesField = null;
            
            try {
                searchQueriesField = Class.forName("net.aegui.commands.AEGuiCommand").getDeclaredField("searchQueries");
                searchQueriesField.setAccessible(true);
                
                currentPagesField = Class.forName("net.aegui.commands.AEGuiCommand").getDeclaredField("currentPages");
                currentPagesField.setAccessible(true);
            } catch (Exception e) {
                plugin.getLogger().warning("无法获取AEGuiCommand的字段信息: " + e.getMessage());
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<org.bukkit.entity.Player, String> searchQueries = (Map<org.bukkit.entity.Player, String>) searchQueriesField.get(null);
            
            // 获取所有在线玩家
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                // 检查玩家是否正在查看附魔GUI
                String playerGuiTitle = player.getOpenInventory().getTitle();
                String currentGuiTitle = AEGuiCommand.getGuiTitle();
                
                if (playerGuiTitle.contains(currentGuiTitle)) {
                    // 关闭当前GUI
                    player.closeInventory();
                    
                    // 重新打开GUI，保留搜索查询
                    String searchQuery = searchQueries.getOrDefault(player, "");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        AEGuiCommand.openEnchantmentGui(player, searchQuery);
                    }, 1L); // 延迟1tick再打开，确保关闭操作完成
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("重新打开玩家GUI时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送配置重载通知给控制台和有权限的在线玩家
     */
    private void sendReloadNotification(String message) {
        // 获取配置的前缀
        String consolePrefix = Core.getMessage("watchdog.console_prefix", "§e[AEGUI] §7");
        String playerPrefix = Core.getMessage("watchdog.player_prefix", "§e[AEGUI] §7");
        
        // 向控制台发送消息
        Bukkit.getConsoleSender().sendMessage(consolePrefix + message);
        
        // 向有权限的在线玩家发送消息
        String permission = "aegui.admin";
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission(permission)) {
                player.sendMessage(playerPrefix + message);
            }
        });
    }
}