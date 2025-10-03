# SpigotX – Complete Beginner-to-Advanced Guide

[![](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)  
[JavaDoc Reference](https://adamtroyan.github.io/SpigotX-Javadoc/)

---

## 📦 How to Add SpigotX to Your Project

SpigotX is distributed via [JitPack](https://jitpack.io/).  
**You must add JitPack as a repository** in your build system.

### Maven

Add JitPack to your `<repositories>` section:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
Add SpigotX to your `<dependencies>`:
```xml
<dependency>
    <groupId>com.github.AdamTroyan</groupId>
    <artifactId>SpigotX</artifactId>
    <version>v1.0.9</version>
</dependency>
```

### Gradle (Groovy DSL)

Add JitPack to your repositories:
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
Add SpigotX to your dependencies:
```groovy
dependencies {
    implementation 'com.github.AdamTroyan:SpigotX:v1.0.9'
}
```

### Gradle (Kotlin DSL)
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
dependencies {
    implementation("com.github.AdamTroyan:SpigotX:v1.0.9")
}
```

> **Tip:** Always use the latest [release from JitPack](https://jitpack.io/#AdamTroyan/SpigotX) for bugfixes and new features.

---

## 🏁 Getting Started – Your First Plugin with SpigotX

Below is a full example of a minimal plugin using SpigotX, including a command and a GUI.

### 1. Main Plugin Class

```java
// filepath: src/main/java/com/example/MyPlugin.java
package com.example;

import dev.adam.SpigotX;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Initialize SpigotX with your plugin instance
        SpigotX.init(this);

        // Register your commands
        SpigotX.registerCommands(new MyCommands());

        // You can also register events or GUIs here
    }
}
```

### 2. Simple Command Example

```java
// filepath: src/main/java/com/example/MyCommands.java
package com.example;

import dev.adam.commands.Command;
import org.bukkit.command.CommandSender;

public class MyCommands {
    // This will register the /hello command
    @Command(name = "hello", description = "Say hello to the player")
    public void hello(CommandSender sender, String[] args) {
        sender.sendMessage("Hello, " + sender.getName() + "!");
    }
}
```

**How to use:**  
- Add `MyCommands.java` to your project.
- Register it in your `onEnable()` as shown above.
- Type `/hello` in-game to see the message.

---

## 🧑‍💻 Advanced Command Example – With Arguments, Permissions, and Tab Completion

```java
// filepath: src/main/java/com/example/AdminCommands.java
package com.example;

import dev.adam.commands.Command;
import dev.adam.commands.TabComplete;
import dev.adam.commands.TabHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AdminCommands {
    // /admin <reload|status>
    @Command(name = "admin", permission = "myplugin.admin", usage = "/admin <reload|status>")
    public void admin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /admin <reload|status>");
            return;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("Plugin reloaded!");
        } else if (args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("Plugin is running.");
        } else {
            sender.sendMessage("Unknown subcommand.");
        }
    }

    // /warp <warpname> with tab completion
    @TabComplete(handler = WarpTab.class)
    @Command(name = "warp", description = "Warp to a location")
    public void warp(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("Usage: /warp <warpname>");
            return;
        }
        String warpName = args[0];
        // Example: warp logic here
        player.sendMessage("Warping to " + warpName + "...");
    }

    public static class WarpTab implements TabHandler {
        @Override
        public List<String> complete(CommandSender sender, String[] args) {
            // Suggest warp names
            return List.of("spawn", "shop", "arena");
        }
    }
}
```

---

## ⚡ Asynchronous Commands

For heavy operations (like database or API calls), use `@AsyncCommand`:

```java
import dev.adam.commands.AsyncCommand;
import dev.adam.commands.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;

