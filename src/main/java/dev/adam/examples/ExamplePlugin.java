package dev.adam.examples;

import dev.adam.SpigotX;
import dev.adam.commands.CommandBuilder;
import dev.adam.gui.GUIBuilder;
import dev.adam.events.EventBuilder;
import dev.adam.config.Config;
import dev.adam.placeholders.PlaceholderManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;

public class ExamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        SpigotX.init(this);

        // Register placeholder
        PlaceholderManager.register("rank", p -> "VIP");

        // Command Example
        new CommandBuilder()
            .name("menu")
            .description("Open the menu")
            .executor((sender, args) -> {
                if (sender instanceof Player player) {
                    new GUIBuilder("Menu", 1)
                        .setItem(0, new ItemStack(Material.EMERALD), ctx -> ctx.getPlayer().sendMessage("Clicked!"))
                        .build()
                        .open(player);
                }
            })
            .register();

        // Event Example
        new EventBuilder<>(PlayerJoinEvent.class)
            .handle(ctx -> ctx.getPlayer().sendMessage("Welcome, {rank}".replace("{rank}", PlaceholderManager.parse(ctx.getPlayer(), "{rank}"))))
            .register();

        // Config Example
        Config config = new Config(this, "settings.yml");
        String prefix = config.getString("chat.prefix", "&7[Server]");
    }
}