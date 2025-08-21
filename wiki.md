# AdvancedEnchantmentsGUI Wiki

## 目录

- [插件概述](#插件概述)
- [安装指南](#安装指南)
- [命令详解](#命令详解)
- [配置文件详解](#配置文件详解)
- [权限系统](#权限系统)
- [自定义消息](#自定义消息)
- [常见问题](#常见问题)

## 插件概述

AdvancedEnchantmentsGUI 是一款为 Minecraft 服务器设计的高级附魔查看插件，它为 AdvancedEnchantments 插件提供了直观的图形界面，使用户能够轻松浏览和搜索所有可用的附魔效果。

## 安装指南

### 前置条件
- Spigot/Paper 或其他兼容的 Minecraft 服务端
- AdvancedEnchantments 插件（必须先安装）
- Minecraft 版本：1.12.2及以上

### 安装步骤
1. 下载最新版本的 AdvancedEnchantmentsGUI 插件 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹中
3. 重启服务器以加载插件
4. 插件会自动生成默认配置文件在 `plugins/AdvancedEnchantmentsGUI/` 目录下

## 命令详解

### 基本命令

#### `/aegui`
- **描述**：打开附魔GUI界面
- **权限**：`advancedenchantmentsgui.use`
- **使用示例**：`/aegui`

#### `/aegui open`
- **描述**：打开附魔GUI界面（与基本命令相同）
- **权限**：`advancedenchantmentsgui.use`
- **使用示例**：`/aegui open`

#### `/aegui search <关键词>`
- **描述**：搜索特定的附魔
- **权限**：`advancedenchantmentsgui.use`
- **使用示例**：`/aegui search 锋利`

#### `/aegui help`
- **描述**：显示插件帮助信息
- **权限**：`advancedenchantmentsgui.use`
- **使用示例**：`/aegui help`

#### `/aegui reload`
- **描述**：重载插件配置
- **权限**：`advancedenchantmentsgui.reload`
- **使用示例**：`/aegui reload`

### 重载命令

#### `/aegui-reload`
- **描述**：重载附魔配置
- **权限**：`advancedenchantmentsgui.reload`
- **使用示例**：`/aegui-reload`
- **别名**：`/enchantmentsguireload`, `/egui-reload`

## 配置文件详解

### config.yml

```yaml
# GUI 设置
gui:
  # GUI 标题
  title: "高级附魔"
  
  # 每页显示的附魔数量
  items_per_page: 45
  
  # 翻页按钮设置
  prev_button:
    material: ARROW
    name: "§a上一页"
    lore:
      - "§7点击前往上一页"
    slot: 48
  
  next_button:
    material: ARROW
    name: "§a下一页"
    lore:
      - "§7点击前往下一页"
    slot: 50
  
  # 搜索按钮设置
  search_button:
    name: "§a搜索附魔"
    lore:
      - "§7输入 /aegui <关键词> 来筛选附魔"
      - "§7例如: /aegui 锋利"
    slot: 49

# 插件消息
messages:
  reload_success: "§a成功重载了附魔配置！"
  reload_failed: "§c重载附魔配置失败，请检查配置文件！"

# 字体设置
font:
  language: "chinese"

# 显示文本设置
display_text:
  applies_to: "适用物品: "
  group: "附魔组: "
  levels: "等级: "
  chance: "触发概率: "
  cooldown: "冷却时间: "
  level: "等级 "

# Lore 模板设置
lore_template:
  - '%description%'
  - '&7------------------------'
  - '&e适用物品: &f%applies_to%'
  - '&e附魔组: &f%group_name%'
  - '&e等级信息:'
  - '  %level_text% %level% %chance%%cooldown%'
```

### message.yml

```yaml
# 命令相关消息
commands:
  player_only: "§c只有玩家才可以使用这些命令！"
  no_permission: "§c你没有权限使用这个命令！"
  search_prompt: "§a请输入搜索关键词，然后发送聊天消息！"
  search_usage: "§c请输入搜索关键词！用法: /aegui search <关键词>"
  
  # 帮助信息
  help:
    header: ""
    title: "§e§l====== AdvancedEnchantmentsGUI 帮助 ======"
    command_help: "§a/aegui help §7- 显示此帮助信息"
    command_open: "§a/aegui open §7- 打开附魔GUI界面"
    command_search: "§a/aegui search <关键词> §7- 搜索特定附魔"
    command_reload: "§a/aegui reload §7- 重载插件配置（需要权限）"
    footer: "§e§l======================================="
    footer_space: ""

# 重载相关消息
reload:
  success: "§a成功重载了附魔配置！"
  failed: "§c重载附魔配置失败，请检查配置文件！"
  plugin_failed: "§c重载插件配置失败！"

# 自动重载通知消息
watchdog:
  console_prefix: "§e[AEGUI] §7"
  player_prefix: "§e[AEGUI] §7"
  config_reloaded: "配置文件 %file% 已自动重载"
  message_reloaded: "消息配置文件 %file% 已自动重载"
  groups_reloaded: "分组配置文件 %file% 已自动重载，附魔数据已更新"
```

## 权限系统

| 权限节点 | 描述 | 默认值 |
|---------|------|--------|
| `advancedenchantmentsgui.use` | 允许使用 `/aegui` 命令打开GUI和搜索附魔 | true |
| `advancedenchantmentsgui.reload` | 允许使用 `/aegui reload` 和 `/aegui-reload` 命令 | op |
| `aegui.admin` | 允许接收配置自动重载的通知消息 | op |

## 自定义消息

插件的所有消息都可以在 `message.yml` 文件中自定义。以下是可自定义的消息类型：

1. **命令相关消息**：包括玩家专用消息、无权限消息、搜索提示和帮助信息
2. **重载相关消息**：包括重载成功和失败的消息
3. **自动重载通知消息**：配置文件自动重载时发送的通知

所有消息都支持颜色代码，可以使用 `§` 或 `&` 作为颜色代码前缀。

## GUI 自定义

你可以在 `config.yml` 文件中自定义 GUI 的外观和行为：

1. **GUI 标题**：设置 `gui.title` 来自定义界面标题
2. **每页物品数量**：调整 `gui.items_per_page` 来改变每页显示的附魔数量
3. **翻页按钮**：自定义 `gui.prev_button` 和 `gui.next_button` 的材料、名称、描述和位置
4. **搜索按钮**：自定义 `gui.search_button` 的名称、描述和位置
5. **显示文本**：自定义各种显示文本，如适用物品、附魔组等
6. **Lore 模板**：自定义物品描述的格式和内容

## 配置文件自动重载

插件具有配置文件自动重载功能，当你修改以下配置文件时，插件会自动检测并应用更改：
- `config.yml`
- `message.yml`
- `groups.yml`

当配置文件被重载时，管理员会收到通知，并且所有打开的 GUI 会自动关闭并重新打开，以显示最新的配置内容。

## 常见问题

### 1. GUI 不显示任何附魔
- 确保 AdvancedEnchantments 插件已正确安装并加载
- 检查 `enchantments.yml` 和 `groups.yml` 文件是否存在并且格式正确
- 使用 `/aegui reload` 命令重新加载配置

### 2. 搜索功能不工作
- 确保你使用的是正确的命令格式：`/aegui search <关键词>`
- 检查是否有包含该关键词的附魔

### 3. 配置文件更改后没有生效
- 等待几秒，插件会自动检测并应用更改
- 或者使用 `/aegui reload` 命令手动重载配置
- 检查配置文件格式是否正确

### 4. 如何修改 GUI 中的颜色和样式
- 编辑 `config.yml` 中的 `lore_template` 和其他文本设置
- 使用颜色代码 `§` 或 `&` 来添加颜色

## 版本历史

### v1.0.0
- 初始版本发布
- 支持基本的 GUI 功能
- 支持搜索和分页
- 支持配置文件自动重载

---

*最后更新时间：2025-08-21*