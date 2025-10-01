# SpigotX

**SpigotX** is a Java library for Spigot/Bukkit designed to simplify plugin development by providing easy-to-use modules for commands, GUI, and events. It is a fully external library that can be added as a dependency to your project.

---

## Features

### Commands Framework

* Create commands using annotations (`@Command`)
* Basic permission support
* Subcommands support
* Handles `Player`-only or generic `CommandSender` commands

### Events Utilities

* Quick registration of events using lambdas
* Supports `PlayerJoinEvent` and can be extended to other events

### GUI / Inventory API

* Create custom GUI inventories
* Handle item click events easily
* Manage GUI per player
* Add custom logic to each button effortlessly

---

## Installation

### Maven

```xml
<dependency>
    <groupId>dev.adam</groupId>
    <artifactId>spigotx</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Local usage

1. Run `mvn clean package` to build the JAR.
2. Find the JAR in `target/spigotx-1.0.0.jar`.
3. Add it to your project's `libs` folder or install it to your local Maven repository:

```bash
mvn install:install-file -Dfile=target/spigotx-1.0.0.jar \
-DgroupId=dev.adam -DartifactId=spigotx -Dversion=1.0.0 -Dpackaging=jar
```

---

## Usage Examples

### Commands

```java
import org.bukkit.entity.Player;
import dev.adam.spigotx.commands.Command;
import dev.adam.spigotx.commands.CommandManager;

public class MyCommands {

    @Command(name="heal", permission="myplugin.heal")
    public void heal(Player player) {
        player.setHealth(20);
        player.sendMessage("You have been healed!");
    }
}

// In your JavaPlugin
new CommandManager(this, new MyCommands());
```

---

### Events

```java
import dev.adam.spigotx.events.Events;

Events.onJoin(this, player -> player.sendMessage("Welcome to the server!"));
```

---

### GUI

```java
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.adam.spigotx.gui.GUI;

GUI menu = new GUI(this, "Shop", 3);
menu.setItem(0, new ItemStack(Material.DIAMOND), player -> player.sendMessage("You bought a diamond!"));
menu.open(player);
```

---

## Requirements

* Java 17+
* Spigot API 1.20.1+
* Maven (for building the library)

---

## Contributing

Contributions are welcome! 💡
You can propose improvements for Commands, GUI, or Events via GitHub Issues and Pull Requests.

---

## License

MIT License © 2025 [Your Name]
