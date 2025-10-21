# SpigotX Framework Documentation

SpigotX is a comprehensive Bukkit/Spigot plugin development framework that provides powerful utilities for commands, events, GUIs, placeholders, and more. This documentation covers all major components with detailed examples.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Command System](#command-system)
3. [Event System](#event-system)
4. [GUI System](#gui-system)
5. [Placeholder System](#placeholder-system)
6. [Utility Classes](#utility-classes)

---

## Getting Started

### Initialization

First, initialize SpigotX in your plugin's `onEnable()` method:

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Initialize SpigotX
        SpigotX.init(this);
        
        // Register command classes
        SpigotX.registerCommand(new MyCommands());
    }
}
```

### Quick Event Registration

```java
// Register a simple event listener
SpigotX.on(PlayerJoinEvent.class, ctx -> {
    Player player = ctx.getPlayer();
    player.sendMessage("Welcome to the server!");
});
```

---

## Command System

The command system provides multiple ways to register and handle commands with advanced features like permissions, async execution, and tab completion.

### Annotation-Based Commands

#### Basic Command

```java
public class MyCommands {
    @Command(name = "hello", description = "Say hello to a player")
    public void helloCommand(Player player, String[] args) {
        player.sendMessage("Hello, " + player.getName() + "!");
    }
}
```

#### Command with Permissions

```java
@Command(
    name = "heal", 
    description = "Heal yourself or another player",
    permission = "myplugin.heal",
    usage = "/heal [player]"
)
public void healCommand(Player player, String[] args) {
    if (args.length == 0) {
        player.setHealth(player.getMaxHealth());
        player.sendMessage("§aYou have been healed!");
    } else {
        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            target.setHealth(target.getMaxHealth());
            player.sendMessage("§aHealed " + target.getName());
        } else {
            player.sendMessage("§cPlayer not found!");
        }
    }
}
```

#### Async Command

```java
@Command(name = "lookup", description = "Lookup player data", async = true)
public void lookupCommand(Player player, String[] args) {
    // This runs asynchronously
    String playerName = args.length > 0 ? args[0] : player.getName();
    // Perform database lookup or other heavy operations
    player.sendMessage("§aLookup completed for " + playerName);
}
```

#### SubCommands

```java
@SubCommand(
    parent = "economy", 
    name = "balance", 
    description = "Check your balance"
)
public void balanceCommand(Player player, String[] args) {
    // /economy balance
    player.sendMessage("§aYour balance: $1000");
}

@SubCommand(
    parent = "economy", 
    name = "pay", 
    description = "Pay another player",
    usage = "/economy pay <player> <amount>"
)
public void payCommand(Player player, String[] args) {
    // /economy pay <player> <amount>
    if (args.length < 2) {
        player.sendMessage("§cUsage: /economy pay <player> <amount>");
        return;
    }
    // Handle payment logic
}
```

#### Tab Completion

```java
@TabComplete(command = "heal")
public List<String> healTabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
            .collect(Collectors.toList());
    }
    return Collections.emptyList();
}
```

### Programmatic Command Registration

#### Simple Registration

```java
CommandManager.register("test", "Test command", "", (sender, args) -> {
    sender.sendMessage("Test command executed!");
});
```

#### Async Registration

```java
CommandManager.registerAsync("heavytask", "Heavy task", "myplugin.admin", (sender, args) -> {
    // This runs asynchronously
    sender.sendMessage("Starting heavy task...");
    // Perform heavy operations
    sender.sendMessage("Heavy task completed!");
});
```

#### Player-Only Command

```java
CommandManager.registerPlayer("fly", "Toggle flight", "myplugin.fly", (player, args) -> {
    boolean flying = !player.isFlying();
    player.setFlying(flying);
    player.sendMessage(flying ? "§aFlight enabled!" : "§cFlight disabled!");
});
```

### Command Builder

```java
// Fluent command building
CommandBuilder.create("shop")
    .description("Open the shop")
    .permission("myplugin.shop")
    .playerExecutor(player -> {
        // Open shop GUI
        new ShopGUI().open(player);
    })
    .register();

