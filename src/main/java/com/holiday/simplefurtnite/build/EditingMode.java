package com.holiday.simplefurtnite.build;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class EditingMode {

    UUID uuid;
    ItemDisplay itemDisplay;
    float rotation;
    BukkitRunnable task;
    boolean placed;

    public EditingMode(UUID uuid, ItemDisplay itemDisplay, float rotation) {
        this.uuid = uuid;
        this.itemDisplay = itemDisplay;
        this.rotation = rotation;
        this.placed = true;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }

    public boolean isPlaced() {
        return placed;
    }

    public UUID getUuid() {
        return uuid;
    }

    public BukkitRunnable getTask() {
        return task;
    }

    public float getRotation() {
        return rotation;
    }

    public ItemDisplay getItemDisplay() {
        return itemDisplay;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public void setTask(BukkitRunnable task) {
        this.task = task;
    }
}
