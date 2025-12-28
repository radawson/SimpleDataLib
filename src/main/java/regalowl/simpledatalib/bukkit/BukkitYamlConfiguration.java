package regalowl.simpledatalib.bukkit;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import regalowl.simpledatalib.SimpleDataLib;

/**
 * Wrapper for Bukkit's YamlConfiguration that integrates with SimpleDataLib features.
 * Provides auto-save functionality, version management, and resource loading utilities
 * while maintaining full Bukkit API compatibility.
 */
public class BukkitYamlConfiguration {
    
    private final SimpleDataLib sdl;
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration bukkitConfig;
    private boolean autoSave;
    private String versionKey;
    
    /**
     * Creates a new BukkitYamlConfiguration instance.
     * 
     * @param sdl The SimpleDataLib instance
     * @param plugin The JavaPlugin instance (can be null if not in Bukkit environment)
     * @param file The configuration file
     */
    public BukkitYamlConfiguration(SimpleDataLib sdl, JavaPlugin plugin, File file) {
        this.sdl = sdl;
        this.plugin = plugin;
        this.file = file;
        this.bukkitConfig = YamlConfiguration.loadConfiguration(file);
        this.autoSave = false;
        this.versionKey = "version";
    }
    
    /**
     * Gets the underlying Bukkit FileConfiguration.
     * This allows full access to Bukkit's YAML API.
     * 
     * @return The Bukkit FileConfiguration instance
     */
    public FileConfiguration getBukkitConfig() {
        return bukkitConfig;
    }
    
    /**
     * Gets the configuration file.
     * 
     * @return The File object
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Loads the configuration from the file.
     * If the file doesn't exist, creates it from the default resource if available.
     * 
     * @param defaultResourcePath The path to the default resource in the plugin JAR (e.g., "config.yml")
     * @return true if loaded successfully, false otherwise
     */
    public boolean load(String defaultResourcePath) {
        try {
            if (!file.exists()) {
                // Try to copy from JAR resource
                if (plugin != null && defaultResourcePath != null) {
                    if (sdl.getFileTools().fileExists(defaultResourcePath)) {
                        sdl.getFileTools().copyFileFromJar(defaultResourcePath, file.getAbsolutePath());
                    } else if (plugin.getResource(defaultResourcePath) != null) {
                        // Use Bukkit's saveResource if available
                        plugin.saveResource(defaultResourcePath, false);
                        // File might be saved to data folder, need to move it if needed
                        File dataFolderFile = new File(plugin.getDataFolder(), defaultResourcePath);
                        if (dataFolderFile.exists() && !dataFolderFile.equals(file)) {
                            sdl.getFileTools().copyFile(dataFolderFile.getAbsolutePath(), file.getAbsolutePath());
                        }
                    }
                }
            }
            
            if (file.exists()) {
                bukkitConfig = YamlConfiguration.loadConfiguration(file);
                return true;
            } else {
                // Create empty config if file doesn't exist
                bukkitConfig = new YamlConfiguration();
                return false;
            }
        } catch (Exception e) {
            sdl.getErrorWriter().writeError(e, "Failed to load Bukkit YAML configuration: " + file.getAbsolutePath());
            bukkitConfig = new YamlConfiguration();
            return false;
        }
    }
    
    /**
     * Loads the configuration from the file without a default resource.
     * 
     * @return true if loaded successfully, false otherwise
     */
    public boolean load() {
        return load(null);
    }
    
