package regalowl.simpledatalib.bukkit;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bukkit.plugin.java.JavaPlugin;

import regalowl.simpledatalib.SimpleDataLib;

/**
 * Specialized YamlHandler for Bukkit/Paper plugins.
 * Manages BukkitYamlConfiguration instances with auto-save functionality,
 * resource loading, and version-aware config updates.
 */
public class BukkitYamlHandler {
    
    private final SimpleDataLib sdl;
    private final JavaPlugin plugin;
    private final ConcurrentHashMap<String, BukkitYamlConfiguration> configs;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> saveTaskFuture;
    private Long saveInterval;
    
    /**
     * Creates a new BukkitYamlHandler.
     * 
     * @param sdl The SimpleDataLib instance
     * @param plugin The JavaPlugin instance (can be null if not in Bukkit environment)
     */
    public BukkitYamlHandler(SimpleDataLib sdl, JavaPlugin plugin) {
        this.sdl = sdl;
        this.plugin = plugin;
        this.configs = new ConcurrentHashMap<>();
        this.scheduler = null;
        this.saveTaskFuture = null;
        this.saveInterval = null;
    }
    
    /**
     * Registers a configuration file for management.
     * 
     * @param fileName The name of the configuration file (without .yml extension)
     * @param defaultResourcePath The path to the default resource in the plugin JAR (e.g., "config.yml")
     * @return The BukkitYamlConfiguration instance, or null if registration failed
     */
    public BukkitYamlConfiguration registerConfiguration(String fileName, String defaultResourcePath) {
        File configFile;
        if (plugin != null) {
            configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        } else {
            configFile = new File(sdl.getStoragePath(), fileName + ".yml");
        }
        
        BukkitYamlConfiguration config = new BukkitYamlConfiguration(sdl, plugin, configFile);
        if (config.load(defaultResourcePath != null ? defaultResourcePath : (fileName + ".yml"))) {
            configs.put(fileName, config);
            return config;
        } else {
            // Still register even if load failed (might be a new file)
            configs.put(fileName, config);
            return config;
        }
    }
    
    /**
     * Registers a configuration file for management without a default resource.
     * 
     * @param fileName The name of the configuration file (without .yml extension)
     * @return The BukkitYamlConfiguration instance, or null if registration failed
     */
    public BukkitYamlConfiguration registerConfiguration(String fileName) {
        return registerConfiguration(fileName, null);
    }
    
    /**
     * Unregisters a configuration file.
     * Saves the configuration before unregistering if auto-save is enabled.
     * 
     * @param fileName The name of the configuration file (without .yml extension)
     */
    public void unregisterConfiguration(String fileName) {
        if (configs.containsKey(fileName)) {
            BukkitYamlConfiguration config = configs.get(fileName);
            if (config.isAutoSave()) {
                config.save();
            }
            configs.remove(fileName);
        }
    }
    
    /**
     * Gets a registered configuration.
     * 
     * @param fileName The name of the configuration file (without .yml extension)
     * @return The BukkitYamlConfiguration instance, or null if not registered
     */
    public BukkitYamlConfiguration getConfiguration(String fileName) {
        return configs.get(fileName);
    }
    
    /**
     * Saves a specific configuration file.
     * 
     * @param fileName The name of the configuration file (without .yml extension)
     * @return true if saved successfully, false otherwise
     */
    public boolean saveConfiguration(String fileName) {
        BukkitYamlConfiguration config = configs.get(fileName);
        if (config != null) {
            return config.save();
        }
        return false;
    }
    
    /**
     * Saves all registered configurations.
     */
    public void saveAllConfigurations() {
        for (BukkitYamlConfiguration config : configs.values()) {
            if (config.isAutoSave()) {
                config.save();
            }
        }
    }
    
    /**
     * Starts the auto-save task that periodically saves all configurations with auto-save enabled.
     * 
     * @param interval The interval in milliseconds between saves
     */
    public void startAutoSaveTask(long interval) {
        this.saveInterval = interval;
        stopAutoSaveTask();
        
        if (scheduler == null) {
            scheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "SimpleDataLib-BukkitYamlSaveTask");
                t.setDaemon(true);
                return t;
            });
        }
        
        saveTaskFuture = scheduler.scheduleWithFixedDelay(this::saveAllConfigurations, interval, interval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Stops the auto-save task.
     */
    public void stopAutoSaveTask() {
        if (saveTaskFuture != null) {
            saveTaskFuture.cancel(false);
            saveTaskFuture = null;
        }
    }
    
    /**
     * Gets the current auto-save interval.
     * 
     * @return The interval in milliseconds, or null if not set
     */
    public Long getSaveInterval() {
        return saveInterval;
    }
    
    /**
     * Shuts down the handler, saving all configurations and stopping the auto-save task.
     */
    public void shutdown() {
        stopAutoSaveTask();
        saveAllConfigurations();
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Checks if a configuration file is registered.
     * 
     * @param fileName The name of the configuration file (without .yml extension)
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(String fileName) {
        return configs.containsKey(fileName);
    }
    
    /**
     * Gets the number of registered configurations.
     * 
     * @return The number of registered configurations
     */
    public int getConfigurationCount() {
        return configs.size();
    }
}

