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
    <version>v1.4.3</version>
</dependency>
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.AdamTroyan:SpigotX:v1.4.3'
}
```

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.AdamTroyan:SpigotX:v1.4.3")
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

        // 2. Register commands (choose your favorite way!)
        new dev.adam.commands.CommandBuilder()
            .name("hello")
            .description("Say hello")
            .executor((sender, args) -> sender.sendMessage("Hello from CommandBuilder!"))
            .register();

        // 3. Register annotation-based commands
        SpigotX.registerCommand(new MyCommands());

        // 4. Register events
        SpigotX.on(org.bukkit.event.player.PlayerJoinEvent.class, ctx -> {
            ctx.getPlayer().sendMessage("Welcome with SpigotX.on!");
        });
    }
}
```

---

## 🏷️ Annotation-Based Commands

SpigotX supports annotation-based commands for maximum simplicity and power.  
Just annotate your methods and register the class instance:

```java
package com.example;

import dev.adam.commands.annotations.Command;
import dev.adam.commands.annotations.AsyncCommand;
import dev.adam.commands.annotations.TabComplete;
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
        sender.sendMessage("Looking up stats...");
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

## 🧑‍💻 Three Ways to Register Commands

You can register commands in any of these ways:

### 1. **CommandBuilder (classic builder style)**
```java
new dev.adam.commands.CommandBuilder()
    .name("hello")
    .description("Say hello")
    .executor((sender, args) -> sender.sendMessage("Hello from CommandBuilder!"))
    .register();
```

### 2. **CommandManager (annotation class)**
```java
new dev.adam.commands.CommandManager(this, new MyCommands());
```

### 3. **SpigotX static registerCommand (annotation class, recommended)**
```java
dev.adam.SpigotX.registerCommand(new MyCommands());
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

SpigotX supports multiple ways to register events, including a super-simple lambda-based API!

### 1. Using EventBuilder

```java
import dev.adam.events.EventBuilder;
import org.bukkit.event.player.PlayerJoinEvent;

new EventBuilder<>(PlayerJoinEvent.class)
    .handle(ctx -> ctx.getPlayer().sendMessage("🎉 Welcome to the server, " + ctx.getPlayer().getName() + "!"))
    .register();
```

### 2. Using EventUtil (Lambda, no annotation, no EventBuilder)

```java
import dev.adam.events.EventUtil;
import org.bukkit.event.player.PlayerJoinEvent;

EventUtil.listen(this, PlayerJoinEvent.class, event -> {
    event.getPlayer().sendMessage("Welcome with EventUtil!");
});
```

### 3. Using SpigotX.on (if you want static sugar)

```java
SpigotX.on(PlayerJoinEvent.class, ctx -> {
    ctx.getPlayer().sendMessage("Welcome with SpigotX.on!");
});
```

---

## 🖼️ GUI: Interactive Menus That Wow

### Simple GUI

```java
import dev.adam.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GuiExample {
    public void openSimpleGui(Player player) {
        new GUIBuilder("💎 My First GUI", 1)
            .setItem(4, new ItemStack(Material.EMERALD), ctx -> ctx.getPlayer().sendMessage("💚 You clicked the emerald!"))
            .open(player);
    }
}
```

### Animated GUI

```java
import dev.adam.gui.GUIBuilder;
import dev.adam.gui.Animation;
import dev.adam.gui.GUIUpdater;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public void openAnimatedGui(Player player) {
    Animation anim = new Animation(List.of(
        new ItemStack(Material.RED_WOOL),
        new ItemStack(Material.GREEN_WOOL),
        new ItemStack(Material.BLUE_WOOL)
    ), 10L);

    GUIBuilder builder = new GUIBuilder("Animated", 1)
        .setItem(4, anim.nextFrame(), ctx -> ctx.getPlayer().sendMessage("Clicked!"));

    GUIUpdater.scheduleRepeating(SpigotX.getPlugin(), builder.build(), anim.getTicksPerFrame(), gui -> {
        gui.setItem(4, anim.nextFrame(), null);
    });

    builder.open(player);
}
```

---

## 📄 Paginated GUI: For Large Lists

PaginatedGUI makes it easy to show large lists with navigation.

```java
import dev.adam.gui.PaginatedGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public void openPaginatedGui(Player player) {
    PaginatedGUI pgui = new PaginatedGUI("🛒 Shop", 5);

    List<ItemStack> items = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
        items.add(new ItemStack(Material.DIAMOND));
    }
    pgui.setContent(items);

    pgui.setPrevButton(new ItemStack(Material.ARROW));
    pgui.setNextButton(new ItemStack(Material.ARROW));

    // Optional: set handler for each item
    for (int i = 0; i < items.size(); i++) {
        int index = i;
        pgui.setItemHandler(i, ctx -> ctx.getPlayer().sendMessage("Clicked item #" + index));
    }

    pgui.open(player);
}
```

---

## 🏷️ Placeholders: Dynamic Text Everywhere

```java
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
import dev.adam.gui.GUIUpdater;
import dev.adam.gui.GUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public void startUpdatingGui(GUI gui) {
    GUIUpdater.scheduleRepeating(dev.adam.SpigotX.getPlugin(), gui, 20L, g -> {
        g.setItem(0, new ItemStack(Material.values()[(int) (Math.random() * Material.values().length)]), null);
    });
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
**A:** Make sure you registered it with one of the supported ways (`CommandBuilder`, `CommandManager`, or `SpigotX.registerCommand`) and annotated it with `@Command`.

**Q:** How do I update a GUI for all viewers?  
**A:** Use `gui.updateAllViewers();`

**Q:** Can I use SpigotX with Paper or Purpur?  
**A:** Yes! SpigotX is compatible with all Spigot forks.

**Q:** How do I unregister an event or GUI updater?  
**A:** Use your own unregister logic or `GUIUpdater.cancel(gui)`.

**Q:** How do I listen to events without annotations?  
**A:** Use `EventUtil.listen(plugin, EventClass.class, event -> { ... });`

**Q:** How do I make a command async?  
**A:** Add `@AsyncCommand` to your command method.

**Q:** How do I add tab completion?  
**A:** Implement `TabHandler` and use `@TabComplete(handler = MyTabHandler.class)`.

**Q:** How do I animate a GUI item?  
**A:** Use `Animation` and `GUIUpdater.scheduleRepeating`.

**Q:** How do I make a paginated GUI with navigation at the bottom row?  
**A:** Use `PaginatedGUI` with 5+ rows, set prev/next items, and set content.

**Q:** How do I use placeholders in GUI titles?  
**A:** Register with `PlaceholderManager.register` and use `{placeholder}` in the title.

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

**This README is just the beginning. For full documentation, advanced usage, and more examples, see the [JavaDoc](https://adamtroyan.github.io/SpigotX-Javadoc/) and the source code!**

---

*Happy coding and may your plugins be bug-free and full of features!* 🚀
