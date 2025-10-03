# SpigotX – The Ultimate Spigot Plugin Development Toolkit 🚀

[![](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)  
[JavaDoc Reference](https://adamtroyan.github.io/SpigotX-Javadoc/)

---

> **Why SpigotX?**  
> SpigotX is a modern, modular, and developer-friendly framework that empowers you to build robust, maintainable, and beautiful Minecraft plugins with minimal boilerplate and maximum power.  
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
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.AdamTroyan:SpigotX:v1.0.9'
}
```

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
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
package com.example;

import dev.adam.SpigotX;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // 1. Initialize SpigotX
        SpigotX.init(this);

        // 2. Register annotated commands
        SpigotX.registerCommands(new MyCommands());

        // 3. Register events (optional)
        SpigotX.registerCommands(new AdminCommands());
    }
}
```

---

## 🏷️ Annotation-Based Commands

SpigotX supports annotation-based commands for maximum simplicity and power.  
Just annotate your methods and register the class instance:

```java
package com.example;

import dev.adam.commands.Command;
import dev.adam.commands.AsyncCommand;
import dev.adam.commands.TabComplete;
import dev.adam.commands.TabHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class MyCommands {
    // Registers /hello
    @Command(name = "hello", description = "Say hello to the player")
    public void hello(CommandSender sender, String[] args) {
        sender.sendMessage("👋 Hello, " + sender.getName() + "! Welcome to the server.");
    }

    // Registers /lookup as async
    @AsyncCommand
    @Command(name = "lookup", description = "Lookup player stats")
    public void lookup(CommandSender sender, String[] args) {
        // Heavy operation in background
        String player = args.length > 0 ? args[0] : "unknown";
        // ... do async work, then send result to sender
    }

    // Registers /warp with tab completion
    @TabComplete(handler = WarpTab.class)
    @Command(name = "warp", description = "Warp to a location")
    public void warp(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("Usage: /warp <warpname>");
            return;
        }
        String warpName = args[0];
        player.sendMessage("🚀 Warping to " + warpName + "...");
    }

    public static class WarpTab implements TabHandler {
        @Override
        public List<String> complete(CommandSender sender, String[] args) {
            return List.of("spawn", "shop", "arena");
        }
    }
}
```

---

## 🧑‍💻 Classic CommandBuilder (Optional)

You can also use the builder API for full control:

```java
package com.example;

import dev.adam.commands.CommandBuilder;
import org.bukkit.command.CommandSender;

public class MyCommands {
    public void register() {
        new CommandBuilder()
            .name("hello")
            .description("Say hello to the player")
            .executor((sender, args) -> sender.sendMessage("👋 Hello, " + sender.getName() + "! Welcome to the server."))
            .register();
    }
}
```

---

## ⚡ Async Commands: No More Lag

Just add `@AsyncCommand` to your command method and SpigotX will run it off the main thread.

```java
@AsyncCommand
@Command(name = "lookup", description = "Lookup player stats")
public void lookup(CommandSender sender, String[] args) {
    // Heavy operation here
}
```

---

## 🎯 Events: React to the World

```java
package com.example;

import dev.adam.events.EventBuilder;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinListener extends JavaPlugin {
    @Override
    public void onEnable() {
        dev.adam.SpigotX.init(this);

        new EventBuilder<>(PlayerJoinEvent.class)
            .handle(ctx -> ctx.getPlayer().sendMessage("🎉 Welcome to the server, " + ctx.getPlayer().getName() + "!"))
            .register();
    }
}
```

---

## 🖼️ GUI: Interactive Menus That Wow

```java
package com.example;

import dev.adam.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiExample {
    public void openSimpleGui(Player player) {
        new GUIBuilder("💎 My First GUI", 1)
            .setItem(4, new ItemStack(Material.EMERALD), ctx -> ctx.getPlayer().sendMessage("💚 You clicked the emerald!"))
            .build()
            .open(player);
    }
}
```

---

## 📄 Paginated GUI: For Large Lists

```java
package com.example;

import dev.adam.gui.PaginatedGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class PaginatedGuiExample {
    public void openPaginatedGui(Player player) {
        PaginatedGUI pgui = new PaginatedGUI("🛒 Shop", 5);

        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            items.add(new ItemStack(Material.DIAMOND));
        }
        pgui.setContent(items);

        pgui.setPrevItem(new ItemStack(Material.ARROW));
        pgui.setNextItem(new ItemStack(Material.ARROW));

        pgui.open(player);
    }
}
```

---

## 🏷️ Placeholders: Dynamic Text Everywhere

```java
package com.example;

import dev.adam.placeholders.PlaceholderManager;
import org.bukkit.entity.Player;

public class PlaceholderExample {
    public void setupPlaceholders() {
        PlaceholderManager.register("rank", (Player p) -> "VIP");
        PlaceholderManager.register("balance", (Player p) -> "1000");
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
package com.example;

import dev.adam.gui.GUIUpdater;
import dev.adam.gui.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class AnimationUtilExample {
    public void startUpdatingGui(GUI gui) {
        GUIUpdater.scheduleRepeating(dev.adam.SpigotX.getPlugin(), gui, 20L, g -> {
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
**A:** Use your own unregister logic or `GUIUpdater.cancel(gui)`.

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