// Quick command creation
CommandBuilder.quickPlayer("spawn", player -> {
    player.teleport(player.getWorld().getSpawnLocation());
    player.sendMessage("§aTeleported to spawn!");
});
```

### Command Context

```java
CommandBuilder.create("info")
    .contextExecutor(ctx -> {
        if (!ctx.ensurePlayer()) return;
        if (!ctx.ensureArgs(1, "/info <player>")) return;
        
        Optional<Player> target = ctx.getPlayerArg(0);
        if (target.isPresent()) {
            Player p = target.get();
            ctx.sendSuccess("Player: " + p.getName());
            ctx.sendInfo("Health: " + p.getHealth());
            ctx.sendInfo("Level: " + p.getLevel());
        } else {
            ctx.sendError("Player not found!");
        }
    })
    .register();
```

### Tab Completer Builder

```java
TabCompleterBuilder.create()
    .arg(0, sender -> Arrays.asList("create", "delete", "list"))
    .players(1) // Second argument: player names
    .numbers(2, 1, 100) // Third argument: numbers 1-100
    .ifPermission(3, "admin.permission", "admin", "moderator")
    .defaultTo("help")
    .build();
```

---

## Event System

The event system provides powerful event handling with context, filtering, and advanced features.

### Basic Event Handling

```java
// Simple event listener
EventBuilder.listen(PlayerJoinEvent.class)
    .handleEvent(event -> {
        event.getPlayer().sendMessage("Welcome!");
    })
    .register();

// Using event context
EventBuilder.listen(PlayerChatEvent.class)
    .handle(ctx -> {
        Player player = ctx.getPlayer();
        PlayerChatEvent event = ctx.getEvent();
        
        if (event.getMessage().contains("badword")) {
            ctx.cancel();
            player.sendMessage("§cWatch your language!");
        }
    })
    .register();
```

### Event Filtering

```java
// Players only
EventBuilder.listen(EntityDamageEvent.class)
    .playersOnly()
    .handlePlayer(player -> {
        player.sendMessage("You took damage!");
    })
    .register();

// Permission-based filtering
EventBuilder.listen(BlockBreakEvent.class)
    .playerHasPermission("myplugin.build")
    .handle(ctx -> {
        ctx.getPlayer().sendMessage("Block broken!");
    })
    .register();

// World-specific events
EventBuilder.listen(PlayerMoveEvent.class)
    .playerInWorld("pvp_world")
    .handle(ctx -> {
        // Handle movement in PVP world
    })
    .register();

// Custom filtering
EventBuilder.listen(PlayerInteractEvent.class)
    .filter(ctx -> ctx.getEvent().getAction() == Action.RIGHT_CLICK_BLOCK)
    .filterPlayer(player -> player.getLevel() > 10)
    .handle(ctx -> {
        // Handle right-click by players level 10+
    })
    .register();
```

### Event Priorities and Cancellation

```java
// High priority event
EventBuilder.listen(PlayerChatEvent.class)
    .high()
    .onlyIfNotCancelled()
    .handle(ctx -> {
        // Handle chat with high priority
    })
    .register();

// Monitor cancelled events
EventBuilder.listen(BlockBreakEvent.class)
    .monitor()
    .onlyIfCancelled()
    .handle(ctx -> {
        // Log cancelled block breaks
    })
    .register();
```

### Limited Execution Events

```java
// Execute only once
EventBuilder.listen(ServerLoadEvent.class)
    .once()
    .handleEvent(event -> {
        System.out.println("Server loaded!");
    })
    .register();

// Execute maximum 5 times
EventBuilder.listen(PlayerJoinEvent.class)
    .maxExecutions(5)
    .handle(ctx -> {
        ctx.getPlayer().sendMessage("Welcome new player!");
    })
    .register();

// Timeout after 30 seconds
EventBuilder.listen(PlayerQuitEvent.class)
    .timeout(600) // 30 seconds in ticks
    .handleEvent(event -> {
        // Handle player quit for first 30 seconds only
    })
    .register();
```

### Event Context Features

```java
EventBuilder.listen(PlayerJoinEvent.class)
    .handle(ctx -> {
        Player player = ctx.getPlayer();
        
        // Store metadata
        ctx.setMetadata("join_time", System.currentTimeMillis());
        ctx.setMetadata("welcomed", false);
        
        // Check event properties
        if (ctx.hasPlayer()) {
            String eventName = ctx.getEventName();
            long timestamp = ctx.getTimestamp();
            
            // Access other context info
            if (ctx.hasLocation()) {
                Location loc = ctx.getLocation();
            }
            
            if (ctx.hasWorld()) {
                World world = ctx.getWorld();
            }
        }
        
        // Debug info for ops
        if (player.isOp()) {
            ctx.printDebugInfo();
        }
    })
    .register();
