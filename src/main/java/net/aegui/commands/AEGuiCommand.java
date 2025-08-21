package net.aegui.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.aegui.Core;
import net.aegui.enchantments.EnchantmentManager;
import net.aegui.models.AdvancedEnchantment;

public class AEGuiCommand implements CommandExecutor, TabCompleter, Listener {

    private static final int GUI_SIZE = 54; // 最大箱子大小
    private static String GUI_TITLE = "高级附魔"; // 默认GUI标题
    private static final Map<Player, String> searchQueries = new HashMap<>(); // 存储玩家的搜索查询
    private static final Map<Player, Integer> currentPages = new HashMap<>(); // 存储玩家当前的页码
    private static final Set<Player> awaitingSearchInput = new HashSet<>(); // 跟踪等待搜索输入的玩家
    
    // 获取配置的GUI标题
    public static String getGuiTitle() {
        if (Core.getInstance() != null && Core.getPluginConfig() != null) {
            return Core.getPluginConfig().getString("gui.title", "高级附魔");
        }
        return GUI_TITLE;
    }
    
    // 更新GUI标题
    public static void updateGuiTitle() {
        GUI_TITLE = getGuiTitle();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Core.getMessage("commands.player_only", "§c只有玩家才可以使用这些命令！"));
            return true;
        }

        Player player = (Player) sender;

        // 处理不同的子命令
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("open"))) {
            // 打开GUI，不进行搜索
            searchQueries.put(player, "");
            openEnchantmentGui(player, "");
        } else if (args.length >= 1) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "help":
                    showHelp(player);
                    break;
                case "open":
                    // 已经在上面处理了
                    break;
                case "search":
                    if (args.length >= 2) {
                        // 提取搜索关键词（从第二个参数开始）
                        String searchQuery = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                        searchQueries.put(player, searchQuery);
                        openEnchantmentGui(player, searchQuery);
                    } else {
                        player.sendMessage(Core.getMessage("commands.search_usage", "§c请输入搜索关键词！用法: /aegui search <关键词>"));
                    }
                    break;
                case "reload":
                    // 检查权限
                    if (player.hasPermission("advancedenchantmentsgui.reload")) {
                        try {
                            // 调用ReloadCommand中的重载逻辑
                            ReloadCommand reloadCommand = new ReloadCommand();
                            reloadCommand.onCommand(sender, command, label, new String[0]);
                        } catch (Exception e) {
                            player.sendMessage(Core.getMessage("reload.plugin_failed", "§c重载插件配置失败！"));
                            e.printStackTrace();
                        }
                    } else {
                        player.sendMessage(Core.getMessage("commands.no_permission", "§c你没有权限使用这个命令！"));
                    }
                    break;
                default:
                    // 默认行为：将整个参数列表作为搜索关键词
                    String searchQuery = String.join(" ", args);
                    searchQueries.put(player, searchQuery);
                    openEnchantmentGui(player, searchQuery);
                    break;
            }
        }

        return true;
    }

    /**
     * 显示帮助信息
     */
    private void showHelp(Player player) {
        player.sendMessage(Core.getMessage("commands.help.header", ""));
        player.sendMessage(Core.getMessage("commands.help.title", "§e§l====== AdvancedEnchantmentsGUI 帮助 ======"));
        player.sendMessage(Core.getMessage("commands.help.command_help", "§a/aegui help §7- 显示此帮助信息"));
        player.sendMessage(Core.getMessage("commands.help.command_open", "§a/aegui open §7- 打开附魔GUI界面"));
        player.sendMessage(Core.getMessage("commands.help.command_search", "§a/aegui search <关键词> §7- 搜索特定附魔"));
        player.sendMessage(Core.getMessage("commands.help.command_reload", "§a/aegui reload §7- 重载插件配置（需要权限）"));
        player.sendMessage(Core.getMessage("commands.help.footer", "§e§l======================================="));
        player.sendMessage(Core.getMessage("commands.help.footer_space", ""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 如果是第一个参数，提供子命令补全
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            // 子命令列表
            String[] subCommands = {"help", "open", "search", "reload"};
            
            // 过滤匹配输入的子命令
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(input)) {
                    completions.add(subCommand);
                }
            }
            
            // 如果用户没有输入特定子命令，同时提供附魔名称补全
            if (completions.isEmpty()) {
                // 获取所有附魔名称用于自动补全
                List<String> enchantmentNames = EnchantmentManager.getEnchantments().stream()
                        .map(AdvancedEnchantment::getName)
                        .collect(Collectors.toList());
                
                // 过滤匹配前缀的附魔名称
                completions = enchantmentNames.stream()
                        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                        .collect(Collectors.toList());
            }
        } 
        // 如果是search子命令的后续参数，提供附魔名称补全
        else if (args.length >= 2 && args[0].equalsIgnoreCase("search")) {
            // 获取所有附魔名称用于自动补全
            List<String> enchantmentNames = EnchantmentManager.getEnchantments().stream()
                    .map(AdvancedEnchantment::getName)
                    .collect(Collectors.toList());
            
            // 当前输入的前缀（从第二个参数开始）
            String prefix = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
            
            // 过滤匹配前缀的附魔名称
            completions = enchantmentNames.stream()
                    .filter(name -> name.toLowerCase().contains(prefix))
                    .collect(Collectors.toList());
        }
        
        return completions;
    }

    /**
     * 打开附魔GUI
     * @param player 玩家
     * @param searchQuery 搜索查询
     */
    public static void openEnchantmentGui(Player player, String searchQuery) {
        // 更新GUI标题
        updateGuiTitle();
        
        // 如果是新的搜索查询，重置页码
        if (!searchQuery.equals(searchQueries.get(player))) {
            currentPages.put(player, 0);
        }
        
        // 获取当前页码
        int page = currentPages.getOrDefault(player, 0);
        
        // 创建GUI
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, getGuiTitle() + 
                (searchQuery.isEmpty() ? "" : " - 搜索: " + searchQuery) + 
                " (第 " + (page + 1) + "页)");

        // 获取所有附魔并应用搜索过滤
        List<AdvancedEnchantment> enchantments = EnchantmentManager.getEnchantments().stream()
                .filter(enchantment -> searchQuery.isEmpty() || 
                        enchantment.getName().toLowerCase().contains(searchQuery.toLowerCase()) || 
                        enchantment.getDisplay().toLowerCase().contains(searchQuery.toLowerCase()) || 
                        enchantment.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());

        // 填充GUI
        fillGuiWithEnchantments(gui, enchantments, page);

        // 添加搜索提示物品
        addSearchHintItem(gui);
        
        // 添加翻页按钮
        addPaginationButtons(gui, enchantments, page, player, searchQuery);

        // 打开GUI
        player.openInventory(gui);
    }

    /**
     * 用附魔填充GUI
     * @param gui GUI
     * @param enchantments 附魔列表
     * @param page 当前页码
     */
    private static void fillGuiWithEnchantments(Inventory gui, List<AdvancedEnchantment> enchantments, int page) {
        // 从配置文件获取每页显示的物品数量
        int itemsPerPage = 45; // 默认值
        if (Core.getInstance() != null && Core.getPluginConfig() != null) {
            itemsPerPage = Core.getPluginConfig().getInt("gui.items_per_page", 45);
        }
        
        // 计算当前页的起始和结束索引
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, enchantments.size());
        
        // 清除GUI中除了按钮位置外的所有物品
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i != getButtonSlot("prev_button", 48) && i != getButtonSlot("next_button", 50) && i != getButtonSlot("search_button", 49)) {
                gui.setItem(i, null);
            }
        }
        
        // 填充当前页的附魔
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            // 跳过按钮位置
            if (slot == getButtonSlot("prev_button", 48) || 
                slot == getButtonSlot("next_button", 50) || 
                slot == getButtonSlot("search_button", 49)) {
                slot++;
            }
            
            if (slot >= GUI_SIZE) break; // 防止超出GUI大小
            
            AdvancedEnchantment enchantment = enchantments.get(i);
            // 创建附魔物品
            ItemStack item = createEnchantmentItem(enchantment);
            gui.setItem(slot++, item);
        }

        // 填充空槽位
        for (int i = 0; i < GUI_SIZE; i++) {
            if (gui.getItem(i) == null && 
                i != getButtonSlot("prev_button", 48) && 
                i != getButtonSlot("next_button", 50) && 
                i != getButtonSlot("search_button", 49)) {
                // 在Minecraft 1.12.2中，使用STAINED_GLASS_PANE并设置数据值7（灰色）
                ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)7);
                ItemMeta meta = filler.getItemMeta();
                meta.setDisplayName("");
                filler.setItemMeta(meta);
                gui.setItem(i, filler);
            }
        }
    }
    
    /**
     * 获取按钮的槽位
     * @param buttonType 按钮类型
     * @param defaultValue 默认值
     * @return 槽位索引
     */
    private static int getButtonSlot(String buttonType, int defaultValue) {
        if (Core.getInstance() != null && Core.getPluginConfig() != null) {
            return Core.getPluginConfig().getInt("gui." + buttonType + ".slot", defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * 添加翻页按钮
     * @param gui GUI
     * @param enchantments 附魔列表
     * @param page 当前页码
     * @param player 玩家
     * @param searchQuery 搜索查询
     */
    private static void addPaginationButtons(Inventory gui, List<AdvancedEnchantment> enchantments, int page, Player player, String searchQuery) {
        // 从配置文件获取每页显示的物品数量
        int itemsPerPage = 45; // 默认值
        if (Core.getInstance() != null && Core.getPluginConfig() != null) {
            itemsPerPage = Core.getPluginConfig().getInt("gui.items_per_page", 45);
        }
        
        // 计算总页数
        int totalPages = (int) Math.ceil((double) enchantments.size() / itemsPerPage);
        
        // 获取按钮配置
        Map<String, Object> prevButtonConfig = getButtonConfig("prev_button");
        Map<String, Object> nextButtonConfig = getButtonConfig("next_button");
        
        // 创建上一页按钮
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.valueOf(prevButtonConfig.get("material").toString()));
            ItemMeta meta = prevButton.getItemMeta();
            meta.setDisplayName(prevButtonConfig.get("name").toString());
            meta.setLore((List<String>) prevButtonConfig.get("lore"));
            prevButton.setItemMeta(meta);
            gui.setItem(getButtonSlot("prev_button", 48), prevButton);
        }
        
        // 创建下一页按钮
        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.valueOf(nextButtonConfig.get("material").toString()));
            ItemMeta meta = nextButton.getItemMeta();
            meta.setDisplayName(nextButtonConfig.get("name").toString());
            meta.setLore((List<String>) nextButtonConfig.get("lore"));
            nextButton.setItemMeta(meta);
            gui.setItem(getButtonSlot("next_button", 50), nextButton);
        }
    }
    
    /**
     * 获取按钮配置
     * @param buttonType 按钮类型
     * @return 按钮配置
     */
    private static Map<String, Object> getButtonConfig(String buttonType) {
        Map<String, Object> config = new HashMap<>();
        
        // 默认配置
        config.put("material", "ARROW");
        config.put("name", buttonType.equals("prev_button") ? "§a上一页" : "§a下一页");
        List<String> lore = new ArrayList<>();
        lore.add(buttonType.equals("prev_button") ? "§7点击前往上一页" : "§7点击前往下一页");
        config.put("lore", lore);
        
        // 从配置文件获取配置
        if (Core.getInstance() != null && Core.getPluginConfig() != null && Core.getPluginConfig().contains("gui." + buttonType)) {
            org.bukkit.configuration.ConfigurationSection section = Core.getPluginConfig().getConfigurationSection("gui." + buttonType);
            if (section != null) {
                if (section.contains("material")) {
                    config.put("material", section.getString("material"));
                }
                if (section.contains("name")) {
                    config.put("name", section.getString("name"));
                }
                if (section.contains("lore")) {
                    config.put("lore", section.getStringList("lore"));
                }
            }
        }
        
        return config;
    }

    /**
     * 创建附魔物品
     * @param enchantment 附魔
     * @return 物品栈
     */
    private static ItemStack createEnchantmentItem(AdvancedEnchantment enchantment) {
        // 获取附魔适用的物品类型来决定显示材质
        Material material = getMaterialForEnchantment(enchantment);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // 设置名称，并解析其中的变量
        String displayName = enchantment.getDisplay();
        
        // 解析%group-color%变量
        if (displayName.contains("%group-color%")) {
            String tempGroupColor = getGroupColor(enchantment.getGroup());
            displayName = displayName.replace("%group-color%", tempGroupColor);
        }
        
        meta.setDisplayName(displayName);

        // 从配置中读取显示文本
        final String appliesToText = Core.getPluginConfig().getString("display_text.applies_to", "Applies to: ");
        final String groupText = Core.getPluginConfig().getString("display_text.group", "Group: ");
        final String levelsText = Core.getPluginConfig().getString("display_text.levels", "Levels: ");
        final String chanceText = Core.getPluginConfig().getString("display_text.chance", "Chance: ");
        final String cooldownText = Core.getPluginConfig().getString("display_text.cooldown", "Cooldown: ");
        final String levelText = Core.getPluginConfig().getString("display_text.level", "Level ");
        
        // 获取附魔组信息
        final String enchantmentGroup = enchantment.getGroup();
        final String groupColor = getGroupColor(enchantmentGroup);
        final String groupName = getGroupName(enchantmentGroup);
        
        // 设置 lore
        final List<String> lore = new ArrayList<>();
        
        // 从配置中读取lore模板列表
        final FileConfiguration config = Core.getPluginConfig();
        final List<String> loreTemplateList = config.getStringList("lore_template");
        
        // 创建final引用以便在lambda中使用
        final AdvancedEnchantment finalEnchantment = enchantment;
        
        // 遍历lore模板列表，处理每一行
        for (String templateLine : loreTemplateList) {
            // 复制模板行，避免直接修改配置
            String processedLine = templateLine;
            
            // 将&符号转换为§符号
            processedLine = processedLine.replace('&', '§');
            
            // 替换通用变量
            processedLine = processedLine.replace("%description%", finalEnchantment.getDescription().replace("\n", "§7\n"));
            processedLine = processedLine.replace("%applies_to%", "§l" + finalEnchantment.getAppliesTo());
            processedLine = processedLine.replace("%group_name%", groupName);
            
            // 添加处理后的行到lore列表
            // 特殊处理：如果是等级信息标题行，先添加到lore，然后会在下边单独处理等级信息
            if (!processedLine.contains("%level_text%") && !processedLine.contains("%level%") && 
                !processedLine.contains("%chance%") && !processedLine.contains("%cooldown%")) {
                lore.add(processedLine);
            }
        }
        
        // 6. 添加所有级别的信息
        // 先从lore模板列表中找出等级信息相关的模板行
        List<String> levelInfoTemplates = new ArrayList<>();
        for (String templateLine : loreTemplateList) {
            if (templateLine.contains("%level_text%") || templateLine.contains("%level%") ||
                templateLine.contains("%chance%") || templateLine.contains("%cooldown%")) {
                levelInfoTemplates.add(templateLine);
            }
        }
        
        // 处理每个等级
        finalEnchantment.getLevels().forEach((level, levelConfig) -> {{
            // 如果chance未设置或为null，默认显示100%
            final String chance = levelConfig.containsKey("chance") && levelConfig.get("chance") != null ? "§e" + chanceText + "§f" + levelConfig.get("chance") + "% §7" : "§e" + chanceText + "§f100% §7";
            
            // 如果cooldown未设置，显示为0；如果设置了则验证是否为有效数值
            final String cooldown = levelConfig.containsKey("cooldown") && levelConfig.get("cooldown") != null && 
                                   levelConfig.get("cooldown").toString().matches("\\d+") ? 
                                   "§e" + cooldownText + "§f" + levelConfig.get("cooldown") + "s" : 
                                   "§e" + cooldownText + "§f0s";
            
            // 处理等级信息模板行
            for (String levelTemplate : levelInfoTemplates) {
                String processedLine = levelTemplate;
                
                // 将&符号转换为§符号
                processedLine = processedLine.replace('&', '§');
                
                // 替换等级相关变量
                processedLine = processedLine.replace("%level_text%", levelText);
                processedLine = processedLine.replace("%level%", level.toString());
                processedLine = processedLine.replace("%chance%", chance);
                processedLine = processedLine.replace("%cooldown%", cooldown);
                
                // 添加到lore列表
                lore.add(processedLine);
            }
        }});

        meta.setLore(lore);

        // 添加发光效果
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);

        return item;
    }
    
    /**
     * 添加lore模板到lore列表中，并替换变量
     * @param lore 要添加到的lore列表
     * @param config 配置文件
     * @param configPath 配置路径
     * @param variables 要替换的变量映射
     */
    private static void addLoreTemplate(List<String> lore, FileConfiguration config, String configPath, Map<String, String> variables) {
        // 检查配置是否存在
        if (!config.contains(configPath)) {
            return;
        }
        
        // 获取模板行列表
        List<String> templateLines = config.getStringList(configPath);
        
        // 遍历每一行并替换变量
        for (String templateLine : templateLines) {
            String processedLine = templateLine;
            
            // 将&符号转换为§符号
            processedLine = processedLine.replace('&', '§');
            
            // 替换变量
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                processedLine = processedLine.replace(entry.getKey(), entry.getValue());
            }
            
            // 添加到lore列表
            lore.add(processedLine);
        }
    }
    
    /**
     * 根据组名获取显示名称
     * @param groupKey 组的键名
     * @return 显示名称
     */
    private static String getGroupName(String groupKey) {
        try {
            // 使用Core中的groups配置，这样在重载时也能获取到最新的配置
            FileConfiguration groupsConfig = Core.getGroupsConfig();
            if (groupsConfig != null && groupsConfig.contains("groups." + groupKey + ".group-name")) {
                return groupsConfig.getString("groups." + groupKey + ".group-name", groupKey);
            }
        } catch (Exception e) {
            Core.getInstance().getLogger().warning("Failed to load group name for " + groupKey + ": " + e.getMessage());
        }
        return groupKey;
    }
    
    /**
     * 根据组名获取颜色代码
     * @param groupKey 组的键名
     * @return 颜色代码
     */
    private static String getGroupColor(String groupKey) {
        try {
            // 使用Core中的groups配置，这样在重载时也能获取到最新的配置
            FileConfiguration groupsConfig = Core.getGroupsConfig();
            if (groupsConfig != null && groupsConfig.contains("groups." + groupKey + ".global-color")) {
                String colorCode = groupsConfig.getString("groups." + groupKey + ".global-color", "§f");
                return colorCode;
            }
        } catch (Exception e) {
            Core.getInstance().getLogger().warning("Failed to load group color for " + groupKey + ": " + e.getMessage());
        }
        return "§f";
    }

    /**
     * 获取附魔适用的材质
     * @param enchantment 附魔
     * @return 材质
     */
    private static Material getMaterialForEnchantment(AdvancedEnchantment enchantment) {
        String appliesTo = enchantment.getAppliesTo().toLowerCase();

        if (appliesTo.contains("sword")) return Material.DIAMOND_SWORD;
        if (appliesTo.contains("bow")) return Material.BOW;
        if (appliesTo.contains("axe")) return Material.DIAMOND_AXE;
        if (appliesTo.contains("pickaxe")) return Material.DIAMOND_PICKAXE;
        if (appliesTo.contains("shovel")) return Material.DIAMOND_SPADE; // 在1.12.2中是SPADE而不是SHOVEL
        if (appliesTo.contains("hoe")) return Material.DIAMOND_HOE;
        if (appliesTo.contains("helmet")) return Material.DIAMOND_HELMET;
        if (appliesTo.contains("chestplate")) return Material.DIAMOND_CHESTPLATE;
        if (appliesTo.contains("leggings")) return Material.DIAMOND_LEGGINGS;
        if (appliesTo.contains("boots")) return Material.DIAMOND_BOOTS;
        if (appliesTo.contains("trident")) return Material.IRON_SWORD; // 1.12.2中没有TRIDENT，使用IRON_SWORD替代
        if (appliesTo.contains("fishing")) return Material.FISHING_ROD;

        return Material.ENCHANTED_BOOK; // 默认使用附魔书
    }

    /**
     * 填充空槽位
     * @param gui GUI
     * @param startSlot 起始槽位
     */
    private static void fillEmptySlots(Inventory gui, int startSlot) {
        // 在Minecraft 1.12.2中，使用STAINED_GLASS_PANE并设置数据值7（灰色）
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)7);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName("");
        filler.setItemMeta(meta);

        for (int i = startSlot; i < GUI_SIZE; i++) {
            gui.setItem(i, filler);
        }
    }

    /**
     * 添加搜索提示物品
     * @param gui GUI
     */
    private static void addSearchHintItem(Inventory gui) {
        // 从配置文件获取按钮配置
        Map<String, Object> config = new HashMap<>();
        config.put("material", "COMPASS");
        config.put("name", "§a搜索附魔");
        List<String> lore = new ArrayList<>();
        lore.add("§7输入 /aegui <关键词> 来筛选附魔");
        config.put("lore", lore);
        
        if (Core.getInstance() != null && Core.getPluginConfig() != null && Core.getPluginConfig().contains("gui.search_button")) {
            org.bukkit.configuration.ConfigurationSection section = Core.getPluginConfig().getConfigurationSection("gui.search_button");
            if (section != null) {
                if (section.contains("material")) {
                    config.put("material", section.getString("material"));
                }
                if (section.contains("name")) {
                    config.put("name", section.getString("name"));
                }
                if (section.contains("lore")) {
                    config.put("lore", section.getStringList("lore"));
                }
            }
        }
        
        ItemStack searchHint = new ItemStack(Material.valueOf(config.get("material").toString()));
        ItemMeta meta = searchHint.getItemMeta();
        meta.setDisplayName(config.get("name").toString());
        meta.setLore((List<String>) config.get("lore"));

        searchHint.setItemMeta(meta);

        gui.setItem(getButtonSlot("search_button", 49), searchHint);
    }

    /**
     * 处理GUI点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(getGuiTitle())) {
            event.setCancelled(true); // 防止拿取物品
            
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            
            // 检查是否点击了按钮
            if (clickedItem != null && slot < GUI_SIZE) {
                String searchQuery = searchQueries.getOrDefault(player, "");
                
                // 处理上一页按钮点击
                if (slot == getButtonSlot("prev_button", 48)) {
                    int currentPage = currentPages.getOrDefault(player, 0);
                    if (currentPage > 0) {
                        currentPages.put(player, currentPage - 1);
                        openEnchantmentGui(player, searchQuery);
                    }
                }
                
                // 处理下一页按钮点击
                else if (slot == getButtonSlot("next_button", 50)) {
                    int currentPage = currentPages.getOrDefault(player, 0);
                    
                    // 计算总页数
                    List<AdvancedEnchantment> enchantments = EnchantmentManager.getEnchantments().stream()
                            .filter(enchantment -> searchQuery.isEmpty() || 
                                    enchantment.getName().toLowerCase().contains(searchQuery.toLowerCase()) || 
                                    enchantment.getDisplay().toLowerCase().contains(searchQuery.toLowerCase()) || 
                                    enchantment.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                            .collect(Collectors.toList());
                    
                    int itemsPerPage = 45; // 默认值
                    if (Core.getInstance() != null && Core.getPluginConfig() != null) {
                        itemsPerPage = Core.getPluginConfig().getInt("gui.items_per_page", 45);
                    }
                    
                    int totalPages = (int) Math.ceil((double) enchantments.size() / itemsPerPage);
                    
                    if (currentPage < totalPages - 1) {
                        currentPages.put(player, currentPage + 1);
                        openEnchantmentGui(player, searchQuery);
                    }
                }
                
                // 处理搜索按钮点击
                else if (slot == getButtonSlot("search_button", 49)) {
                    // 关闭GUI并提示用户输入关键词
                    player.closeInventory();
                    player.sendMessage(Core.getMessage("commands.search_prompt", "§a请输入搜索关键词，然后发送聊天消息！"));
                    // 将玩家添加到等待搜索输入的集合中
                    awaitingSearchInput.add(player);
                }
            }
        }
    }

    /**
     * 处理玩家聊天事件用于搜索
     */
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (awaitingSearchInput.contains(player)) {
            // 取消聊天事件
            event.setCancelled(true);

            // 获取搜索查询
            String searchQuery = event.getMessage();
            searchQueries.put(player, searchQuery);
            
            // 从等待集合中移除玩家
            awaitingSearchInput.remove(player);

            // 打开新的GUI显示搜索结果
            openEnchantmentGui(player, searchQuery);
        }
    }
    
    /**
     * 清理静态变量，用于热加载
     */
    public static void cleanup() {
        searchQueries.clear();
        currentPages.clear();
        awaitingSearchInput.clear();
    }
}