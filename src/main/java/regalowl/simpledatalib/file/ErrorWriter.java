package regalowl.simpledatalib.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;





import regalowl.simpledatalib.CommonFunctions;
import regalowl.simpledatalib.SimpleDataLib;

/**
 * Error logging utility that writes errors to a file with optional Bukkit logger integration.
 * Supports error categorization, log rotation, and console logging alongside file logging.
 */
public class ErrorWriter {

	private SimpleDataLib sdl;
	private String path;
	private String text;
	private String error;
	private JavaPlugin bukkitPlugin;
	private Logger bukkitLogger;
	private boolean consoleLogging;
	private long maxLogSize;
	private static final long DEFAULT_MAX_LOG_SIZE = 10 * 1024 * 1024; // 10 MB

	/**
	 * Creates a new ErrorWriter.
	 * 
	 * @param path The path to the error log file
	 * @param sdl The SimpleDataLib instance
	 */
	public ErrorWriter(String path, SimpleDataLib sdl) {
		this.sdl = sdl;
		this.path = path;
		this.bukkitPlugin = null;
		this.bukkitLogger = null;
		this.consoleLogging = false;
		this.maxLogSize = DEFAULT_MAX_LOG_SIZE;
	}
	
	/**
	 * Sets the Bukkit plugin for logger integration.
	 * 
	 * @param plugin The JavaPlugin instance
	 */
	public void setBukkitPlugin(JavaPlugin plugin) {
		this.bukkitPlugin = plugin;
		if (plugin != null) {
			this.bukkitLogger = plugin.getLogger();
		}
	}
	
	/**
	 * Enables or disables console logging alongside file logging.
	 * 
	 * @param enabled true to enable console logging, false to disable
	 */
	public void setConsoleLogging(boolean enabled) {
		this.consoleLogging = enabled;
	}
	
	/**
	 * Sets the maximum log file size before rotation.
	 * 
	 * @param maxSize The maximum size in bytes
	 */
	public void setMaxLogSize(long maxSize) {
		this.maxLogSize = maxSize;
	}
	
	/**
	 * Gets the maximum log file size.
	 * 
	 * @return The maximum size in bytes
	 */
	public long getMaxLogSize() {
		return maxLogSize;
	}
	
	
	/**
	 * Writes an error to the log file.
	 * 
	 * @param e The exception to log
	 * @param info Additional information about the error
	 */
	public void writeError(Exception e, String info) {
		writeError(e, info, false, LogLevel.SEVERE);
	}
	
	/**
	 * Writes an error to the log file.
	 * 
	 * @param e The exception to log
	 */
	public void writeError(Exception e) {
		writeError(e, null, false, LogLevel.SEVERE);
	}
	
	/**
	 * Writes an error message to the log file.
	 * 
	 * @param info The error message
	 */
	public void writeError(String info) {
		writeError(null, info, false, LogLevel.SEVERE);
	}
	
	/**
	 * Writes an error to the log file with specified log level.
	 * 
	 * @param e The exception to log
	 * @param text Additional information about the error
	 * @param logLevel The log level (SEVERE, WARNING, INFO)
	 */
	public void writeError(Exception e, String text, LogLevel logLevel) {
		writeError(e, text, false, logLevel);
	}
	
	/**
	 * Writes an error to the log file.
	 * 
	 * @param e The exception to log
	 * @param text Additional information about the error
	 * @param sync Whether to write synchronously (blocks until complete)
	 */
	public void writeError(Exception e, String text, boolean sync) {
		writeError(e, text, sync, LogLevel.SEVERE);
	}
	
	/**
	 * Writes an error to the log file with full control.
	 * 
	 * @param e The exception to log
	 * @param text Additional information about the error
	 * @param sync Whether to write synchronously (blocks until complete)
	 * @param logLevel The log level (SEVERE, WARNING, INFO)
	 */
	public void writeError(Exception e, String text, boolean sync, LogLevel logLevel) {
		this.error = CommonFunctions.getErrorString(e);
		this.text = text;
		
		// Log to console if enabled
		if (consoleLogging || bukkitLogger != null) {
			logToConsole(e, text, logLevel);
		}
		
		// Check log rotation before writing
		checkLogRotation();
		
		if (!sync) {
			new Thread(new Writer()).start();
		} else {
			write();
		}
	}
	
	/**
	 * Logs the error to console using Bukkit logger or standard output.
	 * 
	 * @param e The exception
	 * @param text Additional information
	 * @param logLevel The log level
	 */
	private void logToConsole(Exception e, String text, LogLevel logLevel) {
		if (bukkitLogger != null) {
			Level javaLevel = convertLogLevel(logLevel);
			if (text != null) {
				if (e != null) {
					bukkitLogger.log(javaLevel, text, e);
				} else {
					bukkitLogger.log(javaLevel, text);
				}
			} else if (e != null) {
				bukkitLogger.log(javaLevel, "Error occurred", e);
			}
		} else if (consoleLogging) {
			// Fallback to standard output if no Bukkit logger
			if (text != null) {
				System.err.println("[" + logLevel.name() + "] " + text);
			}
			if (e != null) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Converts SimpleDataLib LogLevel to Java Logger Level.
	 * 
	 * @param logLevel The SimpleDataLib log level
	 * @return The Java Logger level
	 */
	private Level convertLogLevel(LogLevel logLevel) {
		if (logLevel == null) {
			return Level.SEVERE;
		}
		switch (logLevel) {
			case SEVERE:
				return Level.SEVERE;
			case WARNING:
				return Level.WARNING;
			case INFO:
				return Level.INFO;
			default:
				return Level.SEVERE;
		}
	}
	
	/**
	 * Checks if log rotation is needed and performs it if necessary.
	 */
	private void checkLogRotation() {
		try {
			File logFile = new File(path);
			if (logFile.exists() && logFile.length() > maxLogSize) {
				// Rotate log: rename current to .old
				File oldLog = new File(path + ".old");
				if (oldLog.exists()) {
					oldLog.delete();
				}
				logFile.renameTo(oldLog);
			}
		} catch (Exception e) {
			// Silently fail rotation - don't break error logging
		}
	}
	
	/**
	 * Enum for log levels.
	 */
	public enum LogLevel {
		SEVERE, WARNING, INFO
	}
	
	/**
	 * Writes the error to the log file.
	 */
	private void write() {
		try {
			File file = new File(path);
			// Ensure parent directory exists
			File parent = file.getParentFile();
			if (parent != null && !parent.exists()) {
				sdl.getFileTools().makeFolder(parent.getAbsolutePath());
			}
			
			if (!file.exists()) {
				sdl.getFileTools().makeFile(file.getAbsolutePath());
			}
			
			try (FileWriter fw = new FileWriter(file, true);
				 BufferedWriter bw = new BufferedWriter(fw)) {
				bw.newLine();
				bw.newLine();
				bw.write(sdl.getName() + "[" + CommonFunctions.getTimeStamp() + "]");
				bw.newLine();
				if (text != null) {
					bw.write(text.replace("{{newline}}", System.getProperty("line.separator")));
					bw.newLine();
				}
				if (error != null) {
					bw.write(error);
				}
				bw.newLine();
				bw.newLine();
			}
		} catch (IOException e) {
			// Try to log to console if file writing fails
			if (bukkitLogger != null) {
				bukkitLogger.log(Level.SEVERE, "Failed to write to error log file: " + path, e);
			} else {
				System.err.println("Failed to write to error log file: " + path);
				e.printStackTrace();
			}
		}
	}
	
	private class Writer implements Runnable {
		@Override
		public void run() {
			write();
		}
	}
	
}