```

### Event Manager

```java
// Named handlers
EventManager.registerNamed("welcome-handler", PlayerJoinEvent.class, ctx -> {
    ctx.getPlayer().sendMessage("Welcome!");
});

// Check if handler exists
if (EventManager.isNamedHandlerRegistered("welcome-handler")) {
    EventManager.unregisterNamed("welcome-handler");
}

// Conditional registration
EventManager.registerConditional(PlayerChatEvent.class, ctx -> {
    ctx.getEvent().setMessage("[FILTERED] " + ctx.getEvent().getMessage());
}, ctx -> ctx.getPlayer().hasPermission("chat.filter"));

// Player-only events
EventManager.registerPlayerOnly(EntityDamageEvent.class, ctx -> {
    ctx.getPlayer().sendMessage("You were damaged!");
});
```

### Event Middleware

```java
EventManager.addMiddleware(new EventManager.EventMiddleware() {
    @Override
    public boolean beforeHandle(EventContext<?> context) {
        // Log all events
        System.out.println("Event: " + context.getEventName());
        return true; // Continue processing
    }
    
    @Override
    public void afterHandle(EventContext<?> context) {
        // Cleanup after event handling
    }
    
    @Override
    public void onError(EventContext<?> context, Exception error) {
        System.err.println("Error handling " + context.getEventName() + ": " + error.getMessage());
    }
});
```

---

## GUI System

The GUI system provides comprehensive inventory management with click handling, animations, and advanced features.

### Basic GUI Creation

```java
public class ShopGUI {
    public void open(Player player) {
        GUI gui = new GUI("Shop", 6);
        
        // Add items with click handlers
        ItemStack diamond = GUI.createItem(Material.DIAMOND, "&bDiamond", "&7Price: $100");
        gui.setItem(10, diamond, ctx -> {
            if (hasEnoughMoney(ctx.getPlayer(), 100)) {
                takeMoney(ctx.getPlayer(), 100);
                ctx.giveItem(new ItemStack(Material.DIAMOND));
                ctx.success("Purchased diamond for $100!");
                ctx.closeInventory();
            } else {
                ctx.error("Not enough money!");
            }
        });
        
        // Add close button
        ItemStack closeButton = GUI.createItem(Material.BARRIER, "&cClose");
        gui.setItem(49, closeButton, ctx -> ctx.closeInventory());
        
        // Fill border with glass
        gui.fillBorder(GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, " "), null);
        
        gui.open(player);
    }
}
```

### GUI Click Context Features

```java
gui.setItem(20, emerald, ctx -> {
    Player player = ctx.getPlayer();
    
    // Click type detection
    if (ctx.isLeftClick()) {
        // Buy 1 item
    } else if (ctx.isRightClick()) {
        // Buy 10 items
    } else if (ctx.isShiftClick()) {
        // Buy max items
    }
    
    // Item management
    if (ctx.hasItem(currencyItem)) {
        ctx.takeItem(currencyItem);
        ctx.giveItem(purchaseItem);
    }
    
    // Player communication
    ctx.success("Purchase successful!");
    ctx.playSuccessSound();
    ctx.sendTitle("&aPurchased!", "&7Thank you for your purchase");
    
    // Permission checking
    if (ctx.requirePermission("shop.vip", "VIP required for this item!")) {
        // VIP purchase logic
    }
    
    // Async operations
    ctx.runAsync(plugin, () -> {
        // Database operations
    });
    
    // Debug info
    if (player.isOp()) {
        ctx.debug();
    }
});
```

### GUI Builder

```java
GUI gui = new GUIBuilder("Custom Shop", 6)
    .setValidateItems(true)
    .setAutoFillBackground(true)
    .quickItem(10, Material.DIAMOND, "&bDiamond Sword", ctx -> {
        // Handle purchase
    }, "&7A powerful sword", "&7Price: $500")
    .quickButton(20, Material.EMERALD, "&aEmerald Tool", () -> {
        // Handle emerald tool purchase
    }, "&7Special tools")
    .quickCloseButton(49, Material.BARRIER, "&cClose")
    .fillBorder(GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, " "), null)
    .onOpen(player -> {
        player.sendMessage("Welcome to the shop!");
    })
    .onClose(player -> {
        player.sendMessage("Thanks for visiting!");
    })
    .build();

