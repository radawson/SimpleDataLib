# SimpleDataLib Configuration Guide

This guide covers setup and configuration for SimpleDataLib.

## Table of Contents

1. [Initialization](#initialization)
2. [Database Setup](#database-setup)
3. [Connection Pool Configuration](#connection-pool-configuration)
4. [YAML Configuration](#yaml-configuration)
5. [Error Logging Configuration](#error-logging-configuration)
6. [Best Practices](#best-practices)

## Initialization

### Basic Initialization

```java
SimpleDataLib sdl = new SimpleDataLib("MyPlugin");
sdl.initialize();
```

**Parameters:**
- `name`: Plugin/library name (used for storage path)

**Storage Path:**
- Default: `{jar_path}/{name}/`
- Can be customized with `setStoragePath()`

### Bukkit/Paper Plugin Initialization

```java
public class MyPlugin extends JavaPlugin {
    private SimpleDataLib sdl;
    
    @Override
    public void onEnable() {
        sdl = new SimpleDataLib(getName());
        sdl.setBukkitPlugin(this);
        sdl.initialize();
        // ... rest of initialization
    }
    
    @Override
    public void onDisable() {
        if (sdl != null) {
            sdl.shutDown();
        }
    }
}
```

### Shutdown

Always call `shutDown()` when done:

```java
sdl.shutDown();
```

This ensures:
- Database connections are closed
- YAML files are saved
- Resources are cleaned up
- Background tasks are stopped

## Database Setup

### SQLite Setup

SQLite is the default database (no server required).

```java
SimpleDataLib sdl = new SimpleDataLib("MyPlugin");
sdl.initialize();
sdl.getSQLManager().createDatabase();
```

**Database File:**
- Location: `{storage_path}/database.db`
- Automatic creation
- No configuration needed

**Use Cases:**
- Single-server setups
- Development/testing
- Small to medium data volumes
- No network requirements

### MySQL Setup

MySQL requires a database server.

```java
SimpleDataLib sdl = new SimpleDataLib("MyPlugin");
sdl.initialize();

SQLManager sqlManager = sdl.getSQLManager();
sqlManager.enableMySQL(
    "localhost",      // host
    "mydatabase",     // database
    "username",        // username
    "password",        // password
    3306,              // port
    false              // useSSL
);
sqlManager.createDatabase();
```

**Configuration Parameters:**
- `host`: Database server hostname or IP
- `database`: Database name
- `username`: Database username
- `password`: Database password
- `port`: Database port (default: 3306)
- `useSSL`: Enable SSL connection

**Security Notes:**
- Never hardcode passwords
- Use environment variables or secure config
- Enable SSL for production
- Use strong passwords

**Use Cases:**
- Multi-server setups
- Production environments
- Large data volumes
- Network-based access

### PostgreSQL Setup

PostgreSQL setup is similar to MySQL:

```java
SQLManager sqlManager = sdl.getSQLManager();
sqlManager.enablePostgreSQL(
    "localhost",      // host
    "mydatabase",     // database
    "username",        // username
    "password",        // password
    5432,              // port
    false              // useSSL
);
sqlManager.createDatabase();
```

**Configuration Parameters:**
- Same as MySQL
- Default port: 5432

## Connection Pool Configuration

### HikariCP Configuration

SimpleDataLib uses HikariCP for connection pooling.

**Default Settings:**
- Pool size: 10 connections
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max lifetime: 30 minutes

### Customizing Pool Size

```java
SQLManager sqlManager = sdl.getSQLManager();
sqlManager.setConnectionPoolSize(20);  // Set pool size
```

**Pool Size Guidelines:**
- Small applications: 5-10 connections
- Medium applications: 10-20 connections
- Large applications: 20-50 connections
- Very large: 50+ connections

**Considerations:**
- More connections = more memory
- Too many connections can hurt performance
- Match to expected concurrent load
- Monitor connection usage

### Advanced Pool Configuration

Access HikariCP DataSource for advanced configuration:

```java
SQLManager sqlManager = sdl.getSQLManager();
DataSource dataSource = sqlManager.getDataSource();

if (dataSource instanceof HikariDataSource) {
    HikariDataSource hikari = (HikariDataSource) dataSource;
    hikari.setMaximumPoolSize(20);
    hikari.setMinimumIdle(5);
    hikari.setConnectionTimeout(30000);
    hikari.setIdleTimeout(600000);
    hikari.setMaxLifetime(1800000);
}
```

**Configuration Options:**
- `maximumPoolSize`: Maximum pool size
- `minimumIdle`: Minimum idle connections
- `connectionTimeout`: Connection timeout (ms)
- `idleTimeout`: Idle connection timeout (ms)
- `maxLifetime`: Maximum connection lifetime (ms)

## YAML Configuration

### Registering YAML Files

```java
YamlHandler yamlHandler = sdl.getYamlHandler();
yamlHandler.registerFileConfiguration("config");
yamlHandler.registerFileConfiguration("data");
```

**File Location:**
- Files are created in `{storage_path}/`
- Extension `.yml` is added automatically
- Files are created if they don't exist

### Accessing YAML Files

```java
FileConfiguration config = sdl.getYamlHandler().getFileConfiguration("config");
String value = config.getString("key");
config.set("key", "value");
config.save();
```

### Auto-Save Configuration

```java
YamlHandler yamlHandler = sdl.getYamlHandler();
yamlHandler.setSaveInterval(60000);  // Save every 60 seconds
yamlHandler.startAutoSave();
```

**Save Intervals:**
- Too frequent: Performance impact
- Too infrequent: Data loss risk
- Recommended: 30-300 seconds

### Manual Saving

```java
// Save specific file
yamlHandler.saveYaml("config");

// Save all files
yamlHandler.saveYamls();
```

### Bukkit YAML Integration

For Bukkit/Paper plugins:

```java
BukkitYamlHandler bukkitYaml = sdl.getBukkitYamlHandler();
BukkitYamlConfiguration config = bukkitYaml.getConfig("config.yml");
String value = config.getString("key");
```

## Error Logging Configuration

### Error File Location

Error logs are automatically written to:
- `{storage_path}/errors.log`

### Error Writer Access

```java
ErrorWriter errorWriter = sdl.getErrorWriter();
errorWriter.writeError("Error message", exception);
```

### Debug Mode

```java
sdl.setDebug(true);  // Enable debug logging
```

**Debug Mode:**
- More verbose logging
- Additional error details
- Performance impact
- Use for development only

## Best Practices

### Initialization

1. **Initialize Early**: Initialize in plugin `onEnable()`
2. **Shutdown Properly**: Always call `shutDown()` in `onDisable()`
3. **Error Handling**: Wrap initialization in try-catch
4. **Resource Management**: Close resources properly

### Database Configuration

1. **Connection Pooling**: Use appropriate pool sizes
2. **SSL for Production**: Enable SSL for MySQL/PostgreSQL
3. **Credentials Security**: Never hardcode passwords
4. **Connection Limits**: Match pool size to server capacity
5. **Monitor Connections**: Track connection usage

### YAML Configuration

1. **Register Early**: Register files during initialization
2. **Save Regularly**: Use auto-save or manual saves
3. **Save on Shutdown**: Ensure files are saved
4. **Handle Errors**: Check for broken files
5. **Thread Safety**: YAML operations are thread-safe

### Error Handling

1. **Log Errors**: Use ErrorWriter for errors
2. **Debug Mode**: Enable for development
3. **Error Files**: Monitor error logs
4. **Exception Handling**: Catch and handle exceptions

### Performance

1. **Async Operations**: Use async for writes
2. **Batch Operations**: Batch database operations
3. **Connection Pooling**: Properly size pools
4. **Resource Cleanup**: Close resources promptly
5. **Query Optimization**: Optimize database queries

### Security

1. **Credentials**: Never hardcode passwords
2. **SQL Injection**: Use prepared statements
3. **SSL**: Enable for production databases
4. **File Permissions**: Secure file permissions
5. **Input Validation**: Validate all inputs

## Troubleshooting

### Database Connection Issues

**Symptoms:**
- Connection timeouts
- Connection refused errors
- Authentication failures

**Solutions:**
- Verify database server is running
- Check network connectivity
- Verify credentials
- Check firewall settings
- Verify database exists
- Check user permissions

### Connection Pool Exhaustion

**Symptoms:**
- Timeout errors
- Slow performance
- Connection errors

**Solutions:**
- Increase pool size
- Check for connection leaks
- Close connections properly
- Monitor connection usage
- Reduce connection lifetime

### YAML File Issues

**Symptoms:**
- Files not saving
- Broken files
- Data loss

**Solutions:**
- Check file permissions
- Verify file paths
- Check disk space
- Enable auto-save
- Handle broken files

### Performance Issues

**Symptoms:**
- Slow operations
- High memory usage
- Timeout errors

**Solutions:**
- Optimize queries
- Use async operations
- Adjust pool sizes
- Monitor resource usage
- Profile operations

For more help, see the [Usage Guide](usage.md) or check GitHub Issues.

