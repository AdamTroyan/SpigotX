# SpigotX Framework

**SpigotX** is a comprehensive, enterprise-grade framework for Minecraft plugin development built on the Bukkit/Spigot API. Designed for both beginners and advanced developers, it provides powerful abstractions, performance optimizations, and modern development patterns to dramatically accelerate plugin creation while maintaining code quality and maintainability.

## 🚀 Core Features

### 🎯 Advanced Command System
- **Annotation-driven architecture** with `@Command`, `@SubCommand`, and `@AsyncCommand`
- **Automatic command registration** and discovery
- **Intelligent tab completion** with customizable suggestions
- **Permission-based access control** with fallback messages
- **Asynchronous command execution** for database operations
- **Built-in cooldown management** and rate limiting

### 📡 Enhanced Event Management
- **Fluent builder pattern** for clean event registration
- **Middleware chain processing** for cross-cutting concerns
- **Advanced event filtering** with lambda expressions
- **Context-aware event handling** with rich metadata
- **Performance-optimized dispatching** with minimal overhead
- **Automatic cleanup** and memory management

### 🎨 Professional GUI Framework
- **Complete inventory management** with drag-and-drop support
- **Paginated interfaces** for large datasets
- **Theme system** with customizable styles
- **Smooth animations** and transitions
- **Click action handling** with event bubbling
- **Auto-updating content** with real-time data binding

### 🔧 Enterprise Utilities
- **Advanced placeholder system** with caching and performance monitoring
- **Robust task scheduler** with failure recovery and metrics
- **Safe reflection utilities** for NMS operations and version compatibility
- **Comprehensive validation** with sanitization and security checks
- **Performance profiling** and monitoring tools

## 📦 Installation & Setup

### Prerequisites
- **Java 8+** (Recommended: Java 17 or higher)
- **Bukkit/Spigot/Paper** server (1.16+ supported)
- **Maven or Gradle** build system

### Maven Configuration
```xml
<dependency>
    <groupId>dev.adam</groupId>
    <artifactId>spigotx</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>

<!-- Shade the framework into your plugin -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <configuration>
        <relocations>
            <relocation>
                <pattern>dev.adam.spigotx</pattern>
                <shadedPattern>your.plugin.libs.spigotx</shadedPattern>
            </relocation>
        </relocations>
    </configuration>
</plugin>
```

### Gradle Configuration
```gradle
dependencies {
    implementation 'dev.adam:spigotx:1.0.0'
}

shadowJar {
    relocate 'dev.adam.spigotx', 'your.plugin.libs.spigotx'
}
```

## 🏗️ Getting Started

### Basic Plugin Structure
```java
@Plugin(
    name = "MyPlugin",
    version = "1.0.0",
    description = "Professional plugin using SpigotX"
)
public class MyPlugin extends JavaPlugin {
    
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        // Initialize SpigotX framework
        SpigotX.initialize(this);
        
        // Setup managers
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(configManager.getDatabaseConfig());
        
        // Register components
        registerCommands();
        registerEvents();
        registerPlaceholders();
        
        getLogger().info("Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Cleanup resources
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // Cancel all scheduled tasks
        Scheduler.cancelAll();
        
        getLogger().info("Plugin disabled gracefully!");
    }
    
    private void registerCommands() {
        CommandManager.registerCommands(this, 
            new PlayerCommands(this),
            new AdminCommands(this)
        );
    }
    
    private void registerEvents() {
        new PlayerEventHandler(this).register();
        new WorldEventHandler(this).register();
    }
    
    private void registerPlaceholders() {
        PlaceholderManager.getInstance()
            .registerPlayerPlaceholder("level", this::getPlayerLevel)
            .registerPlayerPlaceholder("coins", this::getPlayerCoins);
    }
}
```

## 🎯 Advanced Usage Examples