gui.open(player);
```

### Paginated GUI

```java
public class PlayerListGUI {
    public void open(Player player) {
        PaginatedGUI gui = new PaginatedGUI("Online Players", 6);
        
        // Create items for all online players
        List<ItemStack> playerItems = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack item = GUI.createItem(
                Material.PLAYER_HEAD, 
                "&a" + p.getName(),
                "&7Health: " + p.getHealth(),
                "&7Level: " + p.getLevel(),
                "&7World: " + p.getWorld().getName()
            );
            playerItems.add(item);
        }
        
        gui.setContent(playerItems);
        gui.setMainItemAction(ctx -> {
            // Handle player item click
            ItemStack clicked = ctx.getClickedItem();
            String playerName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                ctx.getPlayer().teleport(target);
                ctx.success("Teleported to " + playerName);
            }
        });
        
        // Set navigation buttons
        gui.setPrevButton(GUI.createItem(Material.ARROW, "&cPrevious Page"));
        gui.setNextButton(GUI.createItem(Material.ARROW, "&aNext Page"));
        
        // Add search functionality
        gui.setSearchQuery(""); // Can be set based on user input
        
        // Add filtering
        gui.addFilter((item, index) -> {
            // Only show players in same world
            String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player p = Bukkit.getPlayer(itemName);
            return p != null && p.getWorld().equals(player.getWorld());
        });
        
        gui.open(player);
    }
}
```

### GUI Animations

```java
// Create loading animation
Animation loading = Animation.builder()
    .addFrame(GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "&7Loading."))
    .addFrame(GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "&7Loading.."))
    .addFrame(GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "&7Loading..."))
    .setTicksPerFrame(10)
    .setLoops(-1) // Infinite
    .build();

// Use in GUI
gui.setItem(22, loading.getCurrentFrame(), null);

// Start animation
loading.play();

// Create rainbow animation
Animation rainbow = Animation.createRainbowAnimation(5, -1);
gui.setItem(4, rainbow.getCurrentFrame(), null);
rainbow.play();
```

### GUI Templates

```java
// Create navigation template
GUIBuilder.GUITemplate navTemplate = new GUIBuilder.GUITemplate("navigation")
    .setItem(45, GUI.createItem(Material.ARROW, "&cPrevious"), ctx -> {
        // Previous page logic
    })
    .setItem(53, GUI.createItem(Material.ARROW, "&aNext"), ctx -> {
        // Next page logic
    })
    .setItem(49, GUI.createItem(Material.BARRIER, "&cClose"), ctx -> {
        ctx.closeInventory();
    });

// Apply template
new GUIBuilder("My GUI", 6)
    .applyTemplate(navTemplate)
    .build();
```

### GUI Updates

```java
// Schedule repeating updates
GUIUpdater.scheduleRepeating(plugin, gui, 20L, g -> {
    // Update server info every second
    ItemStack serverInfo = GUI.createItem(Material.BEACON, 
        "&aServer Info",
        "&7Players: " + Bukkit.getOnlinePlayers().size(),
        "&7TPS: 20.0",
        "&7Uptime: " + getUptime()
    );
    g.getInventory().setItem(4, serverInfo);
});

// Conditional updates
GUIUpdater.scheduleConditional(plugin, gui, 20L, 
    g -> !g.getInventory().getViewers().isEmpty(),
    g -> {
        // Only update when players are viewing
    }
);
```

---

## Placeholder System

The placeholder system provides powerful text replacement with caching and custom placeholders.

### Basic Usage

```java
// Initialize built-in placeholders
PlaceholderManager.initialize();

// Parse text with placeholders
String message = PlaceholderManager.parse(player, 
    "Hello {player_name}! You have {player_level} levels and {player_health} health."
);
player.sendMessage(message);
```

### Custom Placeholders

#### Player Placeholders

```java
// Register player-specific placeholder
PlaceholderManager.register("player_coins", player -> {
    return String.valueOf(getPlayerCoins(player));
});

