package net.aegui.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.aegui.Core;
import net.aegui.enchantments.EnchantmentManager;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!(sender instanceof Player) || sender.hasPermission("advancedenchantmentsgui.reload")) {
            try {
                // 重新加载配置文件并更新静态配置变量
                Core.getInstance().reloadPluginConfig();
                
                // 重新加载groups配置文件
                Core.getInstance().loadGroupsConfig();
                
                // 重新加载消息配置文件
                Core.getInstance().loadMessagesConfig();
                
                // 重新加载附魔数据
                EnchantmentManager.loadEnchantments();
                
                // 更新GUI标题
                AEGuiCommand.updateGuiTitle();
                
                // 发送成功消息
                String reloadSuccessMsg = Core.getMessage("reload.success", "§a成功重载了附魔配置！");
                sender.sendMessage(reloadSuccessMsg);
                Core.getInstance().getLogger().info("Enchantments configuration reloaded successfully!");
            } catch (Exception e) {
                // 发送失败消息
                String reloadFailedMsg = Core.getMessage("reload.failed", "§c重载附魔配置失败，请检查配置文件！");
                sender.sendMessage(reloadFailedMsg);
                Core.getInstance().getLogger().severe("Failed to reload enchantments configuration: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            sender.sendMessage(Core.getMessage("commands.no_permission", "§c你没有权限使用这个命令！"));
        }
        
        return true;
    }
}