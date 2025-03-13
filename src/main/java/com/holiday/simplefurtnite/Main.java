package com.holiday.simplefurtnite;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.holiday.simplefurtnite.build.EditingMode;
import com.holiday.simplefurtnite.commands.MainCommand;
import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.units.qual.N;

import java.util.*;


public final class Main extends JavaPlugin implements Listener {

    private final Set<EditingMode> editingModeSet = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("furtnites").setExecutor(new MainCommand());
    }

    @Override
    public void onDisable() {
        editingModeSet.forEach(mode -> {
            if (mode.getItemDisplay() != null) mode.getItemDisplay().remove();
            if (mode.getTask() != null) mode.getTask().cancel();
        });
        editingModeSet.clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Location location = event.getPlayer().getLocation();
        double radius = 2.0;

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
                if (entity instanceof ItemDisplay display) {
                    if (display.getItemStack().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "decore"), PersistentDataType.BOOLEAN)) {
                        if (isPlayerInRegion(event.getPlayer(), location)) {
                            display.getWorld().dropItem(display.getLocation(), display.getItemStack());
                            display.remove();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;

        CustomStack customFurniture = CustomFurniture.byItemStack(event.getItem());
        if (customFurniture == null) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        EditingMode existingMode = getEditingMode(playerId);
        if (existingMode != null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                event.setCancelled(true);
                if (existingMode.isPlaced()) {
                    if (isPlayerInRegion(player, existingMode.getItemDisplay().getLocation())) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        item.setAmount(item.getAmount() - 1);
                        existingMode.getItemDisplay().getPersistentDataContainer().set(new NamespacedKey(this, "decore"), PersistentDataType.STRING, item.getType().toString());

                        ItemStack itemStack = existingMode.getItemDisplay().getItemStack();
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        itemStack.setAmount(1);
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            itemMeta.getPersistentDataContainer().set(new NamespacedKey(this, "decore"), PersistentDataType.BOOLEAN, true);
                            itemStack.setItemMeta(itemMeta);
                            existingMode.getItemDisplay().setItemStack(itemStack);
                        }, 12);
                        existingMode.getItemDisplay().setGlowing(false);
                        existingMode.getTask().cancel();
                        editingModeSet.remove(existingMode);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        EditingMode mode = getEditingMode(playerId);

        if (mode != null && player.isSneaking()) {

            float currentYaw = mode.getRotation();
            currentYaw += (event.getNewSlot() > event.getPreviousSlot()) ? 5f : -5f;
            currentYaw = (currentYaw + 360f) % 360f;
            mode.setRotation(currentYaw);

            Location newLocation = mode.getItemDisplay().getLocation();
            newLocation.setYaw(currentYaw);
            mode.getItemDisplay().teleport(newLocation);

            event.setCancelled(true);
            return;
        }

        if (mode != null) {
            mode.getItemDisplay().setGlowing(false);
            mode.getItemDisplay().remove();
            mode.getTask().cancel();
            editingModeSet.remove(mode);

            ItemStack itemStack = event.getPlayer().getInventory().getItem(event.getNewSlot());
            CustomStack customFurniture = CustomFurniture.byItemStack(itemStack);
            if (customFurniture == null) return;

            List<String> rawList = Main.getPlugin(Main.class).getConfig().getStringList("whitelist");

            Map<String, String> furnitureSizes = new HashMap<>();
            List<String> available = new ArrayList<>();

            for (String entry : rawList) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    String size = parts[1].trim();
                    available.add(name);
                    furnitureSizes.put(name, size);
                }
            }

            if (available.contains(customFurniture.getId())) {

                String furnitureSize = furnitureSizes.get(customFurniture.getId());

                ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class);
                display.setItemStack(itemStack);
                display.setGlowing(true);

                EditingMode modeS = getEditingMode(playerId);
                if (modeS != null) {
                    modeS.getItemDisplay().setGlowing(false);
                    modeS.getTask().cancel();
                    modeS.getItemDisplay().remove();
                    editingModeSet.remove(mode);
                }

                EditingMode newMode = new EditingMode(playerId, display, player.getLocation().getYaw());
                editingModeSet.add(newMode);

                BukkitRunnable task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline() || !display.isValid()) {
                            display.remove();
                            editingModeSet.remove(newMode);
                            cancel();
                            return;
                        }

                        Location newLocation = getSafeLocation(player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(3)));
                        boolean collides = !newLocation.getBlock().isPassable();
                        boolean isFloating = isFloating(newLocation, furnitureSize);

                        display.setGlowColorOverride(collides ? Color.RED : (isFloating ? Color.ORANGE : Color.GREEN));
                        newLocation.setPitch(0);
                        newLocation.setYaw(newMode.getRotation());
                        display.teleport(newLocation);
                    }
                };
                task.runTaskTimer(this, 0L, 1L);
                newMode.setTask(task);
                return;
            }
        }
        ItemStack itemStack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        CustomStack customFurniture = CustomFurniture.byItemStack(itemStack);
        if (customFurniture == null) return;

        List<String> rawList = Main.getPlugin(Main.class).getConfig().getStringList("whitelist");

        Map<String, String> furnitureSizes = new HashMap<>();
        List<String> available = new ArrayList<>();

        for (String entry : rawList) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String name = parts[0].trim();
                String size = parts[1].trim();
                available.add(name);
                furnitureSizes.put(name, size);
            }
        }

        if (available.contains(customFurniture.getId())) {

            String furnitureSize = furnitureSizes.get(customFurniture.getId());

            ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class);
            display.setItemStack(itemStack);
            display.setGlowing(true);

            EditingMode newMode = new EditingMode(playerId, display, player.getLocation().getYaw());
            editingModeSet.add(newMode);

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {

                    if (!player.getInventory().getItemInMainHand().isSimilar(itemStack)) {
                        display.remove();
                        editingModeSet.remove(newMode);
                        cancel();
                        return;
                    }

                    if (!player.isOnline() || !display.isValid()) {
                        display.remove();
                        editingModeSet.remove(newMode);
                        cancel();
                        return;
                    }

                    Location newLocation = getSafeLocation(player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(3)));
                    boolean collides = !newLocation.getBlock().isPassable();
                    boolean isFloating = isFloating(newLocation, furnitureSize);

                    if (collides) {
                        display.setGlowColorOverride(Color.RED);
                        newMode.setPlaced(false);

                    } else if (hasSideSupport(newLocation)) {
                        display.setGlowColorOverride(Color.GREEN);
                        newMode.setPlaced(true);

                    } else if (!isPlayerInRegion(player, newLocation)) {
                        display.setGlowColorOverride(Color.RED);
                        newMode.setPlaced(true);
                    } else if (isFloating) {
                        display.setGlowColorOverride(Color.ORANGE);
                        newMode.setPlaced(false);

                    } else {
                        display.setGlowColorOverride(Color.GREEN);
                        newMode.setPlaced(true);
                    }

                    newLocation.setPitch(0);
                    newLocation.setYaw(newMode.getRotation());
                    display.teleport(newLocation);
                }
            };
            task.runTaskTimer(this, 0L, 1L);
            newMode.setTask(task);
        }
    }

    private EditingMode getEditingMode(UUID playerId) {
        return editingModeSet.stream().filter(mode -> mode.getUuid().equals(playerId)).findFirst().orElse(null);
    }

    private Location getSafeLocation(Location location) {
        for (int i = 0; i < 10; i++) {
            Location checkLocation = location.clone().add(0, i, 0);
            if (checkLocation.getBlock().isPassable() && checkLocation.clone().add(0, 1, 0).getBlock().isPassable()) {
                return checkLocation;
            }
        }
        return location;
    }

    private boolean isFloating(Location location, String furnitureSize) {
        switch (furnitureSize) {
            case "SMALL" -> {
                return location.clone().subtract(0, 0.5, 0).getBlock().isPassable();
            }

            case "MEDIUM" -> {
                return location.clone().subtract(0, 1.0, 0).getBlock().isPassable();
            }

            case "HIGH" -> {
                return location.clone().subtract(0, 1.5, 0).getBlock().isPassable();
            }
        }
        return location.clone().subtract(0, 0.5, 0).getBlock().isPassable();
    }

    private boolean hasSideSupport(Location location) {
        return !location.clone().add(0.5, 0, 0).getBlock().isPassable() ||  // Este
                !location.clone().add(-0.5, 0, 0).getBlock().isPassable() || // Oeste
                !location.clone().add(0, 0, 0.5).getBlock().isPassable() ||  // Sur
                !location.clone().add(0, 0, -0.5).getBlock().isPassable();   // Norte
    }

    public boolean isPlayerInRegion(Player player, Location location) {
        World world = player.getWorld(); // Obtenemos el mundo en el que está el jugador
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

        if (regionManager != null) {
            ApplicableRegionSet regionSet = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            if (regionSet.size() != 0) {
                for (ProtectedRegion region : regionSet) {
                    StateFlag.State blockBreakFlag = region.getFlag(Flags.BLOCK_PLACE);
                    if (blockBreakFlag == StateFlag.State.ALLOW) {
                        return true;
                    }
                    if (player.isOp()) {
                        return true;
                    }
                    if (region.getOwners().contains(player.getUniqueId())) {
                        return true; // El jugador es propietario de la región
                    } else if (region.getMembers().contains(player.getUniqueId())) {
                        return true; // El jugador es miembro de la región
                    }
                }
                return false; // El jugador no tiene acceso
            } else {
                return true; // No hay regiones en esta ubicación
            }
        } else {
            return true;
        }
    }
}