### Professional Command Implementation
```java
public class PlayerCommands {
    
    private final MyPlugin plugin;
    
    public PlayerCommands(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Command(
        name = "balance",
        description = "Check your current balance",
        permission = "economy.balance",
        usage = "/balance [player]",
        aliases = {"bal", "money"}
    )
    public void balanceCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou must specify a player!");
                return;
            }
            
            Player player = (Player) sender;
            double balance = plugin.getEconomyManager().getBalance(player);
            player.sendMessage(String.format("§aYour balance: §e$%.2f", balance));
        } else {
            // Check other player's balance (requires permission)
            if (!sender.hasPermission("economy.balance.others")) {
                sender.sendMessage("§cYou don't have permission to check others' balance!");
                return;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return;
            }
            
            double balance = plugin.getEconomyManager().getBalance(target);
            sender.sendMessage(String.format("§a%s's balance: §e$%.2f", target.getName(), balance));
        }
    }
    
    @AsyncCommand(
        name = "transfer",
        description = "Transfer money to another player",
        permission = "economy.transfer"
    )
    public void transferCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can transfer money!");
            return;
        }
        
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /transfer <player> <amount>");
            return;
        }
        
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            
            // Perform transfer (this runs asynchronously)
            TransferResult result = plugin.getEconomyManager().transfer(player, target, amount);
            
            // Switch back to main thread for player messaging
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    player.sendMessage(String.format("§aTransferred §e$%.2f §ato %s", amount, target.getName()));
                    target.sendMessage(String.format("§aReceived §e$%.2f §afrom %s", amount, player.getName()));
                } else {
                    player.sendMessage("§c" + result.getErrorMessage());
                }
            });
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
        }
    }
}
```

### Advanced Event Handling
```java
public class PlayerEventHandler {
    
    private final MyPlugin plugin;
    
    public PlayerEventHandler(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        // Player join with async data loading
        EventManager.builder()
            .plugin(plugin)
            .event(PlayerJoinEvent.class)
            .handler(this::onPlayerJoin)
            .register();
        
        // Player move with region detection
        EventManager.builder()
            .plugin(plugin)
            .event(PlayerMoveEvent.class)
            .filter(event -> isSignificantMovement(event))
            .middleware(new RegionCheckMiddleware())
            .handler(this::onPlayerMove)
            .register();
        
        // Command processing with security
        EventManager.builder()
            .plugin(plugin)
            .event(PlayerCommandPreprocessEvent.class)
            .middleware(new CommandSecurityMiddleware())
            .middleware(new CommandLoggingMiddleware())
            .handler(this::onCommandProcess)
            .register();
    }
    
    private void onPlayerJoin(EventContext<PlayerJoinEvent> context) {
        Player player = context.getEvent().getPlayer();
        
        // Set custom join message
        context.getEvent().setJoinMessage(
            String.format("§e%s §7joined the server! §8[%d/%d]", 
                player.getName(), 
                Bukkit.getOnlinePlayers().size(),
                Bukkit.getMaxPlayers())
        );
        
        // Load player data asynchronously
        Scheduler.builder()
            .async(true)
            .execute(() -> {
                PlayerData data = plugin.getDatabaseManager().loadPlayerData(player.getUniqueId());
                
                // Update on main thread
                Scheduler.builder()
                    .execute(() -> {
                        plugin.getPlayerManager().loadPlayer(player, data);
                        
                        // Send welcome messages
                        player.sendMessage("§aWelcome back, " + player.getName() + "!");
                        player.sendMessage("§7Level: §e" + data.getLevel());
                        player.sendMessage("§7Coins: §6" + data.getCoins());
                    })
                    .build()
                    .schedule();
            })
            .build()
            .schedule();
    }
    
    private boolean isSignificantMovement(PlayerMoveEvent event) {
        return event.getFrom().distanceSquared(event.getTo()) > 0.01;
    }
    
    private void onPlayerMove(EventContext<PlayerMoveEvent> context) {
        Player player = context.getEvent().getPlayer();
        Location to = context.getEvent().getTo();
        
        // Check for region changes
        String currentRegion = plugin.getRegionManager().getRegion(to);
        String previousRegion = context.getData("previous_region", String.class);
        
        if (!Objects.equals(currentRegion, previousRegion)) {
            handleRegionChange(player, previousRegion, currentRegion);
            context.setData("previous_region", currentRegion);
        }
    }
    
    private void handleRegionChange(Player player, String from, String to) {
        if (to != null) {
            switch (to) {
                case "spawn":
                    player.sendActionBar("§a✦ Welcome to spawn area");
                    break;
                case "pvp":
                    player.sendActionBar("§c⚔ PvP zone - Be careful!");
                    break;
                case "shop":
                    player.sendActionBar("§6$ Shopping district");
                    break;
            }
        }
    }
}
```