// Register with caching
PlaceholderManager.registerCached("player_rank", player -> {
    return getDatabaseRank(player); // Expensive operation
}, 30000); // Cache for 30 seconds
```

#### Global Placeholders

```java
// Server-wide placeholders
PlaceholderManager.registerGlobal("server_tps", () -> {
    return String.format("%.1f", getCurrentTPS());
});

PlaceholderManager.registerGlobal("total_players", () -> {
    return String.valueOf(getTotalRegisteredPlayers());
});
```

#### Parameterized Placeholders

```java
// Placeholders with parameters
PlaceholderManager.registerParameterized("top_player", (player, params) -> {
    if (params.length >= 1) {
        try {
            int rank = Integer.parseInt(params[0]);
            return getTopPlayer(rank);
        } catch (NumberFormatException e) {
            return "Invalid rank";
        }
    }
    return "No rank specified";
});

// Usage: {top_player:1} = #1 player
// Usage: {top_player:5} = #5 player
```

#### Offline Player Placeholders

```java
PlaceholderManager.registerOffline("offline_last_seen", offlinePlayer -> {
    if (offlinePlayer.hasPlayedBefore()) {
        long lastPlayed = offlinePlayer.getLastPlayed();
        return formatTimeAgo(lastPlayed);
    }
    return "Never";
});
```

### Advanced Parsing

```java
// Parse with context variables
Map<String, String> context = new HashMap<>();
context.put("custom_var", "Custom Value");
context.put("event_name", "PVP Tournament");

String text = PlaceholderManager.parseWithContext(player,
    "Welcome to {event_name}! Your custom value: {custom_var}",
    context
);

// Parse lists
List<String> lore = Arrays.asList(
    "&7Player: {player_name}",
    "&7Health: {player_health}",
    "&7Level: {player_level}"
);
List<String> parsedLore = PlaceholderManager.parseList(player, lore);

// Mathematical expressions
String mathText = "You have {math:10+5*2} points!"; // Results in "You have 20 points!"
String parsed = PlaceholderManager.parse(player, mathText);
```

### Built-in Placeholders

#### Player Information
- `{player_name}` - Player's name
- `{player_displayname}` - Player's display name
- `{player_uuid}` - Player's UUID
- `{player_world}` - Current world name
- `{player_gamemode}` - Current gamemode
- `{player_level}` - Player's level
- `{player_exp}` - Experience percentage
- `{player_health}` - Current health
- `{player_max_health}` - Maximum health
- `{player_food}` - Food level
- `{player_ip}` - Player's IP address
- `{player_ping}` - Player's ping

#### Player Location
- `{player_x}` - X coordinate
- `{player_y}` - Y coordinate  
- `{player_z}` - Z coordinate
- `{player_yaw}` - Yaw rotation
- `{player_pitch}` - Pitch rotation

#### Server Information
- `{server_name}` - Server name
- `{server_version}` - Server version
- `{server_online}` - Online player count
- `{server_max_players}` - Maximum players
- `{server_motd}` - Server MOTD
- `{server_uptime}` - Server uptime

#### Date and Time
- `{date}` - Current date and time
- `{time}` - Current time
- `{timestamp}` - Unix timestamp
- `{year}` - Current year
- `{month}` - Current month
- `{day}` - Current day

#### Utility Placeholders
- `{random:min,max}` - Random number between min and max
- `{format_number:123456}` - Format number with commas
- `{format_money:1000.50}` - Format as currency
- `{upper:text}` - Convert to uppercase
- `{lower:TEXT}` - Convert to lowercase
- `{substring:text,start,end}` - Extract substring

### Cache Management

```java
// Enable/disable caching
PlaceholderManager.setCachingEnabled(true);

// Set cache expiration time
PlaceholderManager.setCacheExpirationTime(10000); // 10 seconds

// Clear cache manually
PlaceholderManager.clearCache();

// Clear only expired entries
PlaceholderManager.clearExpiredCache();

// Print cache statistics
PlaceholderManager.printCacheStats();
```

---

## Utility Classes

### Validation Utils

```java
// Null and empty checks
ValidationUtils.notNull(player, "Player cannot be null");
ValidationUtils.notEmpty(playerName, "Player name cannot be empty");
ValidationUtils.notBlank(message, "Message cannot be blank");

