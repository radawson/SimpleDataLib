# SimpleDataLib Usage Guide

This guide provides practical examples and best practices for using SimpleDataLib.

## Table of Contents

1. [Basic Setup](#basic-setup)
2. [Database Operations](#database-operations)
3. [YAML Operations](#yaml-operations)
4. [CSV Operations](#csv-operations)
5. [Serialization](#serialization)
6. [Best Practices](#best-practices)
7. [Migration Guide](#migration-guide)
8. [Troubleshooting](#troubleshooting)

## Basic Setup

### Plugin Integration

```java
public class MyPlugin extends JavaPlugin {
    private SimpleDataLib sdl;
    private SQLManager sqlManager;
    
    @Override
    public void onEnable() {
        // Initialize SimpleDataLib
        sdl = new SimpleDataLib(getName());
        sdl.setBukkitPlugin(this);
        sdl.initialize();
        
        // Setup database
        sqlManager = sdl.getSQLManager();
        sqlManager.createDatabase();  // SQLite
        
        // Or MySQL
        // sqlManager.enableMySQL("host", "db", "user", "pass", 3306, false);
        // sqlManager.createDatabase();
        
        // Register YAML files
        sdl.getYamlHandler().registerFileConfiguration("config");
        
        getLogger().info("Plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save YAML files
        sdl.getYamlHandler().saveYamls();
        
        // Shutdown SimpleDataLib
        sdl.shutDown();
        
        getLogger().info("Plugin disabled!");
    }
    
    public SimpleDataLib getSimpleDataLib() {
        return sdl;
    }
}
```

## Database Operations

### Writing Data (Traditional Queue-Based)

```java
SQLWrite sw = sdl.getSQLManager().getSQLWrite();

// Insert data
HashMap<String, String> values = new HashMap<>();
values.put("name", "John");
values.put("age", "25");
values.put("email", "john@example.com");
sw.performInsert("users", values);

// Update data
HashMap<String, String> updateValues = new HashMap<>();
updateValues.put("age", "26");
HashMap<String, String> whereValues = new HashMap<>();
whereValues.put("name", "John");
sw.performUpdate("users", updateValues, whereValues);

// Delete data
HashMap<String, String> deleteWhere = new HashMap<>();
deleteWhere.put("name", "John");
sw.performDelete("users", deleteWhere);
```

### Writing Data (Modern Async API)

```java
SQLWrite sw = sdl.getSQLManager().getSQLWrite();

// Async insert
WriteStatement insert = new WriteStatement(
    "INSERT INTO users (name, age, email) VALUES (?, ?, ?)",
    sdl
);
insert.addParameter("John");
insert.addParameter(25);
insert.addParameter("john@example.com");

CompletableFuture<WriteResult> future = sw.writeAsync(insert);
future.thenAccept(result -> {
    if (result.getStatus() == WriteResultType.SUCCESS) {
        getLogger().info("User inserted successfully!");
    } else {
        getLogger().severe("Failed to insert user: " + result.getError());
    }
});

// Async update
WriteStatement update = new WriteStatement(
    "UPDATE users SET age = ? WHERE name = ?",
    sdl
);
update.addParameter(26);
update.addParameter("John");
sw.writeAsync(update);

// Async delete
WriteStatement delete = new WriteStatement(
    "DELETE FROM users WHERE name = ?",
    sdl
);
delete.addParameter("John");
sw.writeAsync(delete);
```

### Reading Data

```java
SQLRead sr = sdl.getSQLManager().getSQLRead();

// Simple select
QueryResult data = sr.select("SELECT * FROM users WHERE name = ?", "John");
while (data.next()) {
    String name = data.getString("name");
    int age = data.getInt("age");
    String email = data.getString("email");
    
    getLogger().info("User: " + name + ", Age: " + age + ", Email: " + email);
}
data.close();

// Select with multiple parameters
QueryResult data2 = sr.select(
    "SELECT * FROM users WHERE age > ? AND email LIKE ?",
    20,
    "%@example.com"
);
while (data2.next()) {
    // Process results
}
data2.close();

// Count records
QueryResult count = sr.select("SELECT COUNT(*) as total FROM users");
if (count.next()) {
    int total = count.getInt("total");
    getLogger().info("Total users: " + total);
}
count.close();
```

### Creating Tables

```java
SQLWrite sw = sdl.getSQLManager().getSQLWrite();

// Create table
sw.performCreate("users",
    "id INTEGER PRIMARY KEY AUTOINCREMENT",
    "name VARCHAR(100) NOT NULL",
    "age INTEGER",
    "email VARCHAR(255)"
);

// Add column
sw.performAddColumn("users", "phone", "VARCHAR(20)");

// Remove column
sw.performRemoveColumn("users", "phone");
```

### Transactions

```java
SQLWrite sw = sdl.getSQLManager().getSQLWrite();

// Start transaction
sw.startTransaction();

try {
    // Multiple operations
    HashMap<String, String> user1 = new HashMap<>();
    user1.put("name", "John");
    user1.put("age", "25");
    sw.performInsert("users", user1);
    
    HashMap<String, String> user2 = new HashMap<>();
    user2.put("name", "Jane");
    user2.put("age", "30");
    sw.performInsert("users", user2);
    
    // Commit transaction
    sw.commitTransaction();
} catch (Exception e) {
    // Rollback on error
    sw.rollbackTransaction();
    getLogger().severe("Transaction failed: " + e.getMessage());
}
```

## YAML Operations

### Basic YAML Usage

```java
// Register file
YamlHandler yamlHandler = sdl.getYamlHandler();
yamlHandler.registerFileConfiguration("config");

// Get file
FileConfiguration config = yamlHandler.getFileConfiguration("config");

// Read values
String value = config.getString("key");
int number = config.getInt("number");
boolean flag = config.getBoolean("flag");
List<String> list = config.getStringList("list");

// Write values
config.set("key", "value");
config.set("number", 42);
config.set("flag", true);
config.set("list", Arrays.asList("item1", "item2"));

// Save file
config.save();
```

### Nested Configuration

```yaml
# config.yml
database:
  host: localhost
  port: 3306
  username: admin
```

```java
FileConfiguration config = yamlHandler.getFileConfiguration("config");

// Read nested values
String host = config.getString("database.host");
int port = config.getInt("database.port");

// Write nested values
config.set("database.host", "newhost");
config.set("database.port", 5432);
config.save();
```

### Default Values

```java
FileConfiguration config = yamlHandler.getFileConfiguration("config");

// Set defaults
config.setDefault("key", "defaultValue");
config.setDefault("number", 0);
config.setDefault("flag", false);

// Get with default
String value = config.getString("key", "default");
int number = config.getInt("number", 0);
```

### Auto-Save

```java
YamlHandler yamlHandler = sdl.getYamlHandler();

// Set save interval (milliseconds)
yamlHandler.setSaveInterval(60000);  // 60 seconds

// Start auto-save
yamlHandler.startAutoSave();

// Stop auto-save
yamlHandler.stopAutoSave();
```

## CSV Operations

### Reading CSV

```java
FileTools ft = sdl.getFileTools();
File csvFile = new File(sdl.getStoragePath(), "data.csv");

// Read CSV
List<String[]> rows = ft.readCSV(csvFile);
for (String[] row : rows) {
    // Process row
    String column1 = row[0];
    String column2 = row[1];
}
```

### Writing CSV

```java
FileTools ft = sdl.getFileTools();
File csvFile = new File(sdl.getStoragePath(), "data.csv");

// Write CSV
List<String[]> rows = new ArrayList<>();
rows.add(new String[]{"Name", "Age", "Email"});
rows.add(new String[]{"John", "25", "john@example.com"});
rows.add(new String[]{"Jane", "30", "jane@example.com"});

ft.writeCSV(csvFile, rows);
```

## Serialization

### Serializing Objects

```java
CommonFunctions cf = new CommonFunctions();

// Serialize object
MyObject obj = new MyObject();
String serialized = cf.serialize(obj);

// Deserialize object
MyObject deserialized = cf.deserialize(serialized, MyObject.class);
```

### Saving Serialized Data

```java
// Serialize and save to file
MyObject obj = new MyObject();
String serialized = cf.serialize(obj);
FileTools ft = sdl.getFileTools();
ft.writeStringToFile(new File(sdl.getStoragePath(), "object.dat"), serialized);

// Load and deserialize
String data = ft.readStringFromFile(new File(sdl.getStoragePath(), "object.dat"));
MyObject loaded = cf.deserialize(data, MyObject.class);
```

## Best Practices

### Resource Management

**Always Close Resources:**
```java
QueryResult data = sr.select("SELECT * FROM users");
try {
    while (data.next()) {
        // Process data
    }
} finally {
    data.close();  // Always close
}
```

**Use Try-With-Resources (if supported):**
```java
try (QueryResult data = sr.select("SELECT * FROM users")) {
    while (data.next()) {
        // Process data
    }
}  // Automatically closed
```

### Error Handling

```java
try {
    SQLWrite sw = sdl.getSQLManager().getSQLWrite();
    sw.performInsert("users", values);
} catch (SQLException e) {
    sdl.getErrorWriter().writeError("Failed to insert user", e);
    getLogger().severe("Database error: " + e.getMessage());
}
```

### Async Operations

**Use Async for Writes:**
```java
// Good: Async write
WriteStatement ws = new WriteStatement("INSERT INTO users (name) VALUES (?)", sdl);
ws.addParameter("John");
sw.writeAsync(ws);

// Avoid: Blocking write in main thread
sw.performInsert("users", values);  // Only if necessary
```

**Handle Async Results:**
```java
CompletableFuture<WriteResult> future = sw.writeAsync(ws);
future.thenAccept(result -> {
    if (result.getStatus() == WriteResultType.SUCCESS) {
        // Success handling
    } else {
        // Error handling
        getLogger().severe("Write failed: " + result.getError());
    }
}).exceptionally(throwable -> {
    getLogger().severe("Async error: " + throwable.getMessage());
    return null;
});
```

### Performance Optimization

**Batch Operations:**
```java
sw.startTransaction();
try {
    for (User user : users) {
        HashMap<String, String> values = new HashMap<>();
        values.put("name", user.getName());
        values.put("age", String.valueOf(user.getAge()));
        sw.performInsert("users", values);
    }
    sw.commitTransaction();
} catch (Exception e) {
    sw.rollbackTransaction();
}
```

**Prepared Statements:**
```java
// Good: Prepared statement
QueryResult data = sr.select("SELECT * FROM users WHERE name = ?", name);

// Avoid: String concatenation (SQL injection risk)
QueryResult data = sr.select("SELECT * FROM users WHERE name = '" + name + "'");
```

## Migration Guide

### From Older Versions

**No Code Changes Required:**
- All existing APIs remain compatible
- Gradual migration possible
- Old code continues to work

**New Features Available:**
- Async operations (optional)
- HikariCP (automatic)
- Modern APIs (optional)

**Migration Steps:**

1. **Update Dependencies:**
   ```kotlin
   dependencies {
       implementation("regalowl.simpledatalib:simpledatalib:0.1.088-SNAPSHOT")
   }
   ```

2. **Update Java Version:**
   - Ensure Java 21+ is available
   - Update build configuration

3. **Test Existing Code:**
   - Run existing code
   - Verify functionality
   - Check for deprecation warnings

4. **Gradually Adopt New Features:**
   - Start using async operations
   - Optimize connection pools
   - Update to modern APIs

### Shading

**Gradle (Shadow Plugin):**
```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

tasks.shadowJar {
    relocate("com.zaxxer.hikari", "your.package.lib.hikari")
    relocate("regalowl.simpledatalib", "your.package.lib.simpledatalib")
}
```

## Troubleshooting

### Database Connection Issues

**Problem:** Cannot connect to database

**Solutions:**
- Verify database server is running
- Check network connectivity
- Verify credentials
- Check firewall settings
- Verify database exists

### Connection Pool Exhaustion

**Problem:** Timeout errors, connection pool exhausted

**Solutions:**
- Increase pool size: `sqlManager.setConnectionPoolSize(20)`
- Check for connection leaks
- Close connections properly
- Reduce connection lifetime

### YAML File Issues

**Problem:** Files not saving or broken

**Solutions:**
- Check file permissions
- Verify file paths
- Check disk space
- Enable auto-save
- Handle broken files gracefully

### Performance Issues

**Problem:** Slow operations

**Solutions:**
- Use async operations
- Optimize queries
- Adjust connection pool
- Use batch operations
- Profile operations

### Getting Help

1. Check this documentation
2. Review [Configuration Guide](configuration.md)
3. Check [Features Guide](features.md)
4. Search GitHub Issues
5. Create a new issue with:
   - SimpleDataLib version
   - Java version
   - Error messages
   - Code examples

## Tips & Best Practices

1. **Initialize Early**: Initialize in plugin `onEnable()`
2. **Shutdown Properly**: Always call `shutDown()` in `onDisable()`
3. **Close Resources**: Always close QueryResult and connections
4. **Use Async**: Use async operations for writes when possible
5. **Handle Errors**: Implement proper error handling
6. **Use Transactions**: For multi-step operations
7. **Optimize Queries**: Use indexes and optimize queries
8. **Monitor Performance**: Track query performance
9. **Backup Data**: Regular backups of important data
10. **Test Thoroughly**: Test all database operations

For more information, see the [Features Guide](features.md) and [Configuration Guide](configuration.md).


