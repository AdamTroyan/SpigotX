# SpigotX – Full Documentation (English)

[![](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)

SpigotX is a lightweight and flexible library that simplifies Minecraft Spigot/Bukkit plugin development by providing:
- Declarative command management using annotations
- A functional events system with filters, centralized registration, and unregistration
- A modular GUI system with buttons, animations, pagination, and placeholder support

This documentation explains everything from installation to advanced usage, enabling even new users to get started quickly and confidently.

---

## Table of Contents
- **[Prerequisites](#prerequisites)**
- **[Installation (JitPack)](#installation-jitpack)**
- **[Basic Plugin Setup](#basic-plugin-setup)**
- **[Commands](#commands)**
- **[Events](#events)**
- **[User Interface (GUI)](#user-interface-gui)**
- **[Paginated GUI](#paginated-gui)**
- **[Placeholders](#placeholders)**
- **[GUI Animation Utilities](#gui-animation-utilities)**
- **[FAQ](#faq)**
- **[Tips & Troubleshooting](#tips--troubleshooting)**
- **[License](#license)**

---

## Prerequisites
- Java 17 or higher (as per `pom.xml`)
- Spigot API 1.20.1 or compatible versions (library compiled against `spigot-api:1.20.1-R0.1-SNAPSHOT`)
- A Maven/Gradle project for your Spigot plugin

Note: In the code, `SpigotX.getVersion()` returns "1.0.4" while `pom.xml` specifies version `v1.0.9`. This is an internal display discrepancy and does not affect usage, but it is recommended to align versions on your side.

---

## Installation (JitPack)
Add the JitPack repository and dependency.

**Gradle:**
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AdamTroyan:SpigotX:Tag'
}
```

**Maven:**
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
        <version>Tag</version>
    </dependency>
</dependencies>
```
Replace `Tag` with a version tag (e.g., `v1.0.9`) or commit hash.

---

## Basic Plugin Setup
In your main `JavaPlugin` class:

```java
import dev.adam.SpigotX;
import dev.adam.events.Events;

@Override
public void onEnable() {
    // Initialize SpigotX (required before using other modules)
    SpigotX.init(this);

    // (Optional) Initialize the events system separately if desired
    Events.init(this);

    // Register commands from an executor class
    SpigotX.registerCommands(new MyCommands());
}
```

Key points:
- `SpigotX.init(plugin)` initializes internal modules and allows `SpigotX.getPlugin()` access later.
- `SpigotX.registerCommands(executor)` automatically registers all methods annotated with `@Command` in the executor class.
- If `pluginInstance` is not initialized, using the API will throw an `IllegalStateException`.

---

## Commands
The system allows you to define commands using annotations on methods in the class passed to `SpigotX.registerCommands(...)` or `new CommandManager(plugin, executor)`.

### Relevant Annotations
- `@Command` – defines a command, including name, description, permission, aliases, etc.
- `@AsyncCommand` – runs the command asynchronously (`CompletableFuture.runAsync`).
- `@TabComplete(handler = MyTabHandler.class)` – attaches custom tab completion.

### Supported Method Signatures
- `void cmd(Player player)` – player-only command; console usage sends a message.
- `void cmd(CommandSender sender, String[] args)` – command with arguments.
- `void cmd(CommandSender sender)` – general command with no arguments.

### Basic Example

```java
import dev.adam.commands.Command;
import dev.adam.commands.AsyncCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyCommands {

    @Command(name = "greet", description = "Greets the player")
    public void greet(Player player) {
        player.sendMessage("Hello " + player.getName() + "!");
    }

    @AsyncCommand
    @Command(name = "say", aliases = "s|speak", permission = "myplugin.say",
             permissionMessage = "You do not have permission.", description = "Sends a message", usage = "/say <msg>")
    public void say(CommandSender sender, String[] args) {
        sender.sendMessage("You said: " + String.join(" ", args));
    }
}
```

### Registration

```java
SpigotX.registerCommands(new MyCommands());
// or: new dev.adam.commands.CommandManager(this, new MyCommands());
```

### Custom Tab Completion

```java
import dev.adam.commands.TabComplete;
import dev.adam.commands.TabHandler;
import org.bukkit.command.CommandSender;

public class MyCommands {

    @TabComplete(handler = NameHandler.class)
    @Command(name = "teleport", aliases = "tp|tpto")
    public void tp(CommandSender sender, String[] args) { /* ... */ }

    public static class NameHandler implements TabHandler {
        @Override
        public java.util.List<String> complete(CommandSender sender, String[] args) {
            return java.util.List.of("Steve", "Alex");
        }
    }
}
```

**Permissions:** If `permission()` is set and the sender lacks permission, a default message or `permissionMessage()` is sent.

**Note:** To register `PluginCommand` without `plugin.yml`, commands are registered via reflection. Defining `plugin.yml` is still recommended for full compatibility and help.

---

## Events
The API provides a unified wrapper for Bukkit events with easy registration, lambda support, filters, and unregistration.

### Initialization

```java
Events.init(this); // automatically called via SpigotX.init(this)
```

### General Registration

```java
// Without filters
Events.register(PlayerJoinEvent.class, ctx -> {
    var e = ctx.getEvent();
    e.getPlayer().sendMessage("Welcome!");
});

// With filter, priority, and async
Events.register(AsyncPlayerChatEvent.class, ctx -> {
    // ...
}, e -> true, EventPriority.NORMAL, false, true);
```

### Convenience Methods for Common Events
- `Events.onJoin(listener)`
- `Events.onQuit(listener)`
- `Events.onChat(listener, async)`
- `Events.onMove(listener)`
- `Events.onTeleport(listener)`
- `Events.onRespawn(listener)`
- `Events.onInteract(listener)`
- `Events.onDamage(listener)`
- `Events.onDeath(listener)`
- `Events.onInventoryClick(listener)`
- `Events.onInventoryClose(listener)`
- `Events.onBlockBreak(listener)` / `onBlockPlace` / `onBlockDamage`
- And many more from `dev.adam.events.Events`.

### Listener Signature

```java
@FunctionalInterface
public interface EventListener<T extends org.bukkit.event.Event> {
    void handle(EventContext<T> ctx);
}
```

`EventContext<T>` provides `getEvent()` and `getPlayer()` (for player-related events).

### Unregistering

```java
Events.unregisterAll();
// or unregister a specific listener you saved a reference to
```

### Example

```java
Events.onDamage(ctx -> {
    var e = ctx.getEvent();
    if (e.getEntity() instanceof org.bukkit.entity.Player p) {
        p.sendMessage("You are being attacked!");
    }
});
```

---

## User Interface (GUI)
The GUI system is a modular replacement for Bukkit inventories, with click handlers, open/close listeners, animations, borders, and more.

### Create & Open

```java
GUI gui = new GUI(this, "Main Menu", 3); // 3 rows (= 27 slots)

gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> {
    ctx.getPlayer().sendMessage("You clicked the diamond!");
});

gui.open(player);
```

### Key GUI Methods
- `setItem(int slot, ItemStack item, Consumer<ClickContext> action)` – set item with full click context.
- `setItemForPlayer(int slot, ItemStack item, Consumer<Player> action)` – legacy version with Player only.
- `addItem(ItemStack item, Consumer<ClickContext> action)` – places item in first free slot.
- `removeItem(int slot)` / `clear()` – remove/clear.
- `open(Player player)` / `openAll(Collection<Player>)` / `closeAll()` – manage openings.
- `fillRow(int row, ItemStack item, Consumer<ClickContext> action)` – fill a row.
- `fillBorder(ItemStack item, Consumer<ClickContext> action)` – fill border.
- `setAnimation(int slot, Animation animation)` / `removeAnimation(int slot)` – animations.
- Global listeners: `setGlobalClickHandler(...)`, `setOnOpen(...)`, `setOnClose(...)`.
- Configuration: `setDefaultCancel(boolean)`, `setAllowShiftClick(boolean)`, `setAllowDrag(boolean)`, `setAutoUnregisterWhenEmpty(boolean)`.

### ClickContext Provides
- `getPlayer()`, `getClicked()`, `getCursor()`
- `getSlot()`, `getRawSlot()`, `isTop()`
- `getClickType()`, `getAction()`, `getHotbarButton()`, `isShiftClick()`
- `getEvent()` + `setCancelled(boolean)`

### Builder Example

```java
GUI gui = GUIBuilder.create(this, "Shop", 4)
    .fillBorder(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), ctx -> {})
    .item(10, new ItemStack(Material.DIAMOND), ctx -> ctx.getPlayer().sendMessage("Diamond bought!"))
    .onOpen(p -> p.sendMessage("Opened"))
    .onClose(p -> p.sendMessage("Closed"))
    .globalHandler(ctx -> { /* global processing */ })
    .build();

gui.open(player);
```

### Animation

```java
List<ItemStack> frames = List.of(frame1, frame2, frame3);
Animation animation = new Animation(frames, 10L); // frame every 10 ticks

gui.setAnimation(13, animation);
```

Placeholders in titles/lore are automatically applied per player on open (see Placeholders section).

---

## Paginated GUI
Multi-page list view with navigation buttons.

```java
PaginatedGUI pgui = new PaginatedGUI(this, "List", 5); // 5 rows; last row for controls
pgui.setContent(myItemsList);
pgui.setPrevItem(prevIcon);
pgui.setNextItem(nextIcon);
pgui.setOnPageChange(page -> getLogger().info("Page: " + page));

pgui.openFirst(player); // or pgui.openPage(player, n)
```

### Behavior
- `pageSize = rows*9 - 9` – last row reserved for controls.
- `prevSlot = rows*9 - 9`, `nextSlot = rows*9 - 1`.
- Clears and fills content on each page open.

---

## Placeholders
Internal manager allows dynamic text replacement in titles and item lore per player.

### Registration

```java
PlaceholderManager.get().register("player", p -> p.getName());
PlaceholderManager.get().register("coins", p -> String.valueOf(getCoins(p)));
```

Use `{key}` syntax in text. Example: "Hello {player}, you have {coins} coins".

### Unregister & Clear

```java
PlaceholderManager.get().unregister("coins");
PlaceholderManager.get().clearAll();
```

Applied internally via `GUI.applyPlaceholdersToPlayer(...)` before inventory opens.

---

## GUI Animation Utilities
`GUIUpdater` – internal scheduler that runs periodic tasks only when viewers are present:

```java
GUIUpdater.scheduleRepeating(this, gui, 10L, g -> {
    // refresh/animation code
});

// cancel a specific resource
GUIUpdater.cancel(gui);

// cancel all
GUIUpdater.cancelAll();
```

`Animation` itself manages frames (`current()`, `advance()`), and GUI updates the slot accordingly.

---

## FAQ

**Do I need to call `Events.init(this)` if I already called `SpigotX.init(this)`?**
- No. `SpigotX.init(this)` internally calls `Events.init(this)`. Extra calls do no harm.

**How do I ensure a command runs only for players?**
- Use a method with a `Player` signature. The system will notify console users.

**How to add Tab Completion support?**
- Add `@TabComplete(handler = MyHandler.class)` on the command method, returning a list of strings in `complete`.

**Why does the GUI auto-close/unregister?**
- With `setAutoUnregisterWhenEmpty(true)` (default), the GUI unregisters when no viewers exist to prevent memory leaks.

---

## Tips & Troubleshooting

**IllegalStateException: "SpigotX.init(plugin) must be called first!"**
- Ensure `SpigotX.init(this)` is called at the start of `onEnable()` before using Events/Commands/GUI.

**Command not recognized**
- Ensure the method is annotated with `@Command(name = "...")` and `SpigotX.registerCommands(executor)` is called. Consider adding commands to `plugin.yml`.

**Permission issues**
- Set `permission` in the annotation and configure permissions in your server or permissions plugin.

**GUI performance issues**
- Avoid heavy synchronous tasks inside click handlers; consider async tasks when relevant.

**Placeholders not replaced**
- Ensure strings use `{key}` and a provider is registered with the same key. Replacement occurs on GUI open/refresh per player.

---

## License
The library is free to use for Spigot/Bukkit plugins. See also the `README.md` for additional details and updates.