// Range validation
ValidationUtils.inRange(level, 1, 100, "Level must be between 1 and 100");
ValidationUtils.isPositive(amount, "Amount must be positive");
ValidationUtils.isNonNegative(score, "Score cannot be negative");

// String validation
ValidationUtils.hasMinLength(password, 8, "Password too short");
ValidationUtils.hasMaxLength(username, 16, "Username too long");
ValidationUtils.isValidUUID(uuidString, "Invalid UUID format");

// Minecraft-specific validation
ValidationUtils.isValidCoordinate(x, "Invalid X coordinate");
ValidationUtils.isValidHeight(y, "Invalid Y coordinate");
ValidationUtils.isValidItemAmount(amount, "Invalid item amount");

// Pattern validation
ValidationUtils.matchesPattern(input, "[a-zA-Z0-9]+", "Only alphanumeric characters allowed");
ValidationUtils.isSafeString(input, "String contains unsafe characters");

// Non-throwing versions
if (ValidationUtils.isInRange(value, 1, 10)) {
    // Value is valid
}

if (ValidationUtils.isValidLength(name, 3, 16)) {
    // Name length is valid
}
```

### Reflection Utils

```java
// Method invocation
Object result = ReflectionUtils.callMethod(player, "getHandle");
ReflectionUtils.callStaticMethod(MyClass.class, "staticMethod", arg1, arg2);

// Field access
Object fieldValue = ReflectionUtils.getField(player, "playerConnection");
ReflectionUtils.setField(player, "customField", newValue);

// Class loading
Class<?> nmsClass = ReflectionUtils.getNMSClass("EntityPlayer");
Class<?> craftClass = ReflectionUtils.getCraftBukkitClass("entity.CraftPlayer");

// Create instances  
Object instance = ReflectionUtils.createInstance(MyClass.class, arg1, arg2);

// Packet sending
Object packet = createCustomPacket();
ReflectionUtils.sendPacket(player, packet);

// Utility methods
String version = ReflectionUtils.getBukkitVersion();
boolean exists = ReflectionUtils.classExists("com.example.MyClass");
boolean hasMethod = ReflectionUtils.methodExists(MyClass.class, "myMethod", String.class);

// Debugging
ReflectionUtils.printMethods(MyClass.class);
ReflectionUtils.printFields(MyClass.class);
ReflectionUtils.printCacheStats();
```

## Advanced Examples

### Complete Shop System

```java
public class ShopSystem {
    private final Plugin plugin;
    private final Map<Player, Integer> playerCoins = new HashMap<>();
    
    public ShopSystem(Plugin plugin) {
        this.plugin = plugin;
        registerCommands();
        registerPlaceholders();
    }
    
    private void registerCommands() {
        CommandBuilder.create("shop")
            .description("Open the item shop")
            .playerExecutor(this::openShop)
            .register();
            
        CommandBuilder.create("coins")
            .description("Check your coin balance")
            .contextExecutor(ctx -> {
                if (!ctx.ensurePlayer()) return;
                Player player = ctx.getPlayer();
                int coins = getCoins(player);
                ctx.sendSuccess("You have " + coins + " coins!");
            })
            .register();
    }
    
    private void registerPlaceholders() {
        PlaceholderManager.register("shop_coins", player -> 
            String.valueOf(getCoins(player))
        );
    }
    
