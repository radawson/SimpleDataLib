# SimpleDataLib Features

This document provides a comprehensive overview of all features available in SimpleDataLib.

## Table of Contents

1. [Database Support](#database-support)
2. [YAML File Handling](#yaml-file-handling)
3. [CSV File Handling](#csv-file-handling)
4. [Serialization](#serialization)
5. [Error Logging](#error-logging)
6. [Connection Pooling](#connection-pooling)
7. [Async Operations](#async-operations)
8. [JPA Support](#jpa-support)
9. [Bukkit Integration](#bukkit-integration)

## Database Support

SimpleDataLib provides comprehensive database support for multiple database systems.

### Supported Databases

**SQLite:**
- File-based database
- No server required
- Perfect for single-server setups
- Automatic file management

**MySQL:**
- Server-based database
- Multi-server support
- High performance
- Production-ready

**PostgreSQL:**
- Advanced features
- Enterprise-grade
- Full SQL support

### Database Features

**Connection Pooling:**
- HikariCP integration
- Configurable pool size
- Automatic connection management
- Performance optimized

**Transaction Support:**
- ACID compliance
- Rollback support
- Batch operations
- Data integrity

**Schema Management:**
- Easy field addition
- Field removal
- Field modification
- Automatic migrations

**Query Support:**
- Prepared statements
- Parameter binding
- Result set handling
- Type-safe operations

## YAML File Handling

SimpleDataLib provides easy-to-use YAML file management.

### Features

**File Configuration:**
- Automatic file creation
- File registration system
- Concurrent access support
- Thread-safe operations

**YAML Processing:**
- SnakeYAML 2.2 integration
- Safe loading
- Pretty printing
- Comment preservation

**Bukkit Integration:**
- BukkitYamlHandler for Bukkit/Paper plugins
- Automatic file management
- Plugin data folder integration

**Auto-Save:**
- Scheduled saving
- Manual save triggers
- Save on shutdown
- Configurable intervals

### Use Cases

- Configuration files
- Data storage
- Settings management
- Plugin state persistence

## CSV File Handling

SimpleDataLib includes CSV file support for data import/export.

### Features

**CSV Processing:**
- OpenCSV 5.9 integration
- Reading and writing
- Header support
- Type conversion

**Data Import/Export:**
- Export database to CSV
- Import CSV to database
- Data transformation
- Batch operations

### Use Cases

- Data migration
- Backup/restore
- Data analysis
- External tool integration

## Serialization

SimpleDataLib provides serialization utilities for object persistence.

### Features

**Object Serialization:**
- Java serialization support
- Custom serialization
- Version compatibility
- Data compression

**Use Cases:**
- Object persistence
- Cache storage
- State saving
- Data transfer

## Error Logging

SimpleDataLib includes comprehensive error logging.

### Features

**Error Tracking:**
- Automatic error capture
- Stack trace logging
- Error file management
- Timestamp tracking

**ErrorWriter:**
- Dedicated error files
- Error categorization
- Log rotation
- Debug information

**Use Cases:**
- Debugging
- Issue tracking
- Error analysis
- Production monitoring

## Connection Pooling

SimpleDataLib uses HikariCP for efficient connection management.

### Features

**HikariCP Integration:**
- High-performance pooling
- Connection validation
- Leak detection
- Automatic recovery

**Configuration:**
- Pool size configuration
- Connection timeout
- Idle timeout
- Maximum lifetime

**Benefits:**
- Reduced connection overhead
- Better resource management
- Improved performance
- Scalability

## Async Operations

SimpleDataLib provides modern async patterns for non-blocking operations.

### Features

**CompletableFuture API:**
- Modern async patterns
- Non-blocking operations
- Chain operations
- Error handling

**ScheduledExecutorService:**
- Scheduled tasks
- Periodic operations
- Task cancellation
- Thread management

**Use Cases:**
- Database writes
- File operations
- Background tasks
- Performance optimization

## JPA Support

SimpleDataLib includes optional JPA/Hibernate support.

### Features

**JPA Integration:**
- Entity management
- ORM support
- Query language
- Relationship mapping

**Hibernate Support:**
- Full Hibernate features
- Advanced queries
- Caching
- Performance optimization

**Use Cases:**
- Complex data models
- Object-relational mapping
- Advanced queries
- Enterprise applications

## Bukkit Integration

SimpleDataLib provides special integration for Bukkit/Paper plugins.

### Features

**BukkitYamlHandler:**
- Plugin data folder integration
- Automatic file management
- Bukkit configuration compatibility
- Paper API support

**Plugin Lifecycle:**
- Automatic initialization
- Shutdown handling
- Resource cleanup
- Event integration

**Use Cases:**
- Minecraft plugin development
- Configuration management
- Data persistence
- Plugin state management

## Additional Features

### File Tools

**File Management:**
- File creation
- Directory management
- Path utilities
- File operations

### Event System

**Event Publishing:**
- Custom events
- Event listeners
- Event handling
- Plugin integration

### Common Functions

**Utility Functions:**
- String utilities
- Number formatting
- Date/time handling
- Type conversion

## Best Practices

1. **Use Connection Pooling**: Configure appropriate pool sizes
2. **Use Async Operations**: For non-critical writes
3. **Handle Errors**: Implement proper error handling
4. **Close Resources**: Always close result sets and connections
5. **Use Transactions**: For multi-step operations
6. **Backup Data**: Regular backups of important data
7. **Monitor Performance**: Track query performance
8. **Use Prepared Statements**: For security and performance

## Migration from Older Versions

### Key Changes

**Java 21+ Required:**
- Modern language features
- Better performance
- Security improvements

**HikariCP:**
- Replaces custom pooling
- Better performance
- More features

**Modern APIs:**
- NIO.2 for file operations
- CompletableFuture for async
- ScheduledExecutorService for tasks

**Backward Compatibility:**
- Existing APIs still work
- No code changes required
- Gradual migration possible

For detailed migration instructions, see the [Usage Guide](usage.md).

## Performance Considerations

1. **Connection Pooling**: Properly size connection pools
2. **Async Operations**: Use async for writes when possible
3. **Query Optimization**: Optimize database queries
4. **Batch Operations**: Use batch operations for bulk data
5. **Resource Management**: Properly close resources
6. **Caching**: Consider caching for frequently accessed data

For more information, see the [Configuration Guide](configuration.md) and [Usage Guide](usage.md).


