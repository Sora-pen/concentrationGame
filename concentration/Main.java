package org.example.plugin.concentration;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.plugin.concentration.Command.Concentration;

public final class Main extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    Concentration concentration = new Concentration(this);
    getCommand("concentration").setExecutor(concentration);
    Bukkit.getPluginManager().registerEvents((Listener) concentration, this);
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
  }
}