    private void openShop(Player player) {
        GUI gui = new GUIBuilder("Item Shop", 6)
            .setAutoFillBackground(true)
            .quickItem(10, Material.DIAMOND_SWORD, "&bDiamond Sword", ctx -> {
                purchaseItem(ctx, "Diamond Sword", 100, new ItemStack(Material.DIAMOND_SWORD));
            }, "&7Price: 100 coins")
            .quickItem(12, Material.DIAMOND_PICKAXE, "&bDiamond Pickaxe", ctx -> {
                purchaseItem(ctx, "Diamond Pickaxe", 150, new ItemStack(Material.DIAMOND_PICKAXE));
            }, "&7Price: 150 coins")
            .quickItem(14, Material.ENCHANTED_GOLDEN_APPLE, "&6Golden Apple", ctx -> {
                if (ctx.requirePermission("shop.vip", "VIP membership required!")) {
                    purchaseItem(ctx, "Golden Apple", 500, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
                }
            }, "&7Price: 500 coins", "&c&lVIP ONLY")
            .quickCloseButton(49, Material.BARRIER, "&cClose Shop")
            .onOpen(p -> p.sendMessage("&aWelcome to the shop!"))
            .build();
            
        gui.open(player);
    }
    
    private void purchaseItem(GUIClickContext ctx, String itemName, int price, ItemStack item) {
        Player player = ctx.getPlayer();
        int coins = getCoins(player);
        
        if (coins >= price) {
            setCoins(player, coins - price);
            ctx.giveItem(item);
            ctx.success("Purchased " + itemName + " for " + price + " coins!");
            ctx.playSuccessSound();
        } else {
            ctx.error("Not enough coins! You need " + (price - coins) + " more coins.");
            ctx.playErrorSound();
        }
    }
    
    private int getCoins(Player player) {
        return playerCoins.getOrDefault(player, 0);
    }
    
    private void setCoins(Player player, int coins) {
        playerCoins.put(player, coins);
    }
}
```

### Event-Driven Achievement System

```java
public class AchievementSystem {
    private final Map<Player, Set<String>> playerAchievements = new HashMap<>();
    
    public void initialize() {
        registerAchievements();
        registerPlaceholders();
    }
    
    private void registerAchievements() {
        // First join achievement
        EventBuilder.listen(PlayerJoinEvent.class)
            .filter(ctx -> !ctx.getPlayer().hasPlayedBefore())
            .handle(ctx -> {
                awardAchievement(ctx.getPlayer(), "first_join", "Welcome to the Server!");
            })
            .register();
            
        // Mining achievements
        EventBuilder.listen(BlockBreakEvent.class)
            .playersOnly()
            .filterEvent(event -> event.getBlock().getType() == Material.DIAMOND_ORE)
            .handle(ctx -> {
                Player player = ctx.getPlayer();
                incrementStat(player, "diamonds_mined");
                
                int diamonds = getStat(player, "diamonds_mined");
                if (diamonds == 1) {
                    awardAchievement(player, "first_diamond", "First Diamond!");
                } else if (diamonds == 100) {
                    awardAchievement(player, "diamond_master", "Diamond Master!");
                }
            })
            .register();
            
        // Death achievements
        EventBuilder.listen(PlayerDeathEvent.class)
            .handle(ctx -> {
                Player player = ctx.getPlayer();
                incrementStat(player, "deaths");
                
                if (getStat(player, "deaths") == 1) {
                    awardAchievement(player, "first_death", "Oops!");
                }
            })
            .register();
    }
    
    private void registerPlaceholders() {
        PlaceholderManager.register("achievements_count", player -> 
            String.valueOf(getAchievementCount(player))
        );
        
        PlaceholderManager.registerParameterized("achievement_unlocked", (player, params) -> {
            if (params.length >= 1) {
                return hasAchievement(player, params[0]) ? "✓" : "✗";
            }
            return "?";
        });
    }
    
    private void awardAchievement(Player player, String id, String name) {
        if (hasAchievement(player, id)) return;
        
        playerAchievements.computeIfAbsent(player, k -> new HashSet<>()).add(id);
        
        // Broadcast achievement
        String message = PlaceholderManager.parse(player, 
            "&6[Achievement] &a{player_name} &7unlocked &e" + name + "&7!"
        );
        Bukkit.broadcastMessage(message);
        
        // Send title to player
        player.sendTitle("&6Achievement Unlocked!", "&e" + name, 10, 70, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    private boolean hasAchievement(Player player, String id) {
        return playerAchievements.getOrDefault(player, Collections.emptySet()).contains(id);
    }
    
    private int getAchievementCount(Player player) {
        return playerAchievements.getOrDefault(player, Collections.emptySet()).size();
    }
    
    // Placeholder implementations for stats system
    private void incrementStat(Player player, String stat) {
        // Implementation depends on your data storage
    }
    
    private int getStat(Player player, String stat) {
        // Implementation depends on your data storage
        return 0;
    }
}
```

This comprehensive documentation covers the major features of the SpigotX framework. Each system is designed to work together seamlessly, providing a powerful foundation for Bukkit/Spigot plugin development with clean, maintainable code patterns.