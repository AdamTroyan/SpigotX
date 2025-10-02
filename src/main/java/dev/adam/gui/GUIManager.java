package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GUIManager implements Listener {

    private static final Map<Plugin, GUIManager> INSTANCES = new ConcurrentHashMap<>();
    private final Map<org.bukkit.inventory.Inventory, GUI> registry = new ConcurrentHashMap<>();
    private final Plugin plugin;

    private GUIManager(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static GUIManager get(Plugin plugin) {
        return INSTANCES.computeIfAbsent(plugin, GUIManager::new);
    }

    public void unregisterAll() {
        registry.keySet().forEach(inv -> registry.remove(inv));
    }

    void register(GUI gui) { if (gui != null) registry.put(gui.getInventory(), gui); }
    void unregister(GUI gui) { if (gui != null) registry.remove(gui.getInventory()); }

    GUI getGUI(org.bukkit.inventory.Inventory inv) { return registry.get(inv); }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        GUI gui = getGUI(e.getInventory());
        if (gui != null) gui.handleClick(e);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        GUI gui = getGUI(e.getInventory());
        if (gui != null) gui.handleDrag(e);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        GUI gui = getGUI(e.getInventory());
        if (gui != null) gui.handleOpen(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        GUI gui = getGUI(e.getInventory());
        if (gui != null) gui.handleClose(e);
    }
}
