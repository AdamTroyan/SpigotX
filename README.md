# SpigotX – The Ultimate Spigot Plugin Development Toolkit 🚀

[![](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)  
[JavaDoc Reference](https://adamtroyan.github.io/SpigotX-Javadoc/)

---

> **Why SpigotX?**  
> SpigotX is not just a utility library – it’s a modern, modular, and developer-friendly framework that empowers you to build robust, maintainable, and beautiful Minecraft plugins with minimal boilerplate and maximum power.  
> Whether you’re a beginner or a seasoned developer, SpigotX will make you fall in love with plugin development.

---

## 📦 Effortless Installation

SpigotX is distributed via [JitPack](https://jitpack.io/).  
**You must add JitPack as a repository** in your build system.

<details>
<summary><b>Show Maven & Gradle Setup</b></summary>

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.AdamTroyan</groupId>
    <artifactId>SpigotX</artifactId>
    <version>v1.0.9</version>
</dependency>
```

### Gradle (Groovy DSL)

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
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
</details>

> **Tip:** Always use the latest [release from JitPack](https://jitpack.io/#AdamTroyan/SpigotX) for bugfixes and new features.

---

## 🏁 Your First Plugin – Step by Step

### 1. Main Plugin Class

```java
// filepath: src/main/java/com/example/MyPlugin.java
package com.example;

import dev.adam.SpigotX;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // 1. Initialize SpigotX
        SpigotX.init(this);

        // 2. Register commands
        SpigotX.registerCommands(new MyCommands());

        // 3. Register events (optional)
        SpigotX.registerCommands(new AdminCommands());
    }
}
```

### 2. A Command Anyone Can Understand

```java
// filepath: src/main/java/com/example/MyCommands.java
package com.example;

import dev.adam.commands.Command;
import org.bukkit.command.CommandSender;

public class MyCommands {
    // Registers /hello
    @Command(name = "hello", description = "Say hello to the player")
    public void hello(CommandSender sender, String[] args) {
        sender.sendMessage("👋 Hello, " + sender.getName() + "! Welcome to the server.");
    }
}
```

---

## 🧑‍💻 Advanced Command: Arguments, Permissions, Tab Completion

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
        switch (args[0].toLowerCase()) {
            case "reload" -> sender.sendMessage("🔄 Plugin reloaded!");
            case "status" -> sender.sendMessage("✅ Plugin is running.");
            default -> sender.sendMessage("❌ Unknown subcommand.");
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
        // Here you would teleport the player to the warp
        player.sendMessage("🚀 Warping to " + warpName + "...");
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

## ⚡ Async Commands: No More Lag

```java
// filepath: src/main/java/com/example/LookupCommands.java
package com.example;

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
        // Simulate heavy operation (e.g., database)
        String stats = "Kills: 10, Deaths: 2"; // Replace with real lookup
        // Always switch back to main thread for Bukkit API!
        Bukkit.getScheduler().runTask(SpigotX.getPlugin(), () ->
            sender.sendMessage("📊 Stats for " + player + ": " + stats)
        );
    }
}
```

---

## 🎯 Events: React to the World

### Welcome Players on Join

```java
// filepath: src/main/java/com/example/JoinListener.java
package com.example;

import dev.adam.events.Events;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinListener extends JavaPlugin {
    @Override
    public void onEnable() {
        SpigotX.init(this);

        Events.register(PlayerJoinEvent.class, ctx -> {
            ctx.getPlayer().sendMessage("🎉 Welcome to the server, " + ctx.getPlayer().getName() + "!");
        });
    }
}
```

### Block Breaking Protection

```java
// filepath: src/main/java/com/example/BlockBreakListener.java
package com.example;

import dev.adam.events.Events;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockBreakListener extends JavaPlugin {
    @Override
    public void onEnable() {
        SpigotX.init(this);

        Events.onBlockBreak(ctx -> {
            if (!ctx.getPlayer().isOp()) {
                ctx.setCancelled(true);
                ctx.getPlayer().sendMessage("⛔ You can't break blocks!");
            }
        });
    }
}
```

---

## 🖼️ GUI: Interactive Menus That Wow

### Simple Clickable GUI

```java
// filepath: src/main/java/com/example/GuiExample.java
package com.example;

import dev.adam.gui.GUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiExample {
    public void openSimpleGui(Player player) {
        GUI gui = new GUI(SpigotX.getPlugin(), "💎 My First GUI", 1); // 1 row
        ItemStack emerald = new ItemStack(Material.EMERALD);

        // Set emerald at slot 4, clicking it sends a message
        gui.setItem(4, emerald, ctx -> {
            ctx.getPlayer().sendMessage("💚 You clicked the emerald!");
        });

        gui.open(player);
    }
}
```

### Animated GUI Item

```java
// filepath: src/main/java/com/example/AnimatedGuiExample.java
package com.example;

import dev.adam.gui.GUI;
import dev.adam.gui.Animation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class AnimatedGuiExample {
    public void openAnimatedGui(Player player) {
        GUI gui = new GUI(SpigotX.getPlugin(), "🌈 Animated GUI", 1);
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

## 📄 Paginated GUI: For Large Lists

```java
// filepath: src/main/java/com/example/PaginatedGuiExample.java
package com.example;

import dev.adam.gui.PaginatedGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class PaginatedGuiExample {
    public void openPaginatedGui(Player player) {
        PaginatedGUI pgui = new PaginatedGUI(SpigotX.getPlugin(), "🛒 Shop", 5);

        // Add 50 items
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

## 🏷️ Placeholders: Dynamic Text Everywhere

```java
// filepath: src/main/java/com/example/PlaceholderExample.java
package com.example;

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

## ⏱️ Animation Utilities: Scheduled GUI Updates

```java
// filepath: src/main/java/com/example/AnimationUtilExample.java
package com.example;

import dev.adam.gui.GUIUpdater;
import dev.adam.gui.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AnimationUtilExample {
    public void startUpdatingGui(GUI gui) {
        // Update slot 0 every second with a random item
        GUIUpdater.scheduleRepeating(SpigotX.getPlugin(), gui, 20L, g -> {
            g.setItem(0, new ItemStack(Material.values()[(int) (Math.random() * Material.values().length)]), null);
        });
    }
}
```

---

## 🛠️ Pro Tips & Best Practices

- **Always call `SpigotX.init(this)` in your `onEnable()` before using any SpigotX features.**
- **Register commands and events after initialization.**
- **For GUIs, always close them when done to avoid memory leaks.**
- **Use async commands for heavy operations, but always return to the main thread for Bukkit API calls.**
- **Use placeholders for all dynamic text in GUIs and messages.**
- **Modularize your code: separate commands, GUIs, and event listeners into different classes.**
- **Check the [JavaDoc](https://adamtroyan.github.io/SpigotX-Javadoc/) for full API documentation.**
- **If you get an error about SpigotX not being initialized, check that you called `SpigotX.init(this)` first!**
- **Use meaningful command descriptions and permissions for better UX and security.**
- **Document your code and use comments to help future maintainers (or yourself!).**

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

> **Ready to build something amazing?**  
> SpigotX is your secret weapon.  
>  
> *If you have suggestions or want to contribute, open an issue or PR on GitHub!*

---
