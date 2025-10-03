# SpigotX – Advanced & In-Depth Documentation

[![](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)  
[JavaDoc Reference](https://adamtroyan.github.io/SpigotX-Javadoc/)

---

## Advanced Table of Contents

- [Deep Dive: Initialization & Lifecycle](#deep-dive-initialization--lifecycle)
- [Command System: Advanced Usage](#command-system-advanced-usage)
- [Events: Filters, Priorities, and Unregistration](#events-filters-priorities-and-unregistration)
- [GUI: Dynamic, Animated, and Reactive Interfaces](#gui-dynamic-animated-and-reactive-interfaces)
- [Paginated GUI: Customization & Navigation](#paginated-gui-customization--navigation)
- [Placeholders: Dynamic Content & Context](#placeholders-dynamic-content--context)
- [Animation Utilities: Custom Animations & Scheduling](#animation-utilities-custom-animations--scheduling)
- [Error Handling & Debugging](#error-handling--debugging)
- [Best Practices & Patterns](#best-practices--patterns)
- [FAQ & Troubleshooting](#faq--troubleshooting)
- [License](#license)

---

## Deep Dive: Initialization & Lifecycle

### How SpigotX Bootstraps

SpigotX requires explicit initialization to bind itself to your plugin instance and set up its internal event bus and command registry.

```java
@Override
public void onEnable() {
    SpigotX.init(this); // Must be called first!
    SpigotX.registerCommands(new MyCommands());
}
```

**What happens under the hood?**
- Stores your plugin instance for global access (`SpigotX.getPlugin()`).
- Initializes the event system (`Events.init(plugin)`).
- Prepares command reflection and registration.

**Common Pitfall:**  
If you forget to call `SpigotX.init(this)`, any call to `SpigotX.getPlugin()` or command/event registration will throw an `IllegalStateException`.

---

## Command System: Advanced Usage

### Multiple Executors & Modular Command Classes

You can split commands into multiple classes for modularity:

```java
SpigotX.registerCommands(new AdminCommands());
SpigotX.registerCommands(new PlayerCommands());
```

### Subcommands & Argument Parsing

SpigotX supports subcommands and argument parsing by inspecting the method signature:

```java
@Command(name = "user", description = "User management")
public void user(CommandSender sender, String[] args) {
    if (args.length == 0) {
        sender.sendMessage("/user <info|kick> <player>");
        return;
    }
    switch (args[0].toLowerCase()) {
        case "info":
            // ...
            break;
        case "kick":
            // ...
            break;
    }
}
```

### Asynchronous Commands

For heavy operations (database, HTTP, etc.), use `@AsyncCommand`:

```java
@AsyncCommand
@Command(name = "lookup", description = "Lookup player stats")
public void lookup(CommandSender sender, String[] args) {
    // Runs off the main thread!
    String player = args[0];
    Stats stats = statsService.fetchStats(player); // Expensive call
    Bukkit.getScheduler().runTask(SpigotX.getPlugin(), () ->
        sender.sendMessage("Stats: " + stats)
    );
}
```

### Custom Tab Completion with Context

You can provide context-aware tab completion:

```java
@TabComplete(handler = DynamicTab.class)
@Command(name = "warp")
public void warp(CommandSender sender, String[] args) { /* ... */ }

public static class DynamicTab implements TabHandler {
    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return WarpManager.getAllWarpNames();
        }
        return List.of();
    }
}
```

### Permissions & Usage

- `permission` in `@Command` restricts usage.
- `usage` provides a usage message if arguments are missing or invalid.

---

## Events: Filters, Priorities, and Unregistration

### Registering with Filters and Priorities

You can filter events and set their priority and async status:

```java
Events.register(PlayerInteractEvent.class,
    ctx -> {
        Player p = ctx.getPlayer();
        if (ctx.getEvent().getAction() == Action.RIGHT_CLICK_BLOCK) {
            p.sendMessage("You right-clicked a block!");
        }
    },
    event -> event.getPlayer().hasPermission("myplugin.special"),
    EventPriority.HIGH,
    false, // ignoreCancelled
    false  // async
);
```

### Unregistering Listeners

To avoid memory leaks, unregister listeners when not needed:

```java
var reg = Events.register(...);
Events.unregister(reg); // Unregister a specific listener
Events.unregisterAll(); // Unregister all listeners registered by your plugin
```

### Lambda Listeners for Common Events

```java
Events.onJoin(ctx -> {
    ctx.getPlayer().sendMessage("Welcome, " + ctx.getPlayer().getName());
});
```

### EventContext: Advanced Usage

`EventContext<T>` gives you:
- `getEvent()`
- `getPlayer()` (if applicable)
- `setCancelled(boolean)` for cancellable events

Example: Cancel block breaking for non-ops

```java
Events.onBlockBreak(ctx -> {
    if (!ctx.getPlayer().isOp()) {
        ctx.setCancelled(true);
        ctx.getPlayer().sendMessage("You can't break blocks!");
    }
});
```

---

## GUI: Dynamic, Animated, and Reactive Interfaces

### Dynamic GUIs

You can update GUIs in real-time based on player actions or external events:

```java
GUI gui = new GUI(this, "Stats", 3);
gui.setItem(10, getStatsItem(player), ctx -> {
    // Refresh stats
    gui.setItem(10, getStatsItem(ctx.getPlayer()), null);
});
```

### Animated Items

```java
List<ItemStack> frames = List.of(frame1, frame2, frame3);
Animation anim = new Animation(frames, 5L); // 5 ticks per frame
gui.setAnimation(13, anim);
```

### Global Click Handlers

```java
gui.setGlobalClickHandler(ctx -> {
    ctx.getPlayer().sendMessage("Clicked slot: " + ctx.getSlot());
});
```

### Open/Close Listeners

```java
gui.setOnOpen(player -> player.sendMessage("GUI opened!"));
gui.setOnClose(player -> player.sendMessage("GUI closed!"));
```

### Filling Borders and Rows

```java
gui.fillBorder(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), ctx -> {});
gui.fillRow(1, new ItemStack(Material.GOLD_INGOT), ctx -> {});
```

### Per-Player Customization

You can set items or titles dynamically per player:

```java
gui.setItem(0, getPersonalizedItem(player), ctx -> {});
gui.setTitle("Welcome, {player}");
```

---

## Paginated GUI: Customization & Navigation

### Custom Navigation Buttons

```java
pgui.setPrevItem(new ItemStack(Material.ARROW));
pgui.setNextItem(new ItemStack(Material.ARROW));
```

### Handling Page Changes

```java
pgui.setOnPageChange(page -> {
    Bukkit.getLogger().info("User switched to page " + page);
});
```

### Dynamic Content

```java
pgui.setContent(fetchItemsForPlayer(player));
```

---

## Placeholders: Dynamic Content & Context

### Registering Placeholders

```java
PlaceholderManager.get().register("rank", p -> getRank(p));
PlaceholderManager.get().register("balance", p -> String.valueOf(getBalance(p)));
```

### Using Placeholders in GUIs

```java
gui.setTitle("Balance: {balance}");
gui.setItem(0, new ItemStack(Material.EMERALD), ctx -> {
    ctx.getPlayer().sendMessage("Your balance: {balance}");
});
```

### Removing Placeholders

```java
PlaceholderManager.get().unregister("rank");
```

---

## Animation Utilities: Custom Animations & Scheduling

### Scheduling GUI Updates

```java
GUIUpdater.scheduleRepeating(this, gui, 20L, g -> {
    // Update GUI every second
    g.setItem(0, getRandomItem(), null);
});
```

### Cancelling Updates

```java
GUIUpdater.cancel(gui);
```

---

## Error Handling & Debugging

- All core methods throw clear exceptions if misused (e.g., not initialized).
- Use try/catch for async commands to handle exceptions gracefully.
- Use logging (`Bukkit.getLogger()`) for debugging event flows.

---

## Best Practices & Patterns

- Always unregister listeners and GUI updaters when not needed.
- Use async commands for heavy operations.
- Use placeholders for all dynamic text.
- Modularize commands and GUIs for maintainability.

---

## FAQ & Troubleshooting

**Q:** Why does my command not appear?  
**A:** Ensure you called `SpigotX.registerCommands()` and annotated your method with `@Command`.

**Q:** Why do placeholders not update?  
**A:** Make sure you registered the placeholder and use `{key}` in your text.

**Q:** How do I prevent memory leaks with GUIs?  
**A:** Use `setAutoUnregisterWhenEmpty(true)` (default) and always close GUIs when done.

---

## License

See the main repository for license details.

---

## Further Reading

- [SpigotX Javadoc](https://adamtroyan.github.io/SpigotX-Javadoc/)
- [Spigot Plugin Development Guide](https://www.spigotmc.org/wiki/spigot-plugin-development/)