    /**
     * Saves the configuration to the file.
     * 
     * @return true if saved successfully, false otherwise
     */
    public boolean save() {
        try {
            // Ensure parent directory exists
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                sdl.getFileTools().makeFolder(parent.getAbsolutePath());
            }
            
            bukkitConfig.save(file);
            return true;
        } catch (IOException e) {
            sdl.getErrorWriter().writeError(e, "Failed to save Bukkit YAML configuration: " + file.getAbsolutePath());
            return false;
        }
    }
    
    /**
     * Reloads the configuration from the file.
     * 
     * @return true if reloaded successfully, false otherwise
     */
    public boolean reload() {
        return load();
    }
    
    /**
     * Checks if the configuration version matches the expected version.
     * 
     * @param expectedVersion The expected version string
     * @return true if versions match, false otherwise
     */
    public boolean checkVersion(String expectedVersion) {
        String currentVersion = bukkitConfig.getString(versionKey, "0.0.0");
        return expectedVersion != null && expectedVersion.equals(currentVersion);
    }
    
    /**
     * Sets the version key used for version checking.
     * Default is "version".
     * 
     * @param versionKey The key to use for version checking
     */
    public void setVersionKey(String versionKey) {
        this.versionKey = versionKey;
    }
    
    /**
     * Gets the current version from the configuration.
     * 
     * @return The version string, or "0.0.0" if not set
     */
    public String getVersion() {
        return bukkitConfig.getString(versionKey, "0.0.0");
    }
    
    /**
     * Sets the version in the configuration.
     * 
     * @param version The version string to set
     */
    public void setVersion(String version) {
        bukkitConfig.set(versionKey, version);
    }
    
    /**
     * Enables or disables auto-save functionality.
     * When enabled, the configuration will be automatically saved periodically.
     * 
     * @param autoSave true to enable auto-save, false to disable
     */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }
    
    /**
     * Checks if auto-save is enabled.
     * 
     * @return true if auto-save is enabled, false otherwise
     */
    public boolean isAutoSave() {
        return autoSave;
    }
    
    /**
     * Gets a string value from the configuration.
     * 
     * @param path The path to the value
     * @return The string value, or null if not found
     */
    public String getString(String path) {
        return bukkitConfig.getString(path);
    }
    
    /**
     * Gets a string value from the configuration with a default.
     * 
     * @param path The path to the value
     * @param def The default value
     * @return The string value, or the default if not found
     */
    public String getString(String path, String def) {
        return bukkitConfig.getString(path, def);
    }
    
    /**
     * Gets an integer value from the configuration.
     * 
     * @param path The path to the value
     * @return The integer value, or 0 if not found
     */
    public int getInt(String path) {
        return bukkitConfig.getInt(path);
    }
    
    /**
     * Gets an integer value from the configuration with a default.
     * 
     * @param path The path to the value
     * @param def The default value
     * @return The integer value, or the default if not found
     */
    public int getInt(String path, int def) {
        return bukkitConfig.getInt(path, def);
    }
    
    /**
     * Gets a double value from the configuration.
     * 
     * @param path The path to the value
     * @return The double value, or 0.0 if not found
     */
    public double getDouble(String path) {
        return bukkitConfig.getDouble(path);
    }
    
    /**
     * Gets a double value from the configuration with a default.
     * 
     * @param path The path to the value
     * @param def The default value
     * @return The double value, or the default if not found
     */
    public double getDouble(String path, double def) {
        return bukkitConfig.getDouble(path, def);
    }
    
    /**
     * Gets a boolean value from the configuration.
     * 
     * @param path The path to the value
     * @return The boolean value, or false if not found
     */
    public boolean getBoolean(String path) {
        return bukkitConfig.getBoolean(path);
    }
    
    /**
     * Gets a boolean value from the configuration with a default.
     * 
     * @param path The path to the value
     * @param def The default value
     * @return The boolean value, or the default if not found
     */
    public boolean getBoolean(String path, boolean def) {
        return bukkitConfig.getBoolean(path, def);
    }
    
    /**
     * Sets a value in the configuration.
     * 
     * @param path The path to set
     * @param value The value to set
     */
    public void set(String path, Object value) {
        bukkitConfig.set(path, value);
    }
    
    /**
     * Checks if a path exists in the configuration.
     * 
     * @param path The path to check
     * @return true if the path exists, false otherwise
     */
    public boolean contains(String path) {
        return bukkitConfig.contains(path);
    }
    
    /**
     * Gets all keys in the configuration.
     * 
     * @param deep Whether to get keys recursively
     * @return Set of all keys
     */
    public java.util.Set<String> getKeys(boolean deep) {
        return bukkitConfig.getKeys(deep);
    }
}

