package com.holiday.simplefurtnite.commands;

import com.holiday.simplefurtnite.Main;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MainCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender instanceof Player player) {

            if (args.length == 0) {
                return true;
            }

            if (!player.isOp()) {
                return true;
            }

            String name = args[0];

            switch (name) {
                case "add" -> {
                    if (args.length < 2) {
                        player.sendMessage("§cUsage: add <SMALL|MEDIUM|HIGH>");
                        return true;
                    }

                    String size = args[1].toUpperCase();
                    if (!size.equals("SMALL") && !size.equals("MEDIUM") && !size.equals("HIGH")) {
                        player.sendMessage("§cREQUIRED SMALL, MEDIUM, HIGH");
                        return true;
                    }

                    ItemStack itemStack = player.getInventory().getItemInMainHand();
                    CustomStack customFurniture = CustomFurniture.byItemStack(itemStack);
                    if (customFurniture == null) return true;

                    List<String> available = Main.getPlugin(Main.class).getConfig().getStringList("whitelist");
                    String entry = customFurniture.getId() + ":" + size;

                    boolean exists = available.stream().anyMatch(e -> e.startsWith(customFurniture.getId() + ":"));
                    if (!exists) {
                        available.add(entry);
                        Main.getPlugin(Main.class).getConfig().set("whitelist", available);
                        Main.getPlugin(Main.class).saveConfig();
                    }
                    break;
                }
                case "remove" -> {
                    ItemStack itemStack = player.getInventory().getItemInMainHand();
                    CustomStack customFurniture = CustomFurniture.byItemStack(itemStack);
                    if (customFurniture == null) return true;

                    List<String> available = Main.getPlugin(Main.class).getConfig().getStringList("whitelist");

                    String entryToRemove = null;
                    for (String entry : available) {
                        if (entry.startsWith(customFurniture.getId() + ":")) {
                            entryToRemove = entry;
                            break;
                        }
                    }

                    if (entryToRemove != null) {
                        available.remove(entryToRemove);
                        Main.getPlugin(Main.class).getConfig().set("whitelist", available);
                        Main.getPlugin(Main.class).saveConfig();
                        Main.getPlugin(Main.class).getLogger().info("Deleted!");
                    }
                    break;
                }
            }
        }
        return false;
    }
}
