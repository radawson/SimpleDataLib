package regalowl.simpledatalib;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import regalowl.simpledatalib.bukkit.BukkitYamlHandler;
import regalowl.simpledatalib.event.EventPublisher;
import regalowl.simpledatalib.event.SDLEventListener;
import regalowl.simpledatalib.file.ErrorWriter;
import regalowl.simpledatalib.file.FileTools;
import regalowl.simpledatalib.file.YamlHandler;
import regalowl.simpledatalib.sql.SQLManager;

/**
 * SimpleDataLib provides fast and easy to use database access, YAML access, CSV access,
 * serialization, and error logging. Supports SQLite, MySQL, and PostgreSQL.
 * Now includes Bukkit/Paper integration for Minecraft plugins.
 */
public class SimpleDataLib {

	private EventPublisher ep;
	private YamlHandler yh;
	private BukkitYamlHandler bukkitYamlHandler;
	private FileTools ft;
	private ErrorWriter ew;
	private SQLManager sm;
	
	private String name;
	private String storagePath;
	private boolean shutdown;
	private boolean debug;
	private JavaPlugin bukkitPlugin;
	
	public SimpleDataLib(String name) {
		this.name = name;
		ft = new FileTools(this);
		this.storagePath = ft.getJarPath() + File.separator + name;
		ft.makeFolder(storagePath);
		ep = new EventPublisher();
		yh = new YamlHandler(this);
		ew = new ErrorWriter(getErrorFilePath(), this);
		shutdown = false;
		debug = false;
	}
	
	public void initialize() {
		sm = new SQLManager(this);
	}
	

	public void shutDown() {
		if (!shutdown) {
			sm.shutDown();
			if (yh != null) {yh.shutDown();}
			if (bukkitYamlHandler != null) {bukkitYamlHandler.shutdown();}
			shutdown = true;
		}
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}
	public String getName() {
		return name;
	}
	public boolean debugEnabled() {
		return debug;
	}
	public boolean isDisabled() {
		return shutdown;
	}
	public String getStoragePath() {
		return storagePath;
	}
	public String getErrorFilePath() {
		return storagePath + File.separator + "errors.log";
	}

	
	public void registerListener(SDLEventListener l) {
		ep.registerListener(l);
	}
	public void setDebug(boolean state) {
		this.debug = state;
	}


	public SQLManager getSQLManager() {
		return sm;
	}
	public YamlHandler getYamlHandler() {
		return yh;
	}
	public FileTools getFileTools() {
		return ft;
	}
	public ErrorWriter getErrorWriter() {
		return ew;
	}
	public EventPublisher getEventPublisher() {
		return ep;
	}
	
	/**
	 * Sets the Bukkit plugin reference for enhanced integration.
	 * This enables Bukkit-specific features like BukkitYamlHandler.
	 * 
	 * @param plugin The JavaPlugin instance
	 */
	public void setPlugin(JavaPlugin plugin) {
		this.bukkitPlugin = plugin;
		if (plugin != null) {
			// Initialize BukkitYamlHandler if plugin is available
			if (bukkitYamlHandler == null) {
				bukkitYamlHandler = new BukkitYamlHandler(this, plugin);
			}
			// Configure ErrorWriter for Bukkit integration
			if (ew != null) {
				ew.setBukkitPlugin(plugin);
				ew.setConsoleLogging(true); // Enable console logging by default for Bukkit
			}
		}
	}
	
	/**
	 * Gets the Bukkit plugin reference if set.
	 * 
	 * @return The JavaPlugin instance, or null if not in Bukkit environment
	 */
	public JavaPlugin getPlugin() {
		return bukkitPlugin;
	}
	
	/**
	 * Gets the BukkitYamlHandler for managing Bukkit YAML configurations.
	 * Returns null if not in a Bukkit environment or plugin not set.
	 * 
	 * @return The BukkitYamlHandler instance, or null if not available
	 */
	public BukkitYamlHandler getBukkitYamlHandler() {
		return bukkitYamlHandler;
	}


}
