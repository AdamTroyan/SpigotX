[![](https://jitpack.io/v/AdamTroyan/SpigotX.svg)](https://jitpack.io/#AdamTroyan/SpigotX)
# SpigotX

SpigotX is a powerful and easy-to-use **Spigot/Bukkit library** designed to simplify plugin development. It provides utilities for **commands, GUI management, and event handling**, so you can focus on your plugin logic without repetitive boilerplate.

---

## Features

### CommandManager

* Register commands using `@Command` annotation.
* Supports `args` automatically.
* Works with both `Player` and `CommandSender`.
* Permission support.
* Subcommands ready for extension.

**Usage:**

```java
public class MyCommands {

    @Command(name = "greet")
    public void greet(Player player, String[] args) {
        if(args.length == 0) {
            player.sendMessage("Hello!");
        } else {
            player.sendMessage("Hello " + String.join(" ", args) + "!");
        }
    }
}

// In your plugin main class
new CommandManager(this, new MyCommands());
```

**plugin.yml:**

```yaml
commands:
  greet:
    description: Greets the player
```

---

### Events

* Simple event registration using lambdas.
* Covers **Player, Block, Inventory, Vehicle, Weather** events.
* Examples below show usage.

**Player Events:**

```java
Events.onJoin(player -> player.sendMessage("Welcome!"));
Events.onQuit(player -> Bukkit.broadcastMessage(player.getName() + " left."));
Events.onChat((player, message) -> player.sendMessage("You said: " + message));
Events.onMove((player, event) -> { /* handle movement */ });
Events.onDamage((player, event) -> { /* handle damage */ });
Events.onDeath((player, event) -> { /* handle death */ });
```

**Block Events:**

```java
Events.onBlockBreak((player, event) -> player.sendMessage("You broke a block!"));
Events.onBlockPlace((player, event) -> player.sendMessage("You placed a block!"));
```

**Inventory Events:**

```java
Events.onInventoryClick((player, event) -> { /* handle click */ });
Events.onInventoryClose((player, event) -> { /* handle close */ });
```

**Vehicle Events:**

```java
Events.onVehicleEnter((player, event) -> { /* handle enter */ });
Events.onVehicleExit((player, event) -> { /* handle exit */ });
```

**Weather Events:**

```java
Events.onWeatherChange(event -> { /* handle weather change */ });
```

---

### GUI

* Create interactive inventories easily.
* Attach click actions to items.
* Supports opening, clearing, and dynamic item updates.

**Usage:**

```java
GUI gui = new GUI(this, "My Menu", 3);

gui.setItem(0, new ItemStack(Material.DIAMOND), player -> {
    player.sendMessage("You clicked the diamond!");
});

gui.open(player);
```

**Additional features:**

* Pagination for long lists (planned)
* onClose callbacks
* Dynamic item updates

---

### GUIButton

* Represents a clickable item in GUI.
* Simple API using lambdas.

---

## Installation (via JitPack)

1. Add the JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add SpigotX as a dependency:

```xml
<dependency>
    <groupId>com.github.AdamTroyan</groupId>
    <artifactId>SpigotX</artifactId>
    <version>v1.0.0</version>
</dependency>
```

* Make sure the tag `v1.0.0` exists in your GitHub repository.

---

## Notes

* Ensure that all commands used with `CommandManager` are defined in `plugin.yml`.
* Events are automatically registered using Bukkit's event system.
* GUI actions are thread-safe when triggered from player interactions.

---

## Future Plans

* Support for subcommands with automatic help messages.
* Built-in permission checking and messages.
* Pagination utilities for GUIs.
* Expanded event helpers for more Bukkit events.

---

## License

MIT License
