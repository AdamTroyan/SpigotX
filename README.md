# SpigotX 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Spigot](https://img.shields.io/badge/Spigot-1.8%2B-brightgreen.svg)](https://www.spigotmc.org/)
[![JitPack](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)

> A modern, lightweight utility library for Spigot plugin development that eliminates boilerplate code and provides elegant builder patterns for commands, GUIs, and events.

## 🌟 Why SpigotX?

SpigotX transforms complex Spigot plugin development into simple, readable code:

```java
// Before SpigotX - Traditional Spigot
public class MyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        // 20+ lines of boilerplate code...
    }
}

// After SpigotX - Clean and Simple
SpigotX.registerCommand("heal", (sender, args) -> {
    if (sender instanceof Player) {
        ((Player) sender).setHealth(20);
        sender.sendMessage("§aYou have been healed!");
    }
    return true;
});
```

## 📦 Installation

### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.AdamTroyan</groupId>
        <artifactId>SpigotX</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AdamTroyan:SpigotX:1.0.0'
}
```

## 🚀 Quick Start

```java
public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Initialize SpigotX
        SpigotX.initialize(this);
        
        // Create a command in one line
        SpigotX.registerCommand("hello", (sender, args) -> {
            sender.sendMessage("§aHello from SpigotX!");
            return true;
        });
        
        // Create a GUI with builder pattern
        GUI gui = GUIBuilder.create()
            .title("§6My GUI")
            .size(27)
            .item(13, new ItemStack(Material.DIAMOND))
            .build();
    }
}
```

## ✨ Core Features

### 🎯 **Command System**
Three flexible ways to create commands:
- **Builder Pattern** - Fluent API for simple commands
- **Annotations** - Class-based organization for complex plugins
- **Direct Registration** - Lambda-style for quick prototyping

### 🖼️ **GUI Framework**
- **Easy Creation** - Builder pattern for inventory GUIs
- **Pagination** - Built-in support for multi-page inventories
- **Auto-Updates** - Dynamic content with automatic refresh
- **Event Handling** - Simple click handlers and validators

### ⚡ **Event Management**
- **Fluent API** - Chain conditions and handlers elegantly
- **Async Support** - Built-in async event processing
- **Lambda Support** - Modern Java 8+ syntax
- **Conditional Logic** - Easy event filtering

### 🔧 **Utilities**
- **Validation** - Input validation with readable error messages
- **Reflection** - Safe reflection utilities for version compatibility
- **Item Builders** - Fluent API for ItemStack creation

## 📚 Usage Examples

### Commands with Builder Pattern

```java
CommandBuilder.create("teleport")
    .description("Teleport to another player")
    .permission("myplugin.teleport")
    .playerOnly(true)
    .execute((sender, args) -> {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /teleport <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            ((Player) sender).teleport(target.getLocation());
            sender.sendMessage("§aTeleported to " + target.getName());
        } else {
            sender.sendMessage("§cPlayer not found!");
        }
        return true;
    })
    .tabComplete((sender, args) -> {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
    })
    .register(this);
```

### Annotation-Based Commands

```java
public class PlayerCommands {
    
    @Command(name = "heal", permission = "myplugin.heal")
    public boolean heal(CommandContext ctx) {
        if (!(ctx.getSender() instanceof Player)) {
            ctx.getSender().sendMessage("§cOnly players can use this!");
            return true;
        }
        
        Player player = (Player) ctx.getSender();
        player.setHealth(player.getMaxHealth());
        player.sendMessage("§aYou have been healed!");
        return true;
    }
    
    @TabComplete("heal")
    public List<String> healComplete(CommandContext ctx) {
        return Collections.emptyList();
    }
}