### Professional GUI Implementation
```java
public class ShopGUI {
    
    private final MyPlugin plugin;
    private final Map<Material, ShopItem> shopItems;
    
    public ShopGUI(MyPlugin plugin) {
        this.plugin = plugin;
        this.shopItems = loadShopItems();
    }
    
    public void openMainShop(Player player) {
        PaginatedGUI gui = PaginatedGUI.builder()
            .title("§6✦ Server Shop ✦")
            .size(54)
            .itemsPerPage(36)
            .items(createShopItems())
            .theme(createShopTheme())
            .build();
        
        // Configure navigation
        gui.setPreviousPageItem(45, ItemBuilder.of(Material.ARROW)
            .name("§7« Previous Page")
            .lore("§8Click to go back")
            .build());
        
        gui.setNextPageItem(53, ItemBuilder.of(Material.ARROW)
            .name("§7Next Page »")
            .lore("§8Click to continue")
            .build());
        
        gui.setPageInfoItem(49, ItemBuilder.of(Material.BOOK)
            .name("§ePage §f{page} §7of §f{maxPage}")
            .lore("§7Items: §e{total}")
            .build());
        
        // Add category buttons
        gui.setItem(48, createCategoryButton("Blocks", Material.STONE), 
            event -> openCategoryShop(player, "blocks"));
        gui.setItem(50, createCategoryButton("Tools", Material.DIAMOND_PICKAXE), 
            event -> openCategoryShop(player, "tools"));
        
        gui.open(player);
    }
    
    private List<GUIItem> createShopItems() {
        return shopItems.entrySet().stream()
            .map(entry -> new GUIItem(
                createShopItemStack(entry.getKey(), entry.getValue()),
                event -> handleShopPurchase((Player) event.getWhoClicked(), entry.getValue())
            ))
            .collect(Collectors.toList());
    }
    
    private ItemStack createShopItemStack(Material material, ShopItem item) {
        return ItemBuilder.of(material)
            .name("§b" + item.getDisplayName())
            .lore(
                "§7Price: §a$" + item.getPrice(),
                "",
                "§8• " + item.getDescription(),
                "",
                "§eLeft click to buy 1",
                "§eRight click to buy 64",
                "§eShift+click for custom amount"
            )
            .glow(item.isSpecial())
            .build();
    }
    
    private void handleShopPurchase(Player player, ShopItem item) {
        EconomyManager economy = plugin.getEconomyManager();
        
        if (!economy.hasBalance(player, item.getPrice())) {
            player.sendMessage("§cYou don't have enough money!");
            SoundUtil.playError(player);
            return;
        }
        
        // Process purchase
        economy.withdraw(player, item.getPrice());
        player.getInventory().addItem(new ItemStack(item.getMaterial()));
        
        // Send confirmation
        player.sendMessage(String.format("§aPurchased %s for $%.2f", 
            item.getDisplayName(), item.getPrice()));
        SoundUtil.playSuccess(player);
        
        // Log transaction
        plugin.getLogger().info(String.format("Player %s purchased %s for $%.2f", 
            player.getName(), item.getDisplayName(), item.getPrice()));
    }
    
    private GUITheme createShopTheme() {
        return GUITheme.builder()
            .fillItem(ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build())
            .borderItem(ItemBuilder.of(Material.YELLOW_STAINED_GLASS_PANE).name(" ").build())
            .build();
    }
}
```

## 🔧 Configuration

### Framework Configuration
Create `spigotx.yml` in your plugin's data folder:

```yaml
# SpigotX Framework Configuration
spigotx:
  # Performance settings
  performance:
    enable-monitoring: true
    slow-operation-threshold: 50ms
    max-async-threads: 4
  
  # Command system
  commands:
    enable-debug: false
    default-cooldown: 0
    permission-message: "&cYou don't have permission!"
  
  # Event system  
  events:
    enable-debug: false
    max-middleware-time: 100ms
    enable-async-events: true
  
  # GUI system
  gui:
    enable-animations: true
    update-interval: 1
    enable-sound-effects: true
  
  # Utilities
  placeholders:
    cache-duration: 30s
    enable-metrics: true
  
  scheduler:
    enable-statistics: true
    max-concurrent-tasks: 10
```

## 📊 Performance & Best Practices

### Optimization Tips
- Use `@AsyncCommand` for database operations
- Implement efficient event filters to reduce processing overhead
- Cache frequently accessed GUI components
- Utilize placeholder caching for expensive operations
- Monitor performance with built-in profiling tools

### Security Considerations
- Always validate user input with `ValidationUtils`
- Use permission checks for sensitive operations
- Implement rate limiting for resource-intensive commands
- Sanitize data before database operations

## 🤝 Contributing

We welcome contributions from the community! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details on:

- Code style and conventions
- Testing requirements
- Pull request process
- Issue reporting

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support & Community

- **📖 Documentation**: [GitHub Wiki](https://github.com/adamtroyan/SpigotX/wiki)
- **🐛 Bug Reports**: [GitHub Issues](https://github.com/adamtroyan/SpigotX/issues)
---

**Made with ❤️ for the Minecraft development community**

*SpigotX Framework - Professional plugin development, simplified.*
