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

        // 2. Register commands (choose your favorite way!)
        // --- Option 1: Builder style
        new dev.adam.commands.CommandBuilder()
            .name("hello")
            .description("Say hello")
            .executor((sender, args) -> sender.sendMessage("Hello from CommandBuilder!"))
            .register();

        // --- Option 2: CommandManager (annotation class)
        new dev.adam.commands.CommandManager(this, new MyCommands());

        // --- Option 3: SpigotX static register (annotation class, recommended)
        SpigotX.registerCommand(new MyOtherCommands());

        // 3. Register events (see below for all options)
        dev.adam.events.EventUtil.listen(this, org.bukkit.event.player.PlayerJoinEvent.class, event -> {
            event.getPlayer().sendMessage("Welcome with EventUtil!");
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

    pgui.setPrevItem(new ItemStack(Material.ARROW));
    pgui.setNextItem(new ItemStack(Material.ARROW));

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
- **Use PaginatedGUI for any list over 9 items.**
- **Use Animation and GUIUpdater for dynamic GUIs.**
- **Use EventUtil for quick event listeners.**
- **Use @AsyncCommand for anything that touches a database or external API.**
- **Use TabHandler for smart tab completion.**
- **Use GUIBuilder for all your menus, and PaginatedGUI for shops, lists, etc.**
- **Use PlaceholderManager for all dynamic text, including GUI titles and messages.**
- **Use CommandBuilder for quick, one-off commands.**
- **Use CommandManager or SpigotX.registerCommand for annotation-based command classes.**
- **Use EventBuilder for advanced event handling with context.**
- **Use EventUtil.listen for simple lambda-based event handling.**
- **Use SpigotX.on or SpigotX.onEvent for static sugar.**
- **Use GUIListener and GUIUpdater for all GUIs.**
- **Use Animation for animated items in GUIs.**
- **Use fillBorder in GUIBuilder for quick border design.**
- **Use setOnOpen and setOnClose in GUIBuilder for open/close hooks.**
- **Use setItemHandler in PaginatedGUI for per-item click actions.**
- **Use setPrevHandler and setNextHandler in PaginatedGUI for custom navigation.**
- **Use cancel in GUIClickContext to cancel the event.**
- **Use sendMessage in GUIClickContext for quick messaging.**
- **Use getClickedItem in GUIClickContext for item info.**
- **Use getSlot in GUIClickContext for slot info.**
- **Use getEvent in GUIClickContext for full event access.**
- **Use getPlayer in GUIClickContext for player access.**
- **Use reset in Animation to restart animation.**
- **Use cancel and cancelAll in GUIUpdater to stop updates.**
- **Use getOpenGui in GUIListener to get the current GUI for a player.**
- **Use closeGui in GUIListener to close a GUI for a player.**
- **Use openGui in GUIListener to open a GUI for a player.**
- **Use setOnOpen and setOnClose in GUI for open/close hooks.**
- **Use fillBorder in GUI for quick border design.**
- **Use setItem in GUI for per-slot actions.**
- **Use setContent in PaginatedGUI for list content.**
- **Use setPrevItem and setNextItem in PaginatedGUI for navigation.**
- **Use setPrevHandler and setNextHandler in PaginatedGUI for navigation actions.**
- **Use updatePage in PaginatedGUI to refresh the page.**
- **Use handleClick in GUI for custom click handling.**
- **Use handleClose in GUI for custom close handling.**
- **Use getInventory in GUI for inventory access.**
- **Use getRows in GUI for row count.**
- **Use getTitle in GUI for title.**
- **Use getPage in PaginatedGUI for current page.**
- **Use getItemsPerPage in PaginatedGUI for items per page.**
- **Use getContent in PaginatedGUI for content list.**
- **Use getItemHandlers in PaginatedGUI for handlers.**
- **Use getPrevSlot and getNextSlot in PaginatedGUI for navigation slots.**
- **Use getBottomRowStart and getBottomRowEnd in PaginatedGUI for bottom row slots.**

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
