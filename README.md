SimpleDataLib
===========

SimpleDataLib provides fast and easy to use database access, YAML access, CSV access, serialization, and error logging. SimpleDataLib supports SQLite, MySQL, and PostgreSQL. Database writes can be queued and written asynchronously in transactions to prevent slowing of the main thread. Fields can be added, removed, and changed easily for both MySQL and SQLite.

**Requirements:**
- Java 21 or later
- HikariCP connection pooling (included)
- Modern async patterns using ScheduledExecutorService and CompletableFuture

Want to use SimpleDataLib?
---------
To create a SQLite database:
```
SimpleDataLib sdl = new SimpleDataLib("Demo");
sdl.initialize();
sdl.getSQLManager().createDatabase();
```

To create a MySQL database:
```
SimpleDataLib sdl = new SimpleDataLib("Demo");
sdl.initialize();
sdl.getSQLManager().enableMySQL("host", "database", "username", "password", port, useSSL);
sdl.getSQLManager().createDatabase();
```

**Note:** SimpleDataLib now uses HikariCP for connection pooling, providing better performance and connection management. The connection pool size can be configured using `setConnectionPoolSize()`.

An example database write (traditional queue-based):
```
SQLWrite sw = sdl.getSQLManager().getSQLWrite();
HashMap<String,String> values = new HashMap<String,String>();
values.put("FIELD1", "value1");
values.put("FIELD2", "value2");
values.put("FIELD3", "value3");
sw.performInsert("table_name", values);
```

An example async database write (modern CompletableFuture API):
```
SQLWrite sw = sdl.getSQLManager().getSQLWrite();
WriteStatement ws = new WriteStatement("INSERT INTO table_name (FIELD1, FIELD2) VALUES (?, ?)", sdl);
ws.addParameter("value1");
ws.addParameter("value2");
CompletableFuture<WriteResult> future = sw.writeAsync(ws);
future.thenAccept(result -> {
    if (result.getStatus() == WriteResultType.SUCCESS) {
        // Handle success
    }
});
```

An example database read:
```
SQLRead sr = sdl.getSQLManager().getSQLRead();
QueryResult data = sr.select("SELECT * FROM table_name");
while (data.next()) {
	double field1 = data.getDouble("FIELD1");
	String field2 = data.getString("FIELD2");
	{...do something here...}
}
data.close();
```

## Dependencies

SimpleDataLib uses Gradle and requires Java 21+. For Gradle projects, add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenLocal() // If using local build
    mavenCentral()
}

dependencies {
    implementation("regalowl.simpledatalib:simpledatalib:0.1.088-SNAPSHOT")
    
    // Database drivers (provided at runtime)
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    
    // Optional: For JPA/Hibernate support
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
```

**Note:** SimpleDataLib now uses:
- HikariCP 6.3.0 for connection pooling
- SnakeYAML 2.2 for YAML processing
- OpenCSV 5.9 for CSV handling
- Modern async patterns with CompletableFuture

## Optional JPA Support

SimpleDataLib now includes optional JPA/Hibernate support for plugins that want to use JPA:

```java
JpaManager jpa = new JpaManager(sdl);
DataSource dataSource = sdl.getSQLManager().getDataSource();
if (jpa.initialize(dataSource, "MyPersistenceUnit")) {
    EntityManager em = jpa.createEntityManager();
    // Use JPA...
    em.close();
}
```

## Shading

To shade SimpleDataLib into your jar (Gradle with Shadow plugin):

```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

tasks.shadowJar {
    relocate("com.zaxxer.hikari", "your.package.lib.hikari")
    relocate("regalowl.simpledatalib", "your.package.lib.simpledatalib")
    // ... other relocations
}
```

## Migration Notes

### From Previous Versions

- **Java 21+ Required**: SimpleDataLib now requires Java 21 or later
- **HikariCP**: Connection pooling is now handled by HikariCP instead of custom implementation
- **MySQL Connector**: Updated to `com.mysql:mysql-connector-j:9.1.0` (new artifact name)
- **Async Patterns**: Timer/TimerTask replaced with ScheduledExecutorService
- **Modern APIs**: File operations now use NIO.2, CSV uses OpenCSV 5.9 API

All existing APIs remain backward compatible, so no code changes are required for existing plugins.