public class LookupCommands {
    @AsyncCommand
    @Command(name = "lookup", description = "Lookup player stats")
    public void lookup(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /lookup <player>");
            return;
        }
        String player = args[0];
        // Simulate heavy operation
        String stats = "Kills: 10, Deaths: 2"; // Replace with real lookup
        // Always switch back to main thread for Bukkit API!
        Bukkit.getScheduler().runTask(SpigotX.getPlugin(), () ->
            sender.sendMessage("Stats for " + player + ": " + stats)
        );
    }
}
```

---

## 🎯 Events – Listen and React

### Basic Event Listener

```java
import dev.adam.events.Events;
import org.bukkit.event.player.PlayerJoinEvent;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        SpigotX.init(this);

        // Welcome message on join
        Events.register(PlayerJoinEvent.class, ctx -> {
            ctx.getPlayer().sendMessage("Welcome to the server, " + ctx.getPlayer().getName() + "!");
        });
    }
}
```

### Cancel Block Breaking for Non-OPs

```java
import dev.adam.events.Events;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        SpigotX.init(this);

        Events.onBlockBreak(ctx -> {
            if (!ctx.getPlayer().isOp()) {
                ctx.setCancelled(true);
                ctx.getPlayer().sendMessage("You can't break blocks!");
            }
        });
    }
}
```

---

## 🖼️ GUI – Create Interactive Menus

### Simple Clickable GUI

```java
import dev.adam.gui.GUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiExample {
    public void openSimpleGui(Player player) {
        GUI gui = new GUI(SpigotX.getPlugin(), "My First GUI", 1); // 1 row
        ItemStack emerald = new ItemStack(Material.EMERALD);

        // Set emerald at slot 4, clicking it sends a message
        gui.setItem(4, emerald, ctx -> {
            ctx.getPlayer().sendMessage("You clicked the emerald!");
        });

        gui.open(player);
    }
}
```

### Animated GUI Item

```java
import dev.adam.gui.GUI;
import dev.adam.gui.Animation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AnimatedGuiExample {
    public void openAnimatedGui(Player player) {
        GUI gui = new GUI(SpigotX.getPlugin(), "Animated GUI", 1);
        List<ItemStack> frames = List.of(
            new ItemStack(Material.RED_WOOL),
            new ItemStack(Material.GREEN_WOOL),
            new ItemStack(Material.BLUE_WOOL)
        );
        Animation anim = new Animation(frames, 10L); // 10 ticks per frame
        gui.setAnimation(4, anim);

        gui.open(player);
    }
}
```

---

## 📄 Paginated GUI – For Large Lists

```java
import dev.adam.gui.PaginatedGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PaginatedGuiExample {
    public void openPaginatedGui(Player player) {
        PaginatedGUI pgui = new PaginatedGUI(SpigotX.getPlugin(), "Shop", 5);

        // Example: Add 50 items
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ItemStack item = new ItemStack(Material.DIAMOND);
            items.add(item);
        }
        pgui.setContent(items);

        // Set navigation buttons
        pgui.setPrevItem(new ItemStack(Material.ARROW));
        pgui.setNextItem(new ItemStack(Material.ARROW));

        pgui.open(player);
    }
}
```

---

## 🏷️ Placeholders – Dynamic Text Everywhere

```java
import dev.adam.placeholders.PlaceholderManager;
import org.bukkit.entity.Player;

public class PlaceholderExample {
    public void setupPlaceholders() {
        // Register a placeholder {rank}
        PlaceholderManager.get().register("rank", (Player p) -> "VIP");

        // Register a placeholder {balance}
        PlaceholderManager.get().register("balance", (Player p) -> "1000");
    }
}
```

**Usage in GUI:**
```java
gui.setTitle("Balance: {balance}");
```

---

## ⏱️ Animation Utilities – Scheduled GUI Updates

```java
import dev.adam.gui.GUIUpdater;
import dev.adam.gui.GUI;

public class AnimationUtilExample {
    public void startUpdatingGui(GUI gui) {
        // Update slot 0 every second with a random item
        GUIUpdater.scheduleRepeating(SpigotX.getPlugin(), gui, 20L, g -> {
            g.setItem(0, getRandomItem(), null);
        });
    }
}
```

---

## 🛠️ Tips, Best Practices & Common Pitfalls

- **Always call `SpigotX.init(this)` in your `onEnable()` before using any SpigotX features.**
- **Register commands and events after initialization.**
- **For GUIs, always close them when done to avoid memory leaks.**
- **Use async commands for heavy operations, but always return to the main thread for Bukkit API calls.**
- **Use placeholders for all dynamic text in GUIs and messages.**
- **Modularize your code: separate commands, GUIs, and event listeners into different classes.**
- **Check the [JavaDoc](https://adamtroyan.github.io/SpigotX-Javadoc/) for full API documentation.**
- **If you get an error about SpigotX not being initialized, check that you called `SpigotX.init(this)` first!**

---

## ❓ FAQ

**Q:** My command doesn't work!  
**A:** Make sure you registered it with `SpigotX.registerCommands()` and annotated it with `@Command`.

**Q:** How do I update a GUI for all viewers?  
**A:** Use `gui.updateAllViewers();`

**Q:** Can I use SpigotX with Paper or Purpur?  
**A:** Yes! SpigotX is compatible with all Spigot forks.

**Q:** How do I unregister an event or GUI updater?  
**A:** Use `Events.unregister(registration)` or `GUIUpdater.cancel(gui)`.

---

## 📚 Further Reading

- [SpigotX Javadoc](https://adamtroyan.github.io/SpigotX-Javadoc/)
- [Spigot Plugin Development Guide](https://www.spigotmc.org/wiki/spigot-plugin-development/)
- [JitPack Documentation](https://jitpack.io/docs/)

---

## 📝 License

See the main repository for license details.

---

*This README is designed for both beginners and advanced users. If you have suggestions or want to contribute, open an issue or PR on GitHub!*
