package org.spoofer.alchemist;

import org.api.spoofer.slibandapi.RegisterSLib;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private static Main instance;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        instance = this;
        RegisterSLib.reg(this, Menu.class);
    }

    @Override
    public void onDisable() {
        for (Player player : Menu.isActive) {
            player.closeInventory();
        }
    }

    public static Main getInstance() {
        return instance;
    }
}
