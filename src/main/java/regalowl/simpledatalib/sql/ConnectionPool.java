package regalowl.simpledatalib.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import regalowl.simpledatalib.SimpleDataLib;

public class ConnectionPool {
	private SimpleDataLib sdl;
	private DataSource dataSource;
	private final Set<DatabaseConnection> activeConnections = new HashSet<>();
	private final AtomicBoolean writesBlocked = new AtomicBoolean(false);
    
    public ConnectionPool(SimpleDataLib sdl, DataSource dataSource, int connectionCount) {
    	this.sdl = sdl;
    	this.dataSource = dataSource;
    }
    
	public int getActiveConnections() {
		synchronized (activeConnections) {
			return activeConnections.size();
		}
	}
    
	public void blockDatabaseWrites() {
		writesBlocked.set(true);
		synchronized (activeConnections) {
			for (DatabaseConnection dc : new ArrayList<>(activeConnections)) {
				dc.setIgnoreWrites(true);
			}
		}
	}
	
	public void allowDatabaseWrites() {
		writesBlocked.set(false);
		synchronized (activeConnections) {
			for (DatabaseConnection dc : new ArrayList<>(activeConnections)) {
				dc.setIgnoreWrites(false);
			}
		}
	}
    
	public void returnConnection(DatabaseConnection connection) {
		synchronized (activeConnections) {
			activeConnections.remove(connection);
		}
		// Connection will be closed automatically by HikariCP when returned
		connection.closeConnection();
	}
	
	public DatabaseConnection getDatabaseConnection() {
		try {
			Connection conn = dataSource.getConnection();
			DatabaseConnection dbConn = new DatabaseConnection(sdl, conn, writesBlocked.get());
			synchronized (activeConnections) {
				activeConnections.add(dbConn);
			}
			return dbConn;
		} catch (SQLException e) {
			sdl.getErrorWriter().writeError(e, "Failed to get database connection from pool");
			return null;
		}
	}
	
	public void shutDown() {
		synchronized (activeConnections) {
			for (DatabaseConnection dc : new ArrayList<>(activeConnections)) {
				dc.lock();
				dc.closeConnection();
			}
			activeConnections.clear();
		}
		// HikariCP DataSource will be closed by SQLManager
	}

}
