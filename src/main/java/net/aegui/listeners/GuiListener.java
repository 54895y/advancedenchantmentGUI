package net.aegui.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 这个类现在是冗余的，因为事件监听逻辑已经在AEGuiCommand类中实现
 * 保留它是为了将来可能的扩展需求
 */
public class GuiListener implements Listener {
    
    /**
     * 防止物品被拿取的监听器
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 事件处理逻辑已在AEGuiCommand中实现
        // 此方法保留为占位符，用于将来的扩展
    }
}