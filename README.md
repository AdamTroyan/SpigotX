[![](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)
# SpigotX

SpigotX is a powerful and flexible library for creating **Minecraft Spigot/Bukkit plugins**. It simplifies command handling, event listening, and GUI creation, allowing developers to focus on plugin logic rather than boilerplate code.

---

## Table of Contents

* [1. Commands](#1-commands)
* [2. GUI](#2-gui)
* [3. Events](#3-events)
* [4. Complex Functions](#4-complex-functions)
* [5. Usage](#5-usage)
* [6. Advantages](#6-advantages)
* [7. Examples](#7-examples)
* [8. Adding via Maven / Gradle (JitPack)](#Adding-via-Maven-/-Gradle-(JitPack))
* [9. License](#9-license)

---

## 1. Commands

SpigotX provides a **dynamic and modular command system**.

### Key Features

* Register commands using **Annotations** (`@Command`).
* Supports multiple parameter types:

  * `Player` only
  * `CommandSender` + `String[] args`
  * `CommandSender` only
* Automatic permission checks (`permission()`)
* Automatic registration via `plugin.yml`
* Safe execution with error handling

### Example Usage

```java
public class MyCommands {

    @Command(name = "greet")
    public void greetCommand(Player player) {
        player.sendMessage("Hello " + player.getName() + "!");
    }

    @Command(name = "say")
    public void sayCommand(CommandSender sender, String[] args) {
        sender.sendMessage("You said: " + String.join(" ", args));
    }
}

// Register commands
new CommandManager(this, new MyCommands());
```

---

## 2. GUI

The SpigotX GUI system replaces the standard Bukkit Inventory with a modular and flexible system.

### Key Features

* Supports any Inventory type and size
* ClickHandlers for custom actions per item
* Supports both Legacy (Player only) and ClickContext (full context) handling
* Automatic handling of `InventoryClickEvent` and `InventoryCloseEvent`
* Custom listeners per GUI

### Example Usage

```java
GUI gui = new GUI(this, "My Menu", 3);

gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> {
    Player player = ctx.getPlayer();
    player.sendMessage("You clicked a diamond!");
});

gui.setItem(1, new ItemStack(Material.APPLE), ctx -> {
    if(ctx.getClicked().getType() == Material.APPLE) {
        ctx.getPlayer().sendMessage("Apple clicked!");
    }
});

gui.open(player);
```

### ClickContext

Provides:

* `getPlayer()` – the player who clicked
* `getClicked()` – the clicked ItemStack
* `getSlot()` – the inventory slot

Allows conditional checks like `if(ctx.getClicked().getType() == Material.X)`

---

## 3. Events

SpigotX provides a unified wrapper for Bukkit events, offering:

* Simple and clean registration
* Functional interface support
* Predicate filters for selective event handling
* Supports all common event types:

**Player Events:** Join, Quit, Chat, Move, Teleport, Respawn, Interact, Damage, Death, Hunger

**Inventory Events:** Click, Close

**Block Events:** Break, Place, Damage

**Vehicle Events:** Enter, Exit

**Item Events:** Pickup, Drop, Consume, HeldChange

**Other Events:** PlayerPortal, InteractEntity, CommandPreprocess, WeatherChange

* Automatic management of listener registration/unregistration

### Example Usage

```java
// Initialize plugin
Events.init(this);

// Player join event
Events.onJoin(event -> {
    Player player = event.getPlayer();
    player.sendMessage("Welcome!");
});

// BlockBreak filtered
Events.register(BlockBreakEvent.class, event -> {
    event.getPlayer().sendMessage("You broke a diamond block!");
}, event -> event.getBlock().getType() == Material.DIAMOND_BLOCK);
```

---

## 4. Complex Functions

### Events

* `register(Class<T> clazz, EventListener<T> listener, Predicate<T> filter)` – register an event with optional filtering
* `unregisterAll()` – removes all registered listeners
* `EventListener<T>` – functional interface allowing lambda usage for concise registration

### GUI

* `setItem(int slot, ItemStack item, Consumer<ClickContext> action)` – set item with full context
* ClickContext provides access to player, item, and slot for advanced logic
* Custom listeners can be added per GUI

### Commands

* Uses reflection to register methods annotated with `@Command`
* Supports multiple parameter types and automatic permission handling
* Handles errors and sender type automatically

---

## 5. Usage

**Main Plugin Class**

```java
@Override
public void onEnable() {
    Events.init(this);

    // Register commands
    new CommandManager(this, new MyCommands());

    // Example Events
    Events.onJoin(event -> {
        event.getPlayer().sendMessage("Welcome!");
    });

    // GUI
    GUI gui = new GUI(this, "Main Menu", 3);
    gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> {
        ctx.getPlayer().sendMessage("Diamond clicked!");
    });
}
```

---

## 6. Advantages

* Reduces boilerplate code
* Functional programming style for events and GUI actions
* Highly modular – each system is independent
* Custom filters can be applied to any event
* Supports all player, inventory, block, vehicle, item, and weather events

---

## 7. Examples

### Commands

```java
@Command(name="teleport")
public void teleportPlayer(Player player, String[] args){
    if(args.length == 1){
        Player target = Bukkit.getPlayer(args[0]);
        if(target != null){
            player.teleport(target.getLocation());
        }
    }
}
```

### GUI

```java
GUI gui = new GUI(this, "Shop", 4);
gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> ctx.getPlayer().sendMessage("Bought diamond!"));
```

### Events

```java
Events.onDamage(event -> {
    if(event.getEntity() instanceof Player p){
        p.sendMessage("You are being attacked!");
    }
});
```

---
## 8. Adding via Maven / Gradle (JitPack)
You can add SpigotX to your project easily using JitPack

**Gradle**
```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AdamTroyan:SpigotX:Tag'
}
```

**Maven**
```
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

Replace Tag with the release version or commit hash you want to use.
Example: v1.0.0

---

## 9. License

SpigotX is open-source and free to use in any Spigot/Bukkit plugin project.