// Register in your main class
CommandManager manager = new CommandManager(this);
manager.registerCommands(new PlayerCommands());
```

### Interactive GUIs

```java
public void openPlayerMenu(Player player) {
    GUI gui = GUIBuilder.create()
        .title("§6Player Menu")
        .size(27)
        .item(10, ItemBuilder.create(Material.DIAMOND_SWORD)
            .name("§cPvP Arena")
            .lore("§7Click to join PvP!")
            .build())
        .item(12, ItemBuilder.create(Material.GOLD_INGOT)
            .name("§eShop")
            .lore("§7Buy and sell items")
            .build())
        .item(14, ItemBuilder.create(Material.BOOK)
            .name("§aQuests")
            .lore("§7View available quests")
            .build())
        .clickHandler(10, (clicker, item, slot, clickType) -> {
            clicker.sendMessage("§aTeleporting to PvP arena...");
            // Teleport logic here
        })
        .clickHandler(12, (clicker, item, slot, clickType) -> {
            openShopGUI(clicker);
        })
        .fillEmpty(ItemBuilder.create(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build())
        .build();
        
    gui.open(player);
}
```

### Paginated GUI for Large Lists

```java
public void openPlayerList(Player viewer) {
    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
    
    PaginatedGUI gui = PaginatedGUI.builder()
        .title("§6Online Players ({current}/{max})")
        .itemsPerPage(21)
        .items(players)
        .itemConverter((player, slot) -> 
            ItemBuilder.create(Material.PLAYER_HEAD)
                .name("§e" + player.getName())
                .lore("§7Click to teleport!")
                .skullOwner(player.getName())
                .build())
        .itemClickHandler((clicker, item, slot, clickType, targetPlayer) -> {
            clicker.teleport(targetPlayer.getLocation());
            clicker.sendMessage("§aTeleported to " + targetPlayer.getName());
        })
        .build();
        
    gui.open(viewer);
}
```

### Event Handling

```java
// Simple event listener
EventBuilder.listen(PlayerJoinEvent.class)
    .handler(event -> {
        event.getPlayer().sendMessage("§aWelcome to the server!");
    })
    .register(this);

// Conditional event with async processing
EventBuilder.listen(BlockBreakEvent.class)
    .condition(event -> event.getBlock().getType() == Material.DIAMOND_ORE)
    .asyncHandler(event -> {
        // Heavy database operation
        savePlayerMiningData(event.getPlayer());
        
        // Back to main thread
        Bukkit.getScheduler().runTask(this, () -> {
            event.getPlayer().sendMessage("§aDiamond ore logged!");
        });
    })
    .register(this);
```

### Input Validation

```java
public boolean setPlayerMoney(Player player, double amount) {
    // Validate inputs with clear error messages
    if (!ValidationUtils.notNull(player, "Player cannot be null")) {
        return false;
    }
    
    if (!ValidationUtils.inRange(amount, 0, 999999, "Amount must be between 0 and 999,999")) {
        player.sendMessage("§cInvalid amount! Must be between $0 and $999,999");
        return false;
    }
    
    // Safe to proceed
    setBalance(player, amount);
    player.sendMessage("§aBalance set to $" + String.format("%.2f", amount));
    return true;
}
```

## 🔄 Migration from Traditional Spigot

SpigotX works alongside existing code - no need to rewrite everything:

```java
@Override
public void onEnable() {
    // Keep existing commands
    getCommand("oldcommand").setExecutor(new OldCommandExecutor());
    
    // Add new SpigotX commands
    SpigotX.registerCommand("newcommand", (sender, args) -> {
        // Modern implementation
        return true;
    });
}
```

## 📖 API Reference

### Core Classes

| Class | Description |
|-------|-------------|
| `SpigotX` | Main entry point - initialize and register commands |
| `CommandBuilder` | Fluent API for command creation |
| `CommandManager` | Annotation-based command registration |
| `GUIBuilder` | Builder pattern for creating GUIs |
| `PaginatedGUI` | Multi-page inventory GUIs |
| `EventBuilder` | Fluent event handling |
| `ValidationUtils` | Input validation utilities |
| `ItemBuilder` | Fluent ItemStack creation |

### Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@Command` | Mark methods as command handlers |
| `@TabComplete` | Define tab completion for commands |

## 🛠️ Requirements

- **Java 8+** - Modern Java features for clean code
- **Spigot/Paper 1.8+** - Compatible with all modern Minecraft versions
- **Maven/Gradle** - For dependency management

## 🤝 Contributing

We welcome contributions! Here's how to get started:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

Please read our [Contributing Guidelines](CONTRIBUTING.md) for detailed information.

## 📋 Roadmap

- [ ] **Custom Entity API** - Simplified custom entity creation
- [ ] **Database ORM** - Built-in database mapping
- [ ] **Packet API** - Easy packet manipulation
- [ ] **World Management** - Advanced world utilities
- [ ] **Economy API** - Standardized economy interface

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support & Community

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/AdamTroyan/SpigotX/issues)
- **💡 Feature Requests**: [GitHub Discussions](https://github.com/AdamTroyan/SpigotX/discussions)
- **📚 Documentation**: [Wiki](https://github.com/AdamTroyan/SpigotX/wiki)
- **💬 Discord**: [Join our community](https://discord.gg/spigotx)

## 🙏 Acknowledgments

- **Spigot Team** - For the amazing Spigot API
- **Contributors** - Everyone who helped improve SpigotX
- **Community** - For feedback and bug reports

---

<div align="center">

**[⭐ Star this repo](https://github.com/AdamTroyan/SpigotX)** if SpigotX helped you build better plugins!

*Made with ❤️ for the Minecraft development community*
